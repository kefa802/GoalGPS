package com.example.gpslog;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GpsLoggingService extends Service {
    private static final String CHANNEL_ID = "gps_service_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;
    private MyWebServer server;
    private long lastProcessTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        startForeground(1, getNotification("GPSログ記録中..."));
        server = new MyWebServer(8080);
        try { server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false); } catch (IOException e) {}

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000).setMinUpdateIntervalMillis(10000).build();

        locationCallback = new LocationCallback() {
            @Override public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) { processLocation(location); }
            }
        };
        try { fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper()); } catch (SecurityException e) {}
    }

    private void processLocation(Location location) {
        long now = System.currentTimeMillis();
        long elapsed = 15000;
        if (lastProcessTime > 0) { elapsed = now - lastProcessTime; if (elapsed < 0) elapsed = 15000; }
        lastProcessTime = now;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        
        android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE);
        boolean isMock = prefs.getBoolean("is_mock", false);
        double lat = isMock ? Double.longBitsToDouble(prefs.getLong("mock_lat", 0)) : location.getLatitude();
        double lng = isMock ? Double.longBitsToDouble(prefs.getLong("mock_lng", 0)) : location.getLongitude();

        Intent intent = new Intent("GPS_LOCATION_UPDATE");
        intent.putExtra("lat", lat); intent.putExtra("lng", lng); intent.putExtra("is_mock", isMock); intent.putExtra("fix_time", now);
        sendBroadcast(intent);

        List<LocationEntity> locs = db.locationDao().getAll();
        for (LocationEntity loc : locs) {
            float[] dist = new float[1];
            Location.distanceBetween(lat, lng, loc.latitude, loc.longitude, dist);
            boolean isCurrentlyIn = (dist[0] <= 20.0f);

            // 1. 積算ロジックの更新
            DailyAccumulator data = db.locationDao().getDaily(loc.id, today);
            if (data == null) {
                data = new DailyAccumulator(); data.locationId = loc.id; data.date = today;
                db.locationDao().insertDaily(data);
                data = db.locationDao().getDaily(loc.id, today);
            }
            if (isCurrentlyIn) { data.totalInMs += elapsed; } else { data.totalOutMs += elapsed; }
            db.locationDao().updateDaily(data);

            // 2. 履歴（IN/OUT）の検知ロジック
            VisitHistory lastHistory = db.locationDao().getLastHistory(loc.id);
            boolean wasIn = (lastHistory != null && lastHistory.isEntry);

            if (lastHistory == null) {
                if (isCurrentlyIn) {
                    VisitHistory h = new VisitHistory(); h.locationId = loc.id; h.date = today; h.timestamp = now; h.isEntry = true;
                    db.locationDao().insertHistory(h);
                }
            } else if (isCurrentlyIn != wasIn) {
                VisitHistory h = new VisitHistory(); h.locationId = loc.id; h.date = today; h.timestamp = now; h.isEntry = isCurrentlyIn;
                db.locationDao().insertHistory(h);
            }
        }
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), this.getClass()); restart.setPackage(getPackageName());
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 1, restart, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pi);
        super.onTaskRemoved(rootIntent);
    }

    // ✅ 復活：JSONライブラリが正しく読み取れる専用の箱
    private static class SimpleLog {
        String name; String in_hour; String out_hour;
        SimpleLog(String n, String i, String o) { this.name = n; this.in_hour = i; this.out_hour = o; }
    }

    private class MyWebServer extends NanoHTTPD {
        public MyWebServer(int port) { super(port); }
        @Override public Response serve(IHTTPSession session) {
            // ✅ /logs へのアクセス時のみJSONを返すように修正
            if ("/logs".equals(session.getUri())) {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                List<SimpleLog> results = new ArrayList<>();
                for (LocationEntity loc : db.locationDao().getAll()) {
                    DailyAccumulator data = db.locationDao().getDaily(loc.id, today);
                    String inH = (data != null) ? String.format(Locale.US, "%.2f", (double)data.totalInMs / 3600000.0) : "0.00";
                    String outH = (data != null) ? String.format(Locale.US, "%.2f", (double)data.totalOutMs / 3600000.0) : "0.00";
                    results.add(new SimpleLog(loc.name, inH, outH));
                }
                Response r = newFixedLengthResponse(Response.Status.OK, "application/json", new Gson().toJson(results));
                r.addHeader("Access-Control-Allow-Origin", "*");
                return r;
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GPS記録サービス", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager)getSystemService(NotificationManager.class)).createNotificationChannel(channel);
        }
    }
    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("GoalGPS").setContentText(text).setSmallIcon(android.R.drawable.ic_menu_mylocation).setPriority(NotificationCompat.PRIORITY_MIN).build();
    }
    @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }
    @Override public void onDestroy() { super.onDestroy(); if (server != null) server.stop(); }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
