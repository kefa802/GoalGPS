package com.example.gpslog;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatusBanner, tvDate, tvHeaderIn, tvHeaderOut;
    private Switch switchRecord;
    private RecyclerView rvLogs;
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Calendar displayDate = Calendar.getInstance(); // ✅ 表示中の日付管理

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

        // 単位（分/時）の切り替え
        rgUnit.setOnCheckedChangeListener((group, checkedId) -> {
            isHourUnit = (checkedId == R.id.rbHour);
            String unitLabel = isHourUnit ? "(時)" : "(分)";
            tvHeaderIn.setText("IN時間" + unitLabel);
            tvHeaderOut.setText("OUT時間" + unitLabel);
            refreshLogs();
        });

        // 記録スイッチ ✅
        switchRecord.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        updateUI(isServiceRunning(GpsLoggingService.class));
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        
        updateDateDisplay(); // 初期表示
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN);
        tvDate.setText(sdf.format(displayDate.getTime()));
        refreshLogs();
    }

    private void refreshLogs() {
        // 全ログを取得し、表示中にその場で移動時間を計算するロジック
        List<LocationLogEntity> logs = db.locationDao().getAllLogs();
        
        rvLogs.setAdapter(new RecyclerView.Adapter<LogViewHolder>() {
            @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
            }

            @Override public void onBindViewHolder(@NonNull LogViewHolder h, int position) {
                LocationLogEntity current = logs.get(position);
                h.tvName.setText(current.locationName);

                // ✅ IN時間（滞在時間）の計算
                // まだ滞在中の場合は「今までの時間」を計算
                long stayMs = (current.exitTime == 0) ? (System.currentTimeMillis() - current.entryTime) : current.stayDuration;
                h.tvIn.setText(formatDuration(stayMs));

                // ✅ OUT時間（移動時間）の計算
                // ひとつ古いログ（リストでは次の要素）の退室から、今の入室までの差
                if (position + 1 < logs.size()) {
                    LocationLogEntity previous = logs.get(position + 1);
                    if (previous.exitTime != 0) {
                        long moveMs = current.entryTime - previous.exitTime;
                        h.tvOut.setText(formatDuration(moveMs));
                    } else { h.tvOut.setText("--"); }
                } else {
                    h.tvOut.setText("始動"); // 最初の地点
                }
            }
            @Override public int getItemCount() { return logs.size(); }
        });
    }

    private String formatDuration(long millis) {
        // ミリ秒を 分 または 時 に変換して「5:29」のような形式にする
        long totalSeconds = millis / 1000;
        long totalMinutes = totalSeconds / 60;
        
        if (isHourUnit) {
            double hours = (double) totalMinutes / 60;
            return String.format(Locale.JAPAN, "%.2f", hours);
        } else {
            long minutes = totalMinutes;
            long seconds = totalSeconds % 60;
            return String.format(Locale.JAPAN, "%d:%02d", minutes, seconds); // ✅ スケッチの「5:29」形式
        }
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

    private boolean isServiceRunning(Class<?> sc) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    @Override protected void onResume() { super.onResume(); refreshLogs(); }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIn, tvOut;
        LogViewHolder(View v) { 
            super(v); 
            tvName = v.findViewById(R.id.tvLogName); 
            tvIn = v.findViewById(R.id.tvLogInTime); 
            tvOut = v.findViewById(R.id.tvLogOutTime); 
        }
    }
}
