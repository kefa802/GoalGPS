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
    private LogAdapter adapter;
    private boolean isHourUnit = false;
    private Calendar displayDate = Calendar.getInstance();
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
        
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();

        // アダプターを1回だけセット（消えるバグの修正）
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
        rvLogs.setAdapter(adapter);

        findViewById(R.id.btnPrevDay).setOnClickListener(v -> changeDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> changeDate(1));

        ((RadioGroup)findViewById(R.id.rgUnit)).setOnCheckedChangeListener((g, id) -> {
            isHourUnit = (id == R.id.rbHour);
            String unit = isHourUnit ? "(時)" : "(分)";
            tvHeaderIn.setText("IN" + unit); tvHeaderOut.setText("OUT" + unit);
            refreshDashboard();
        });

        switchRecord.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        updateRunnable = () -> {
            if (isToday()) refreshDashboard(); 
            updateHandler.postDelayed(updateRunnable, 1000);
        };
        updateDateDisplay();
    }

    private void changeDate(int amount) {
        displayDate.add(Calendar.DATE, amount);
        updateDateDisplay();
    }

    private void updateDateDisplay() {
        tvDate.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(displayDate.getTime()));
        refreshDashboard();
    }

    private boolean isToday() {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == displayDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == displayDate.get(Calendar.DAY_OF_YEAR);
    }

    private void refreshDashboard() {
        masterLocations = db.locationDao().getAll();
        adapter.notifyDataSetChanged(); // UIを安全に更新
    }

    private String formatDuration(long ms) {
        if (ms < 0) ms = 0;
        long sec = ms / 1000;
        if (isHourUnit) return String.format(Locale.JAPAN, "%.2f", (double) sec / 3600);
        return String.format(Locale.JAPAN, "%d:%02d", sec / 60, sec % 60);
    }

    class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
        }

        @Override public void onBindViewHolder(@NonNull LogViewHolder h, int p) {
            LocationEntity loc = masterLocations.get(p);
            h.name.setText(loc.name);

            // 指定した日の終わり（23:59:59）
            Calendar endCal = (Calendar) displayDate.clone();
            endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
            long endOfDay = endCal.getTimeInMillis();
            
            // 指定した日の始まり（00:00:00）
            Calendar startCal = (Calendar) displayDate.clone();
            startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0);
            long startOfDay = startCal.getTimeInMillis();

            LocationLogEntity latest = db.locationDao().getLatestLog(loc.id, endOfDay);
            boolean today = isToday();
            long referenceTime = today ? System.currentTimeMillis() : endOfDay;

            if (latest != null && latest.exitTime == 0 && today) {
                // ✅ 滞在中
                h.in.setText(formatDuration(referenceTime - latest.entryTime));
                h.in.setBackgroundColor(Color.parseColor("#C8E6C9")); // 薄緑
                h.out.setText("0:00");
                h.out.setBackgroundColor(Color.TRANSPARENT);
            } else {
                // ✅ 地点の外
                h.in.setBackgroundColor(Color.TRANSPARENT);
                h.in.setText(latest != null ? formatDuration(latest.stayDuration) : "0:00");

                if (p == 0) {
                    // ① 1つ目の地点（拠点）: 外にいるなら 0:00
                    h.out.setText("0:00");
                    h.out.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    // ③ 2つ目以降（訪問先）: 外にいる間カウントアップ
                    long outMs;
                    if (latest != null) {
                        outMs = referenceTime - (latest.exitTime == 0 ? referenceTime : latest.exitTime);
                    } else {
                        // 未訪の場合は「その日の始まり」からの時間をカウント
                        outMs = referenceTime - startOfDay; 
                    }
                    h.out.setText(formatDuration(outMs));
                    h.out.setBackgroundColor(today ? Color.parseColor("#FFCDD2") : Color.TRANSPARENT); // 薄赤
                }
            }

            h.btnUp.setOnClickListener(v -> { if (p > 0) swap(p, p - 1); });
            h.btnDown.setOnClickListener(v -> { if (p < masterLocations.size() - 1) swap(p, p + 1); });
            h.btnDel.setOnClickListener(v -> { db.locationDao().delete(loc); refreshDashboard(); });
        }
        @Override public int getItemCount() { return masterLocations != null ? masterLocations.size() : 0; }
    }

    private void swap(int f, int t) {
        LocationEntity from = masterLocations.get(f), to = masterLocations.get(t);
        int temp = from.displayOrder;
        from.displayOrder = to.displayOrder;
        to.displayOrder = temp;
        db.locationDao().update(from); db.locationDao().update(to);
        refreshDashboard();
    }

    private void startGpsService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1); return;
        }
        startForegroundService(new Intent(this, GpsLoggingService.class));
        updateUI(true);
    }

    private void stopGpsService() { stopService(new Intent(this, GpsLoggingService.class)); updateUI(false); }
    private void updateUI(boolean run) {
        switchRecord.setChecked(run);
        tvStatusBanner.setText(run ? "オンライン：自動記録中" : "オフライン");
        tvStatusBanner.setBackgroundColor(run ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E"));
    }

    @Override protected void onResume() { super.onResume(); updateUI(isServiceRunning(GpsLoggingService.class)); refreshDashboard(); updateHandler.post(updateRunnable); }
    @Override protected void onPause() { super.onPause(); updateHandler.removeCallbacks(updateRunnable); }

    private boolean isServiceRunning(Class<?> sc) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView name, in, out; Button btnUp, btnDown, btnDel;
        LogViewHolder(View v) { super(v); name = v.findViewById(R.id.tvLogName); in = v.findViewById(R.id.tvLogInTime); out = v.findViewById(R.id.tvLogOutTime); btnUp = v.findViewById(R.id.btnRowUp); btnDown = v.findViewById(R.id.btnRowDown); btnDel = v.findViewById(R.id.btnRowDel); }
    }
}
