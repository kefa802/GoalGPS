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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
    private LinearLayout llLogContainer; // ✅ RecyclerViewから変更
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Calendar displayDate = Calendar.getInstance();
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;
    private List<LocationEntity> masterLocations = new ArrayList<>();

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
        llLogContainer = findViewById(R.id.llLogContainer); // ✅ 追加

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvVersion.setText("Ver: " + BuildConfig.VERSION_NAME);

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
                if (isToday()) {
                    runOnUiThread(() -> updateListUI()); // ✅ 毎秒UIを更新
                }
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

    private void refreshData() {
        List<LocationEntity> freshData = db.locationDao().getAll();
        masterLocations.clear();
        if (freshData != null && !freshData.isEmpty()) {
            masterLocations.addAll(freshData);
        }
        
        runOnUiThread(() -> {
            tvDebugCount.setText("🚨 データベース内の地点数: " + masterLocations.size() + " 件");
            updateListUI(); // ✅ データ取得後にUIを描画
        });
    }

    // ✅ RecyclerViewを排除し、直接画面を生成・更新する絶対確実なメソッド
    private void updateListUI() {
        // データ件数と画面の箱の数が違えば、箱を作り直す
        if (llLogContainer.getChildCount() != masterLocations.size()) {
            llLogContainer.removeAllViews();
            for (int i = 0; i < masterLocations.size(); i++) {
                View v = LayoutInflater.from(this).inflate(R.layout.item_log, llLogContainer, false);
                llLogContainer.addView(v);
            }
        }

        Calendar endCal = (Calendar) displayDate.clone();
        endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
        long endOfDay = endCal.getTimeInMillis();

        Calendar startCal = (Calendar) displayDate.clone();
        startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0);
        long startOfDay = startCal.getTimeInMillis();

        boolean today = isToday();
        long referenceTime = today ? System.currentTimeMillis() : endOfDay;

        // 生成された箱（View）に1件ずつデータを流し込む
        for (int pos = 0; pos < masterLocations.size(); pos++) {
            LocationEntity loc = masterLocations.get(pos);
            View v = llLogContainer.getChildAt(pos);

            TextView tvName = v.findViewById(R.id.tvLogName);
            TextView tvIn = v.findViewById(R.id.tvLogInTime);
            TextView tvOut = v.findViewById(R.id.tvLogOutTime);
            Button btnUp = v.findViewById(R.id.btnRowUp);
            Button btnDown = v.findViewById(R.id.btnRowDown);
            Button btnDel = v.findViewById(R.id.btnRowDel);

            tvName.setText(loc.name != null ? loc.name : "不明な地点");

            LocationLogEntity latest = null;
            try {
                latest = db.locationDao().getLatestLog(loc.id, endOfDay);
            } catch (Exception e) {}

            if (latest != null && latest.exitTime == 0 && today) {
                tvIn.setText(formatDuration(referenceTime - latest.entryTime));
                tvIn.setBackgroundColor(Color.parseColor("#C8E6C9"));
                tvOut.setText("0:00");
                tvOut.setBackgroundColor(Color.TRANSPARENT);
            } else {
                tvIn.setBackgroundColor(Color.TRANSPARENT);
                tvIn.setText(latest != null ? formatDuration(latest.stayDuration) : "0:00");
                
                if (pos == 0) {
                    tvOut.setText("0:00");
                    tvOut.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    long outMs;
                    if (latest != null) {
                        outMs = referenceTime - (latest.exitTime == 0 ? referenceTime : latest.exitTime);
                    } else {
                        outMs = referenceTime - startOfDay;
                    }
                    tvOut.setText(formatDuration(outMs));
                    tvOut.setBackgroundColor(today ? Color.parseColor("#FFCDD2") : Color.TRANSPARENT);
                }
            }

            int finalPos = pos;
            btnUp.setOnClickListener(btn -> { if (finalPos > 0) swap(finalPos, finalPos - 1); });
            btnDown.setOnClickListener(btn -> { if (finalPos < masterLocations.size() - 1) swap(finalPos, finalPos + 1); });
            btnDel.setOnClickListener(btn -> { 
                db.locationDao().delete(loc); 
                refreshData(); 
                Toast.makeText(MainActivity.this, "削除しました", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void swap(int f, int t) {
        LocationEntity from = masterLocations.get(f), to = masterLocations.get(t);
        int temp = from.displayOrder;
        from.displayOrder = to.displayOrder;
        to.displayOrder = temp;
        db.locationDao().update(from); db.locationDao().update(to);
        refreshData();
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
}
