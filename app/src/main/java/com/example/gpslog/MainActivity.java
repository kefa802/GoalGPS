package com.example.gpslog;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatusBanner, tvDate, tvHeaderIn, tvHeaderOut;
    private Switch switchRecord;
    private RecyclerView rvLogs;
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvDate = findViewById(R.id.tvDate);
        tvHeaderIn = findViewById(R.id.tvHeaderIn);
        tvHeaderOut = findViewById(R.id.tvHeaderOut);
        switchRecord = findViewById(R.id.switchRecord);
        rvLogs = findViewById(R.id.rvDashboardLogs);
        RadioGroup rgUnit = findViewById(R.id.rgUnit);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvDate.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(new Date()));

        rgUnit.setOnCheckedChangeListener((group, checkedId) -> {
            isHourUnit = (checkedId == R.id.rbHour);
            String unitLabel = isHourUnit ? "(時)" : "(分)";
            tvHeaderIn.setText("IN時間" + unitLabel);
            tvHeaderOut.setText("OUT時間" + unitLabel);
            refreshLogs();
        });

        switchRecord.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        rvLogs.setLayoutManager(new LinearLayoutManager(this));

        // ✅ 1秒ごとに表示を更新するタイマー設定
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLogs();
                updateHandler.postDelayed(this, 1000);
            }
        };
    }

    private void refreshLogs() {
        List<LocationLogEntity> logs = db.locationDao().getAllLogs();
        rvLogs.setAdapter(new RecyclerView.Adapter<LogViewHolder>() {
            @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
            }
            @Override public void onBindViewHolder(@NonNull LogViewHolder h, int p) {
                LocationLogEntity current = logs.get(p);
                h.tvName.setText(current.locationName);
                long stayMs = (current.exitTime == 0) ? (System.currentTimeMillis() - current.entryTime) : current.stayDuration;
                h.tvIn.setText(formatDuration(stayMs));
                if (p + 1 < logs.size()) {
                    LocationLogEntity previous = logs.get(p + 1);
                    h.tvOut.setText(previous.exitTime != 0 ? formatDuration(current.entryTime - previous.exitTime) : "--");
                } else { h.tvOut.setText("始動"); }
            }
            @Override public int getItemCount() { return logs.size(); }
        });
    }

    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        if (isHourUnit) return String.format(Locale.JAPAN, "%.2f", (double) totalSeconds / 3600);
        return String.format(Locale.JAPAN, "%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void updateUI(boolean isRunning) {
        switchRecord.setChecked(isRunning);
        tvStatusBanner.setText(isRunning ? "オンライン：自動記録中" : "オフライン");
        tvStatusBanner.setBackgroundColor(isRunning ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E"));
    }

    private void startGpsService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        startForegroundService(new Intent(this, GpsLoggingService.class));
        updateUI(true);
    }

    private void stopGpsService() {
        stopService(new Intent(this, GpsLoggingService.class));
        updateUI(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(isServiceRunning(GpsLoggingService.class));
        updateHandler.post(updateRunnable); // タイマー開始
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateHandler.removeCallbacks(updateRunnable); // タイマー停止
    }

    private boolean isServiceRunning(Class<?> sc) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIn, tvOut;
        LogViewHolder(View v) { super(v); tvName = v.findViewById(R.id.tvLogName); tvIn = v.findViewById(R.id.tvLogInTime); tvOut = v.findViewById(R.id.tvLogOutTime); }
    }
}
