package com.example.gpslog;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatusBanner, tvDate, tvHeaderIn, tvHeaderOut, tvVersion;
    private Switch switchRecord;
    private RecyclerView rvLogs;
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;
    // ✅ クラッシュ防止のため空リストで初期化
    private List<LocationEntity> masterLocations = new ArrayList<>();
    private LocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvDate = findViewById(R.id.tvDate);
        tvHeaderIn = findViewById(R.id.tvHeaderIn);
        tvHeaderOut = findViewById(R.id.tvHeaderOut);
        tvVersion = findViewById(R.id.tvVersion); // ✅ 追加
        switchRecord = findViewById(R.id.switchRecord);
        rvLogs = findViewById(R.id.rvDashboardLogs);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvDate.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(new Date()));

        // ✅ build.gradle で生成されたバージョン名を表示
        String vName = BuildConfig.VERSION_NAME;
        tvVersion.setText("Ver: " + vName);

        // ✅ リストの準備を onCreate で完了させる
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LocationAdapter();
        rvLogs.setAdapter(adapter);

        ((RadioGroup)findViewById(R.id.rgUnit)).setOnCheckedChangeListener((g, id) -> {
            isHourUnit = (id == R.id.rbHour);
            String unitLabel = isHourUnit ? "(時)" : "(分)";
            tvHeaderIn.setText("IN" + unitLabel);
            tvHeaderOut.setText("OUT" + unitLabel);
            // 単位切り替え時はリスト全体を更新
            refreshLocations();
        });

        switchRecord.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) { startGpsService(); } else { stopGpsService(); }
        });

        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        // ✅ 1秒毎の更新タイマー
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // UIのみを更新（地点リストの再取得はしない）
                adapter.notifyDataSetChanged();
                updateHandler.postDelayed(this, 1000);
            }
        };
    }

    private void refreshLocations() {
        // データベースから地点を取得
        masterLocations = db.locationDao().getAll();
        // ✅ 診断用ログ：地点数を Logcat に出力
        Log.d("GoalGPS", "Location count: " + masterLocations.size());
        adapter.notifyDataSetChanged(); // UI更新
    }

    private String formatDuration(long ms) {
        if (ms < 0) ms = 0;
        long totalSeconds = ms / 1000;
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.POST_NOTIFICATIONS}, 1);
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
        refreshLocations(); // 画面に戻るたびに地点リストを最新にする ✅
        updateHandler.post(updateRunnable); // タイマー開始 ✅
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateHandler.removeCallbacks(updateRunnable); // タイマー停止 ✅
    }

    private boolean isServiceRunning(Class<?> sc) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    // ✅ リストの表示ロジック（Adapter）
    class LocationAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
        }

        @Override public void onBindViewHolder(@NonNull LogViewHolder h, int position) {
            if (position >= masterLocations.size()) return;
            
            LocationEntity loc = masterLocations.get(position);
            LocationLogEntity latestLog = db.locationDao().getLatestLog(loc.id);
            h.tvName.setText(loc.name);

            long now = System.currentTimeMillis();

            if (latestLog != null && latestLog.exitTime == 0) {
                // ✅ 滞在中：INをカウント
                long stayMs = now - latestLog.entryTime;
                h.tvIn.setText(formatDuration(stayMs));
                h.tvIn.setBackgroundColor(Color.parseColor("#C8E6C9")); // 緑
                h.tvOut.setText("0:00");
                h.tvOut.setBackgroundColor(Color.TRANSPARENT);
            } else {
                // ✅ 地点の外
                h.in.setBackgroundColor(Color.TRANSPARENT);
                h.in.setText(latestLog != null ? formatDuration(latestLog.stayDuration) : "0:00");

                if (position == 0) {
                    // ① 1つ目の地点（Homeなど）: 外にいるなら 0:00 ✅
                    h.out.setText("0:00");
                    h.out.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    // ③ 2つ目以降（訪問先）: 外にいる間カウントアップ ✅
                    if (latestLog != null) {
                        long outMs = now - latestLog.exitTime;
                        h.out.setText(formatDuration(outMs));
                        h.out.setBackgroundColor(Color.parseColor("#FFCDD2")); // 赤
                    } else {
                        h.out.setText("始動");
                        h.out.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            }

            h.btnUp.setOnClickListener(v -> { if (position > 0) swap(position, position - 1); });
            h.btnDown.setOnClickListener(v -> { if (position < masterLocations.size() - 1) swap(position, position + 1); });
            h.btnDel.setOnClickListener(v -> { db.locationDao().delete(loc); refreshLocations(); Toast.makeText(MainActivity.this, "削除しました", Toast.LENGTH_SHORT).show(); });
        }
        @Override public int getItemCount() { return masterLocations.size(); }
    }

    private void swap(int f, int t) {
        if (f < 0 || f >= masterLocations.size() || t < 0 || t >= masterLocations.size()) return;
        
        LocationEntity fromLoc = masterLocations.get(f);
        LocationEntity toLoc = masterLocations.get(t);
        
        int tempOrder = fromLoc.displayOrder;
        fromLoc.displayOrder = toLoc.displayOrder;
        toLoc.displayOrder = tempOrder;
        
        db.locationDao().update(fromLoc);
        db.locationDao().update(toLoc);
        refreshLocations();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIn, tvOut; Button btnUp, btnDown, btnDel;
        LogViewHolder(View v) { super(v); tvName = v.findViewById(R.id.tvLogName); tvIn = v.findViewById(R.id.tvLogInTime); tvOut = v.findViewById(R.id.tvLogOutTime); btnUp = v.findViewById(R.id.btnRowUp); btnDown = v.findViewById(R.id.btnRowDown); btnDel = v.findViewById(R.id.btnRowDel); }
    }
}
