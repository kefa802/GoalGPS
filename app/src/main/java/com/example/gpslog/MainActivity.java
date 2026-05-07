package com.example.gpslog;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
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
    private List<LocationEntity> masterLocations;

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
        });

        switchRecord.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        rvLogs.setLayoutManager(new LinearLayoutManager(this));

        updateRunnable = new Runnable() {
            @Override public void run() {
                refreshDashboard();
                updateHandler.postDelayed(this, 1000);
            }
        };
    }

    private void refreshDashboard() {
        masterLocations = db.locationDao().getAll();
        long now = System.currentTimeMillis();
        
        rvLogs.setAdapter(new RecyclerView.Adapter<LogViewHolder>() {
            @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
            }

            @Override public void onBindViewHolder(@NonNull LogViewHolder h, int position) {
                LocationEntity loc = masterLocations.get(position);
                LocationLogEntity latest = db.locationDao().getLatestLog(loc.id);
                h.tvName.setText(loc.name);

                // ✅ 改善③：全地点でのIN/OUTカウントロジック
                if (latest != null) {
                    if (latest.exitTime == 0) {
                        // 今まさに滞在中：INをカウント、OUTは停止
                        h.tvIn.setText(formatDuration(now - latest.entryTime));
                        h.tvIn.setBackgroundColor(Color.parseColor("#C8E6C9")); // 緑
                        h.tvOut.setText("--:--");
                        h.tvOut.setBackgroundColor(Color.TRANSPARENT);
                    } else {
                        // 離れている：INは前回の滞在時間、OUTをカウント
                        h.tvIn.setText(formatDuration(latest.stayDuration));
                        h.tvIn.setBackgroundColor(Color.TRANSPARENT);
                        h.tvOut.setText(formatDuration(now - latest.exitTime));
                        h.tvOut.setBackgroundColor(Color.parseColor("#FFCDD2")); // 薄赤
                    }
                } else {
                    h.tvIn.setText("未訪");
                    h.tvOut.setText("始動");
                }

                // ✅ 改善① & ②：入れ替えと削除ボタンの処理
                h.btnUp.setOnClickListener(v -> { if (position > 0) swap(position, position - 1); });
                h.btnDown.setOnClickListener(v -> { if (position < masterLocations.size() - 1) swap(position, position + 1); });
                h.btnDel.setOnClickListener(v -> { db.locationDao().delete(loc); Toast.makeText(MainActivity.this, "削除しました", Toast.LENGTH_SHORT).show(); });
            }
            @Override public int getItemCount() { return masterLocations.size(); }
        });
    }

    private void swap(int f, int t) {
        LocationEntity from = masterLocations.get(f);
        LocationEntity to = masterLocations.get(t);
        int temp = from.displayOrder;
        from.displayOrder = to.displayOrder;
        to.displayOrder = temp;
        db.locationDao().update(from);
        db.locationDao().update(to);
    }

    private String formatDuration(long millis) {
        long sec = millis / 1000;
        if (isHourUnit) return String.format(Locale.JAPAN, "%.2f", (double) sec / 3600);
        return String.format(Locale.JAPAN, "%d:%02d", sec / 60, sec % 60);
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

    @Override protected void onResume() { super.onResume(); updateUI(isServiceRunning(GpsLoggingService.class)); updateHandler.post(updateRunnable); }
    @Override protected void onPause() { super.onPause(); updateHandler.removeCallbacks(updateRunnable); }

    private boolean isServiceRunning(Class<?> sc) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIn, tvOut; Button btnUp, btnDown, btnDel;
        LogViewHolder(View v) { super(v); tvName = v.findViewById(R.id.tvLogName); tvIn = v.findViewById(R.id.tvLogInTime); tvOut = v.findViewById(R.id.tvLogOutTime); btnUp = v.findViewById(R.id.btnRowUp); btnDown = v.findViewById(R.id.btnRowDown); btnDel = v.findViewById(R.id.btnRowDel); }
    }
}
