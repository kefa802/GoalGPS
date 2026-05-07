package com.example.gpslog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class GpsLoggingService extends Service {
    private static final String CHANNEL_ID = "gps_service_channel";
    private static final float GEOFENCE_RADIUS = 20.0f; 
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;
    private MyWebServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        startForeground(1, getNotification("GPSログ記録中..."));

        server = new MyWebServer(8080);
        try { server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false); } catch (IOException e) { e.printStackTrace(); }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).setMinUpdateIntervalMillis(2000).build();
        locationCallback = new LocationCallback() {
            @Override public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) { handleLocationUpdate(location); }
            }
        };
        try { fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()); } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void handleLocationUpdate(Location location) {
        long now = System.currentTimeMillis();
        android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE);
        boolean isMock = prefs.getBoolean("is_mock", false);
        double lat = isMock ? Double.longBitsToDouble(prefs.getLong("mock_lat", 0)) : location.getLatitude();
        double lng = isMock ? Double.longBitsToDouble(prefs.getLong("mock_lng", 0)) : location.getLongitude();

        Intent intent = new Intent("GPS_LOCATION_UPDATE");
        intent.putExtra("lat", lat); intent.putExtra("lng", lng); intent.putExtra("is_mock", isMock);
        sendBroadcast(intent);

        List<LocationEntity> locs = db.locationDao().getAll();
        if (locs != null) {
            for (LocationEntity loc : locs) {
                float[] results = new float[1];
                Location.distanceBetween(lat, lng, loc.latitude, loc.longitude, results);
                float distance = results[0];
                LocationLogEntity activeLog = db.locationDao().getActiveLog(loc.id);
                if (distance <= GEOFENCE_RADIUS) { 
                    if (activeLog == null) {
                        LocationLogEntity newLog = new LocationLogEntity();
                        newLog.locationId = loc.id; newLog.entryTime = now; newLog.exitTime = 0; newLog.stayDuration = 0;
                        db.locationDao().insertLog(newLog);
                    }
                } else { 
                    if (activeLog != null) {
                        activeLog.exitTime = now; activeLog.stayDuration = now - activeLog.entryTime;
                        db.locationDao().updateLog(activeLog);
                    }
                }
            }
        }
    }

    private static class SimpleLog {
        String name; String in_hour; String out_hour;
        SimpleLog(String n, String i, String o) { this.name = n; this.in_hour = i; this.out_hour = o; }
    }

    private class MyWebServer extends NanoHTTPD {
        public MyWebServer(int port) { super(port); }
        @Override
        public Response serve(IHTTPSession session) {
            if ("/logs".equals(session.getUri())) {
                List<SimpleLog> resultList = new ArrayList<>();
                List<LocationEntity> locs = db.locationDao().getAll();
                Calendar cal = Calendar.getInstance();
                long now = System.currentTimeMillis();
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
                long end = cal.getTimeInMillis();

                for (LocationEntity loc : locs) {
                    List<LocationLogEntity> dayLogs = db.locationDao().getLogsForDay(loc.id, start, end);
                    long totalIn = 0, totalOut = 0;
                    if (dayLogs != null && !dayLogs.isEmpty()) {
                        for (int i = 0; i < dayLogs.size(); i++) {
                            LocationLogEntity log = dayLogs.get(i);
                            totalIn += (log.exitTime == 0) ? (now - log.entryTime) : log.stayDuration;
                            if (i > 0) totalOut += (log.entryTime - dayLogs.get(i-1).exitTime);
                        }
                        LocationLogEntity last = dayLogs.get(dayLogs.size() - 1);
                        if (last.exitTime != 0) totalOut += (now - last.exitTime);
                    }
                    resultList.add(new SimpleLog(loc.name, 
                        String.format(Locale.US, "%.2f", (double)totalIn / 3600000.0), 
                        String.format(Locale.US, "%.2f", (double)totalOut / 3600000.0)));
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", new Gson().toJson(resultList));
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("GoalGPS").setContentText(text).setSmallIcon(android.R.drawable.ic_menu_mylocation).setPriority(NotificationCompat.PRIORITY_MIN).build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "GPS記録サービス", NotificationManager.IMPORTANCE_MIN);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() { super.onDestroy(); if (server != null) server.stop(); if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback); }
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
