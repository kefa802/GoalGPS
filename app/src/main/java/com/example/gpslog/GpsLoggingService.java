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

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        startForeground(1, getNotification("GPSログ記録中..."));
        server = new MyWebServer(8080);
        try { server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false); } catch (IOException e) {}

        // 15秒間隔で省電力モード
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000)
                .setMinUpdateIntervalMillis(10000)
                .build();

        locationCallback = new LocationCallback() {
            @Override public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) { processLocation(location); }
            }
        };
        try { fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper()); } catch (SecurityException e) {}
    }

    private void processLocation(Location location) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Intent intent = new Intent("GPS_LOCATION_UPDATE");
        intent.putExtra("lat", location.getLatitude()); intent.putExtra("lng", location.getLongitude());
        sendBroadcast(intent);

        List<LocationEntity> locs = db.locationDao().getAll();
        for (LocationEntity loc : locs) {
            float[] dist = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), loc.latitude, loc.longitude, dist);
            DailyAccumulator data = db.locationDao().getDaily(loc.id, today);
            if (data == null) {
                data = new DailyAccumulator(); data.locationId = loc.id; data.date = today;
                db.locationDao().insertDaily(data);
                data = db.locationDao().getDaily(loc.id, today);
            }
            if (dist[0] <= 20.0f) { data.totalInMs += 15000; } else { data.totalOutMs += 15000; }
            db.locationDao().updateDaily(data);
        }
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), this.getClass()); restart.setPackage(getPackageName());
        PendingIntent pi = PendingIntent.getService(getApplicationContext(), 1, restart, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        ((AlarmManager)getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pi);
        super.onTaskRemoved(rootIntent);
    }

    private class MyWebServer extends NanoHTTPD {
        public MyWebServer(int port) { super(port); }
        @Override public Response serve(IHTTPSession session) {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            List<Object> results = new ArrayList<>();
            for (LocationEntity loc : db.locationDao().getAll()) {
                DailyAccumulator data = db.locationDao().getDaily(loc.id, today);
                results.add(new Object(){
                    String name = loc.name;
                    String in_hour = (data != null) ? String.format(Locale.US, "%.2f", (double)data.totalInMs/3600000.0) : "0.00";
                    String out_hour = (data != null) ? String.format(Locale.US, "%.2f", (double)data.totalOutMs/3600000.0) : "0.00";
                });
            }
            Response r = newFixedLengthResponse(Response.Status.OK, "application/json", new Gson().toJson(results));
            r.addHeader("Access-Control-Allow-Origin", "*"); // CORS対応
            return r;
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
