package com.example.gpslog;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
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
import java.util.List;

public class GpsLoggingService extends Service {
    private static final String CHANNEL_ID = "gps_service_channel";
    private static final float GEOFENCE_RADIUS = 20.0f; // ✅ ④ IN/OUT判定を半径20mに設定
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();
        startForeground(1, getNotification("GPSログ記録中..."));

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    handleLocationUpdate(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void handleLocationUpdate(Location location) {
        long now = System.currentTimeMillis();

        // ✅ ① 現在地情報を MainActivity に送信（ブロードキャスト）
        Intent intent = new Intent("GPS_LOCATION_UPDATE");
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lng", location.getLongitude());
        sendBroadcast(intent);

        List<LocationEntity> locs = db.locationDao().getAll();
        if (locs != null) {
            for (LocationEntity loc : locs) {
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), loc.latitude, loc.longitude, results);
                float distance = results[0];

                LocationLogEntity activeLog = db.locationDao().getActiveLog(loc.id);

                if (distance <= GEOFENCE_RADIUS) { // 20m以内ならIN
                    if (activeLog == null) {
                        LocationLogEntity newLog = new LocationLogEntity();
                        newLog.locationId = loc.id;
                        newLog.entryTime = now;
                        newLog.exitTime = 0;
                        newLog.stayDuration = 0;
                        db.locationDao().insertLog(newLog);
                    }
                } else { // 20mより離れたらOUT
                    if (activeLog != null) {
                        activeLog.exitTime = now;
                        activeLog.stayDuration = now - activeLog.entryTime;
                        db.locationDao().updateLog(activeLog);
                    }
                }
            }
        }
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GoalGPS")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_MIN) // 通知を最小化
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "GPS記録サービス",
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        long now = System.currentTimeMillis();
        List<LocationEntity> locs = db.locationDao().getAll();
        if (locs != null) {
            for (LocationEntity loc : locs) {
                LocationLogEntity activeLog = db.locationDao().getActiveLog(loc.id);
                if (activeLog != null) {
                    activeLog.exitTime = now;
                    activeLog.stayDuration = now - activeLog.entryTime;
                    db.locationDao().updateLog(activeLog);
                }
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
