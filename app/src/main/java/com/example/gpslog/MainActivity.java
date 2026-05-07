package com.example.gpslog;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.appcompat.app.AlertDialog;
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
    private TextView tvStatusBanner, tvDate, tvHeaderIn, tvHeaderOut, tvVersion, tvEmpty;
    private Switch switchRecord;
    private RecyclerView rvLogs;
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Calendar displayDate = Calendar.getInstance();
    private Handler updateHandler = new Handler();
    private Runnable updateRunnable;
    
    private List<LocationEntity> masterLocations = new ArrayList<>();
    private LogAdapter adapter;

    private String currentLocationStr = "取得中...";
    private boolean isCurrentlyMocking = false;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            isCurrentlyMocking = intent.getBooleanExtra("is_mock", false);
            currentLocationStr = String.format(Locale.US, "%.5f, %.5f", lat, lng);
            
            if (switchRecord != null && switchRecord.isChecked()) {
                String mockText = isCurrentlyMocking ? " 【ワープ中】" : "";
                tvStatusBanner.setText("オンライン：自動記録中" + mockText + "\n[現在地] " + currentLocationStr);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvDate = findViewById(R.id.tvDate);
        tvHeaderIn = findViewById(R.id.tvHeaderIn);
        tvHeaderOut = findViewById(R.id.tvHeaderOut);
        tvVersion = findViewById(R.id.tvVersion);
        tvEmpty = findViewById(R.id.tvEmpty);
        switchRecord = findViewById(R.id.switchRecord);
        rvLogs = findViewById(R.id.rvDashboardLogs);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvVersion.setText("Ver: 1.1.7");

        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter();
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

        tvStatusBanner.setOnClickListener(v -> {
            android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE);
            if (prefs.getBoolean("is_mock", false)) {
                prefs.edit().putBoolean("is_mock", false).apply();
                Toast.makeText(this, "ワープ（現在地偽装）を解除しました", Toast.LENGTH_SHORT).show();
            }
        });

        updateRunnable = new Runnable() {
            @Override public void run() {
                if (switchRecord.isChecked() && isToday()) {
                    refreshData();
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
        
        long now = System.currentTimeMillis();

        if (freshData != null && !freshData.isEmpty()) {
            for (LocationEntity loc : freshData) {
                LocationLogEntity latest = db.locationDao().getLatestLog(loc.id, now);
                if (latest == null) {
                    LocationLogEntity dummy = new LocationLogEntity();
                    dummy.locationId = loc.id;
                    dummy.entryTime = now;
                    dummy.exitTime = now;
                    dummy.stayDuration = 0;
                    db.locationDao().insertLog(dummy);
                }
                masterLocations.add(loc);
            }
            tvEmpty.setVisibility(View.GONE);
            rvLogs.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            rvLogs.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private String formatDuration(long ms) {
        if (ms < 0) ms = 0;
        long sec = ms / 1000;
        if (isHourUnit) {
            long min = sec / 60; 
            if (min == 0) return "0.00"; 
            return String.format(Locale.JAPAN, "%.2f", (double) min / 60.0);
        }
        return String.format(Locale.JAPAN, "%d:%02d", sec / 60, sec % 60);
    }

    @Override protected void onResume() { 
        super.onResume(); 
        updateUI(isServiceRunning(GpsLoggingService.class)); 
        refreshData(); 
        updateHandler.post(updateRunnable); 
        
        IntentFilter filter = new IntentFilter("GPS_LOCATION_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(locationReceiver, filter);
        }
    }

    @Override protected void onPause() { 
        super.onPause(); 
        updateHandler.removeCallbacks(updateRunnable); 
        unregisterReceiver(locationReceiver); 
    }

    private void updateUI(boolean run) {
        switchRecord.setChecked(run);
        if (run) {
            String mockText = isCurrentlyMocking ? " 【ワープ中】" : "";
            tvStatusBanner.setText("オンライン：自動記録中" + mockText + "\n[現在地] " + currentLocationStr);
            tvStatusBanner.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            tvStatusBanner.setText("オフライン\n[現在地] 記録停止中");
            tvStatusBanner.setBackgroundColor(Color.parseColor("#9E9E9E"));
        }
    }

    private void startGpsService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            switchRecord.setChecked(false);
            Toast.makeText(this, "位置情報の権限を許可してください", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            switchRecord.setChecked(false);
            Toast.makeText(this, "通知の権限を許可してください", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            return;
        }
        try {
            currentLocationStr = "取得中...";
            startForegroundService(new Intent(this, GpsLoggingService.class));
            updateUI(true);
        } catch (Exception e) {
            switchRecord.setChecked(false);
            Toast.makeText(this, "起動エラー: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopGpsService() { 
        stopService(new Intent(this, GpsLoggingService.class)); 
        updateUI(false); 
        
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
        refreshData(); 
    }

    private boolean isServiceRunning(Class<?> sc) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
            if (sc.getName().equals(s.service.getClassName())) return true;
        }
        return false;
    }

    class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false));
        }

        @Override public void onBindViewHolder(@NonNull LogViewHolder h, int pos) {
            LocationEntity loc = masterLocations.get(pos);
            h.name.setText(loc.name != null ? loc.name : "不明");
            h.location.setText(String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude));

            Calendar startCal = (Calendar) displayDate.clone();
            startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);
            long startOfDay = startCal.getTimeInMillis();

            Calendar endCal = (Calendar) displayDate.clone();
            endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999);
            long endOfDay = endCal.getTimeInMillis();

            boolean isOnline = switchRecord.isChecked();
            boolean today = isToday();
            long referenceTime = today ? System.currentTimeMillis() : endOfDay;

            List<LocationLogEntity> dayLogs = db.locationDao().getLogsForDay(loc.id, startOfDay, endOfDay);
            long totalInMs = 0;
            long totalOutMs = 0;
            boolean isActive = false;

            if (dayLogs != null && !dayLogs.isEmpty()) {
                for (int i = 0; i < dayLogs.size(); i++) {
                    LocationLogEntity log = dayLogs.get(i);
                    if (log.exitTime == 0) {
                        if (today) totalInMs += (referenceTime - log.entryTime);
                        isActive = true;
                    } else {
                        totalInMs += log.stayDuration;
                    }
                    if (i > 0) {
                        LocationLogEntity prevLog = dayLogs.get(i - 1);
                        long outDuration = log.entryTime - prevLog.exitTime;
                        if (outDuration > 0) totalOutMs += outDuration;
                    }
                }
                LocationLogEntity lastLog = dayLogs.get(dayLogs.size() - 1);
                if (lastLog.exitTime != 0 && today) {
                    long finalOut = referenceTime - lastLog.exitTime;
                    if (finalOut > 0) totalOutMs += finalOut;
                }
            }

            h.in.setText(formatDuration(totalInMs));
            h.in.setBackgroundColor((isActive && isOnline && today) ? Color.parseColor("#C8E6C9") : Color.TRANSPARENT);

            h.out.setText(formatDuration(totalOutMs));
            if (isOnline && today) {
                h.out.setBackgroundColor(isActive ? Color.TRANSPARENT : Color.parseColor("#FFCDD2"));
            } else {
                h.out.setBackgroundColor(Color.TRANSPARENT);
            }

            h.itemView.setOnLongClickListener(v -> {
                CharSequence[] items = {"この日の時間をクリア", "上へ移動", "下へ移動", "削除", "📍 ここにワープ(テスト用)"};
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle(loc.name + " の操作")
                    .setItems(items, (dialog, which) -> {
                        switch (which) {
                            case 0: // ✅ 修正済みの特定日クリア機能
                                Calendar sCal = (Calendar) displayDate.clone();
                                sCal.set(Calendar.HOUR_OF_DAY, 0); sCal.set(Calendar.MINUTE, 0); sCal.set(Calendar.SECOND, 0); sCal.set(Calendar.MILLISECOND, 0);
                                long start = sCal.getTimeInMillis();

                                Calendar eCal = (Calendar) displayDate.clone();
                                eCal.set(Calendar.HOUR_OF_DAY, 23); eCal.set(Calendar.MINUTE, 59); eCal.set(Calendar.SECOND, 59); eCal.set(Calendar.MILLISECOND, 999);
                                long end = eCal.getTimeInMillis();

                                db.locationDao().deleteLogsInRange(loc.id, start, end);
                                refreshData();
                                String dateStr = new SimpleDateFormat("MM/dd", Locale.JAPAN).format(displayDate.getTime());
                                Toast.makeText(MainActivity.this, dateStr + " の時間をリセットしました", Toast.LENGTH_SHORT).show();
                                break;
                            case 1: 
                                if (pos > 0) swap(pos, pos - 1);
                                break;
                            case 2: 
                                if (pos < masterLocations.size() - 1) swap(pos, pos + 1);
                                break;
                            case 3: 
                                db.locationDao().deleteLogsByLocationId(loc.id);
                                db.locationDao().delete(loc);
                                refreshData();
                                Toast.makeText(MainActivity.this, "削除しました", Toast.LENGTH_SHORT).show();
                                break;
                            case 4:
                                android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE);
                                prefs.edit()
                                     .putBoolean("is_mock", true)
                                     .putLong("mock_lat", Double.doubleToRawLongBits(loc.latitude))
                                     .putLong("mock_lng", Double.doubleToRawLongBits(loc.longitude))
                                     .apply();
                                Toast.makeText(MainActivity.this, loc.name + " にワープしました！", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    })
                    .show();
                return true;
            });
        }
        
        @Override public int getItemCount() { return masterLocations.size(); }
    }

    private void swap(int f, int t) {
        LocationEntity from = masterLocations.get(f), to = masterLocations.get(t);
        int temp = from.displayOrder;
        from.displayOrder = to.displayOrder;
        to.displayOrder = temp;
        db.locationDao().update(from); db.locationDao().update(to);
        refreshData();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView name, location, in, out;
        LogViewHolder(View v) { 
            super(v); 
            name = v.findViewById(R.id.tvLogName); 
            location = v.findViewById(R.id.tvLogLocation); 
            in = v.findViewById(R.id.tvLogInTime); 
            out = v.findViewById(R.id.tvLogOutTime); 
        }
    }
}
