package com.example.gpslog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;
import com.google.android.gms.location.*;
import java.util.List;

public class GpsLoggingService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;
    private static final float GEOFENCE_RADIUS = 100.0f;

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db")
                .allowMainThreadQueries().build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                checkGeofence(locationResult.getLastLocation());
            }
        };
    }

    private void checkGeofence(Location currentLoc) {
        List<LocationEntity> locations = db.locationDao().getAll();
        long now = System.currentTimeMillis();
        for (LocationEntity loc : locations) {
            float[] results = new float[1];
            Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(), loc.latitude, loc.longitude, results);
            float distance = results[0];
            LocationLogEntity activeLog = db.locationDao().getActiveLog(loc.id);
            if (distance < GEOFENCE_RADIUS) {
                if (activeLog == null) {
                    LocationLogEntity newLog = new LocationLogEntity();
                    newLog.locationId = loc.id;
                    newLog.locationName = loc.name;
                    newLog.entryTime = now;
                    newLog.exitTime = 0;
                    db.locationDao().insertLog(newLog);
                }
            } else {
                if (activeLog != null) {
                    activeLog.exitTime = now;
                    activeLog.stayDuration = activeLog.exitTime - activeLog.entryTime;
                    db.locationDao().updateLog(activeLog);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "gps_channel")
                .setContentTitle("GoalGPS 稼働中")
                .setContentText("自動滞在記録を行っています")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
        startForeground(1, notification);
        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build();
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("gps_channel", "GPS Service", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
