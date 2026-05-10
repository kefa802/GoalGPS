package com.example.gpslog;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GpsLoggingService extends Service {
    private static final String CHANNEL_ID = "gps_service_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;
    private MyWebServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= 34) { startForeground(1, getNotification("GPSログ記録中..."), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION); } 
        else { startForeground(1, getNotification("GPSログ記録中...")); }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        server = new MyWebServer(8080);
        try { server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false); } catch (IOException e) {}

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000).setMinUpdateIntervalMillis(10000).build();
        locationCallback = new LocationCallback() {
            @Override public void onLocationResult(@NonNull LocationResult result) {
                if (result == null) return;
                for (Location location : result.getLocations()) { processLocation(location); }
            }
        };
        try { fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper()); } catch (SecurityException e) {}
    }

    private void processLocation(Location location) {
        if (location == null) return;
        long now = System.currentTimeMillis();
        
        List<LocationEntity> locs = db.locationDao().getAll();
        if (locs == null || locs.isEmpty()) return;

        // ✅ 日付またぎの自動分割（ミッドナイト・カット）
        checkMidnightCrossing(now, locs);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now));
        android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE);
        boolean isMock = prefs.getBoolean("is_mock", false);
        double lat = isMock ? Double.longBitsToDouble(prefs.getLong("mock_lat", 0)) : location.getLatitude();
        double lng = isMock ? Double.longBitsToDouble(prefs.getLong("mock_lng", 0)) : location.getLongitude();

        sendBroadcast(new Intent("GPS_LOCATION_UPDATE").putExtra("lat", lat).putExtra("lng", lng).putExtra("is_mock", isMock).putExtra("fix_time", now));

        for (LocationEntity loc : locs) {
            float[] dist = new float[1];
            Location.distanceBetween(lat, lng, loc.latitude, loc.longitude, dist);
            boolean isCurrentlyIn = (dist[0] <= 20.0f);

            VisitHistory lastH = db.locationDao().getLastHistory(loc.id);
            if (lastH == null) {
                if (isCurrentlyIn) insertH(loc.id, today, now, true);
            } else if (isCurrentlyIn != lastH.isEntry) {
                insertH(loc.id, today, now, isCurrentlyIn);
            }
        }
    }

    private void checkMidnightCrossing(long now, List<LocationEntity> locs) {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now));
        android.content.SharedPreferences prefs = getSharedPreferences("gps_state", Context.MODE_PRIVATE);
        String lastDateStr = prefs.getString("last_date", todayStr);

        if (!lastDateStr.equals(todayStr)) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
            long startOfToday = cal.getTimeInMillis();
            long endOfYesterday = startOfToday - 1;

            for (LocationEntity loc : locs) {
                VisitHistory lastH = db.locationDao().getLastHistoryBefore(loc.id, startOfToday);
                if (lastH != null && lastH.isEntry) {
                    insertH(loc.id, lastDateStr, endOfYesterday, false);
                    insertH(loc.id, todayStr, startOfToday, true);
                }
            }
            prefs.edit().putString("last_date", todayStr).apply();
        }
    }

    private void insertH(int lid, String d, long ts, boolean ent) {
        VisitHistory vh = new VisitHistory(); vh.locationId = lid; vh.date = d; vh.timestamp = ts; vh.isEntry = ent;
        db.locationDao().insertHistory(vh);
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), this.getClass()); restart.setPackage(getPackageName());
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 1, restart, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pi);
        super.onTaskRemoved(rootIntent);
    }

    private static class SimpleLog { String name; String in_hour; String out_hour; SimpleLog(String n, String i, String o) { this.name = n; this.in_hour = i; this.out_hour = o; } }

    private class MyWebServer extends NanoHTTPD {
        public MyWebServer(int port) { super(port); }
        @Override public Response serve(IHTTPSession session) {
            if ("/logs".equals(session.getUri())) {
                Calendar cal = Calendar.getInstance();
                long now = System.currentTimeMillis();
                List<SimpleLog> results = new ArrayList<>();
                for (LocationEntity loc : db.locationDao().getAll()) {
                    long[] times = AppDatabase.calcTimes(db.locationDao(), loc.id, cal, true, now);
                    results.add(new SimpleLog(loc.name, 
                        String.format(Locale.US, "%.2f", (double)times[0]/3600000.0),
                        String.format(Locale.US, "%.2f", (double)times[1]/3600000.0)));
                }
                Response r = newFixedLengthResponse(Response.Status.OK, "application/json", new Gson().toJson(results));
                r.addHeader("Access-Control-Allow-Origin", "*"); return r;
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GPS記録", NotificationManager.IMPORTANCE_MIN);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
    private Notification getNotification(String text) { return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("GoalGPS").setContentText(text).setSmallIcon(android.R.drawable.ic_menu_mylocation).setPriority(NotificationCompat.PRIORITY_MIN).build(); }
    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }
    @Override public void onDestroy() { super.onDestroy(); if (server != null) server.stop(); }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
