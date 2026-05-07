package com.example.gpslog;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatusBanner, tvVersion, tvRawLog;
    private Switch switchRecord;
    private AppDatabase db;
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvVersion = findViewById(R.id.tvVersion);
        tvRawLog = findViewById(R.id.tvRawLog);
        switchRecord = findViewById(R.id.switchRecord);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvVersion.setText("Ver: " + BuildConfig.VERSION_NAME);

        switchRecord.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        // 1秒ごとにログ画面を更新
        updateRunnable = new Runnable() {
            @Override public void run() {
                refreshRawLog();
                updateHandler.postDelayed(this, 1000);
            }
        };
    }

    private void refreshRawLog() {
        try {
            List<LocationEntity> locs = db.locationDao().getAll();
            StringBuilder sb = new StringBuilder();
            
            sb.append("=== 🕵️ データベース生データ ===\n");
            sb.append("更新時刻: ").append(new SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(new Date())).append("\n");
            sb.append("登録地点数: ").append(locs != null ? locs.size() : "null").append("件\n\n");

            if (locs != null) {
                for (LocationEntity loc : locs) {
                    sb.append("📍 [").append(loc.name).append("]\n");
                    sb.append("  ・ID: ").append(loc.id).append("\n");
                    
                    LocationLogEntity log = db.locationDao().getLatestLog(loc.id, System.currentTimeMillis());
                    if (log != null) {
                        sb.append("  ・最終IN : ").append(log.entryTime).append("\n");
                        sb.append("  ・最終OUT: ").append(log.exitTime == 0 ? "滞在中(0)" : log.exitTime).append("\n");
                    } else {
                        sb.append("  ・履歴ログなし\n");
                    }
                    sb.append("------------------------\n");
                }
            }
            tvRawLog.setText(sb.toString());
        } catch (Exception e) {
            tvRawLog.setText("❌ エラー発生:\n" + e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(isServiceRunning(GpsLoggingService.class));
        refreshRawLog();
        updateHandler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void updateUI(boolean run) {
        switchRecord.setChecked(run);
        tvStatusBanner.setText(run ? "オンライン：自動記録中" : "オフライン");
        tvStatusBanner.setBackgroundColor(run ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E"));
    }

    private void startGpsService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        startForegroundService(new Intent(this, GpsLoggingService.class));
        updateUI(true);
    }

    private void stopGpsService() { stopService(new Intent(this, GpsLoggingService.class)); updateUI(false); }

    private boolean isServiceRunning(Class<?> sc) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }
}
