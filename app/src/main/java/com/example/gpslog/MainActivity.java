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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatusBanner, tvDate, tvHeaderIn, tvHeaderOut, tvVersion;
    private Switch switchRecord;
    private RecyclerView rvLogs;
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Calendar displayDate = Calendar.getInstance();
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
        tvVersion = findViewById(R.id.tvVersion);
        switchRecord = findViewById(R.id.switchRecord);
        rvLogs = findViewById(R.id.rvDashboardLogs);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvVersion.setText("Ver: " + BuildConfig.VERSION_NAME);

        rvLogs.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnPrevDay).setOnClickListener(v -> changeDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> changeDate(1));

        ((RadioGroup)findViewById(R.id.rgUnit)).setOnCheckedChangeListener((g, id) -> {
            isHourUnit = (id == R.id.rbHour);
            String u = isHourUnit ? "(時)" : "(分)";
            tvHeaderIn.setText("IN" + u); tvHeaderOut.setText("OUT" + u);
            refreshDashboard();
        });

        switchRecord.setOnClickListener(v -> {
            if (switchRecord.isChecked()) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        updateRunnable = new Runnable() {
            @Override public void run() {
                if (isToday()) refreshDashboard(); // ✅ 昔動いていた手法：毎秒シンプルにリストを再描画
                updateHandler.postDelayed(this, 1000);
            }
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
        return today.get(Calendar.YEAR) == displayDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == displayDate.get(Calendar.DAY_OF_YEAR);
    }

    // ✅ 小細工なし、一番確実なリスト描画ロジック
    private void refreshDashboard() {
        List<LocationEntity> locs = db.locationDao().getAll();
        
        rvLogs.setAdapter(new RecyclerView.Adapter<LogViewHolder>() {
            @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
            }

            @Override public void onBindViewHolder(@NonNull LogViewHolder h, int pos) {
                LocationEntity loc = locs.get(pos);
                h.name.setText(loc.name != null ? loc.name : "不明");

                Calendar endCal = (Calendar) displayDate.clone();
                endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
                long endOfDay = endCal.getTimeInMillis();

                Calendar startCal = (Calendar) displayDate.clone();
                startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0);
                long startOfDay = startCal.getTimeInMillis();

                LocationLogEntity latest = db.locationDao().getLatestLog(loc.id, endOfDay);
                
                boolean today = isToday();
                long referenceTime = today ? System.currentTimeMillis() : endOfDay;

                if (latest != null && latest.exitTime == 0 && today) {
                    h.in.setText(formatDuration(referenceTime - latest.entryTime));
                    h.in.setBackgroundColor(Color.parseColor("#C8E6C9"));
                    h.out.setText("0:00");
                    h.out.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    h.in.setBackgroundColor(Color.TRANSPARENT);
                    h.in.setText(latest != null ? formatDuration(latest.stayDuration) : "0:00");
                    
                    if (pos == 0) {
                        h.out.setText("0:00");
                        h.out.setBackgroundColor(Color.TRANSPARENT);
                    } else {
                        long outMs;
                        if (latest != null) {
                            outMs = referenceTime - (latest.exitTime == 0 ? referenceTime : latest.exitTime);
                        } else {
                            outMs = referenceTime - startOfDay;
                        }
                        h.out.setText(formatDuration(outMs));
                        h.out.setBackgroundColor(today ? Color.parseColor("#FFCDD2") : Color.TRANSPARENT);
                    }
                }

                h.btnUp.setOnClickListener(v -> { if (pos > 0) swap(pos, pos - 1); });
                h.btnDown.setOnClickListener(v -> { if (pos < locs.size() - 1) swap(pos, pos + 1); });
                h.btnDel.setOnClickListener(v -> { 
                    db.locationDao().delete(loc); 
                    refreshDashboard(); 
                });
            }
            @Override public int getItemCount() { return locs.size(); }
        });
    }

    private void swap(int f, int t) {
        List<LocationEntity> list = db.locationDao().getAll();
        LocationEntity from = list.get(f), to = list.get(t);
        int temp = from.displayOrder;
        from.displayOrder = to.displayOrder;
        to.displayOrder = temp;
        db.locationDao().update(from); db.locationDao().update(to);
        refreshDashboard();
    }

    private String formatDuration(long ms) {
        if (ms < 0) ms = 0;
        long sec = ms / 1000;
        if (isHourUnit) return String.format(Locale.JAPAN, "%.2f", (double) sec / 3600);
        return String.format(Locale.JAPAN, "%d:%02d", sec / 60, sec % 60);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(isServiceRunning(GpsLoggingService.class));
        refreshDashboard();
        updateHandler.post(updateRunnable);
    }

    @Override
    protected void onPause() { super.onPause(); updateHandler.removeCallbacks(updateRunnable); }

    private void updateUI(boolean run) {
        switchRecord.setChecked(run);
        tvStatusBanner.setText(run ? "オンライン：自動記録中" : "オフライン");
        tvStatusBanner.setBackgroundColor(run ? Color.parseColor("#4CAF50") : Color.parseColor("#9E9E9E"));
    }

    private void startGpsService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            switchRecord.setChecked(false);
            List<String> perms = new ArrayList<>();
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), 1);
            Toast.makeText(this, "位置情報の権限を許可してください", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            startForegroundService(new Intent(this, GpsLoggingService.class));
            updateUI(true);
        } catch (Exception e) {
            switchRecord.setChecked(false);
            Toast.makeText(this, "エラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopGpsService() { stopService(new Intent(this, GpsLoggingService.class)); updateUI(false); }

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
