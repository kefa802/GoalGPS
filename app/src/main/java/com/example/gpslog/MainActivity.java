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
    private TextView tvStatusBanner, tvDate, tvHeaderIn, tvHeaderOut, tvVersion, tvDebugCount;
    private Switch switchRecord;
    private RecyclerView rvLogs;
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Calendar displayDate = Calendar.getInstance();
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;
    
    // ✅ 画面に渡すための「計算済み」データのリスト
    private List<DisplayItem> displayList = new ArrayList<>();
    private LocationAdapter adapter;

    // ✅ DBと画面を切り離すための専用の箱
    static class DisplayItem {
        LocationEntity loc;
        LocationLogEntity latestLog;
        DisplayItem(LocationEntity loc, LocationLogEntity latestLog) {
            this.loc = loc;
            this.latestLog = latestLog;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvDate = findViewById(R.id.tvDate);
        tvHeaderIn = findViewById(R.id.tvHeaderIn);
        tvHeaderOut = findViewById(R.id.tvHeaderOut);
        tvVersion = findViewById(R.id.tvVersion);
        tvDebugCount = findViewById(R.id.tvDebugCount);
        switchRecord = findViewById(R.id.switchRecord);
        rvLogs = findViewById(R.id.rvDashboardLogs);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvVersion.setText("Ver: " + BuildConfig.VERSION_NAME);

        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter();
        rvLogs.setAdapter(adapter);

        findViewById(R.id.btnPrevDay).setOnClickListener(v -> changeDate(-1));
        findViewById(R.id.btnNextDay).setOnClickListener(v -> changeDate(1));

        ((RadioGroup)findViewById(R.id.rgUnit)).setOnCheckedChangeListener((g, id) -> {
            isHourUnit = (id == R.id.rbHour);
            String u = isHourUnit ? "(時)" : "(分)";
            tvHeaderIn.setText("IN" + u); tvHeaderOut.setText("OUT" + u);
            refreshData();
        });

        switchRecord.setOnClickListener(v -> {
            if (switchRecord.isChecked()) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        updateRunnable = new Runnable() {
            @Override public void run() {
                if (isToday()) refreshData(); // 1秒ごとに安全にデータ再構築
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
        refreshData();
    }

    private boolean isToday() {
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == displayDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == displayDate.get(Calendar.DAY_OF_YEAR);
    }

    // ✅ 画面描画の前に、DBアクセスを全てここで終わらせておく
    private void refreshData() {
        new Thread(() -> {
            List<LocationEntity> freshLocs = db.locationDao().getAll();
            List<DisplayItem> newItems = new ArrayList<>();

            Calendar endCal = (Calendar) displayDate.clone();
            endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
            long endOfDay = endCal.getTimeInMillis();

            if (freshLocs != null) {
                for (LocationEntity loc : freshLocs) {
                    LocationLogEntity latest = null;
                    try {
                        latest = db.locationDao().getLatestLog(loc.id, endOfDay);
                    } catch (Exception e) {}
                    newItems.add(new DisplayItem(loc, latest));
                }
            }

            runOnUiThread(() -> {
                displayList.clear();
                displayList.addAll(newItems);
                tvDebugCount.setText("🚨 データベース内の地点数: " + displayList.size() + " 件");
                adapter.notifyDataSetChanged(); // 準備が完了したデータだけを画面に渡す
            });
        }).start();
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
        refreshData();
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

    // ✅ DBアクセスをしない、超軽量で絶対に真っ白にならないアダプター
    class LocationAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
        }

        @Override public void onBindViewHolder(@NonNull LogViewHolder h, int pos) {
            if (pos >= displayList.size()) return;
            
            try {
                DisplayItem item = displayList.get(pos);
                h.name.setText(item.loc.name != null ? item.loc.name : "不明な地点");

                Calendar endCal = (Calendar) displayDate.clone();
                endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
                long endOfDay = endCal.getTimeInMillis();

                Calendar startCal = (Calendar) displayDate.clone();
                startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0);
                long startOfDay = startCal.getTimeInMillis();

                boolean today = isToday();
                long referenceTime = today ? System.currentTimeMillis() : endOfDay;

                if (item.latestLog != null && item.latestLog.exitTime == 0 && today) {
                    h.in.setText(formatDuration(referenceTime - item.latestLog.entryTime));
                    h.in.setBackgroundColor(Color.parseColor("#C8E6C9"));
                    h.out.setText("0:00");
                    h.out.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    h.in.setBackgroundColor(Color.TRANSPARENT);
                    h.in.setText(item.latestLog != null ? formatDuration(item.latestLog.stayDuration) : "0:00");
                    
                    if (pos == 0) {
                        h.out.setText("0:00");
                        h.out.setBackgroundColor(Color.TRANSPARENT);
                    } else {
                        long outMs;
                        if (item.latestLog != null) {
                            outMs = referenceTime - (item.latestLog.exitTime == 0 ? referenceTime : item.latestLog.exitTime);
                        } else {
                            outMs = referenceTime - startOfDay;
                        }
                        h.out.setText(formatDuration(outMs));
                        h.out.setBackgroundColor(today ? Color.parseColor("#FFCDD2") : Color.TRANSPARENT);
                    }
                }

                h.btnUp.setOnClickListener(v -> { if (pos > 0) swap(pos, pos - 1); });
                h.btnDown.setOnClickListener(v -> { if (pos < displayList.size() - 1) swap(pos, pos + 1); });
                h.btnDel.setOnClickListener(v -> { 
                    new Thread(() -> {
                        db.locationDao().delete(item.loc);
                        refreshData();
                    }).start();
                    Toast.makeText(MainActivity.this, "削除しました", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                h.name.setText("表示エラー");
            }
        }
        @Override public int getItemCount() { return displayList.size(); }
    }

    private void swap(int f, int t) {
        new Thread(() -> {
            LocationEntity from = displayList.get(f).loc;
            LocationEntity to = displayList.get(t).loc;
            int temp = from.displayOrder;
            from.displayOrder = to.displayOrder;
            to.displayOrder = temp;
            db.locationDao().update(from); 
            db.locationDao().update(to);
            refreshData();
        }).start();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView name, in, out; Button btnUp, btnDown, btnDel;
        LogViewHolder(View v) { super(v); name = v.findViewById(R.id.tvLogName); in = v.findViewById(R.id.tvLogInTime); out = v.findViewById(R.id.tvLogOutTime); btnUp = v.findViewById(R.id.btnRowUp); btnDown = v.findViewById(R.id.btnRowDown); btnDel = v.findViewById(R.id.btnRowDel); }
    }
}
