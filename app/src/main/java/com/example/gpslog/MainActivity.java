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
        @Override public void onReceive(Context context, Intent intent) {
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

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatusBanner = findViewById(R.id.tvStatusBanner); tvDate = findViewById(R.id.tvDate); tvHeaderIn = findViewById(R.id.tvHeaderIn); tvHeaderOut = findViewById(R.id.tvHeaderOut); tvVersion = findViewById(R.id.tvVersion); tvEmpty = findViewById(R.id.tvEmpty); switchRecord = findViewById(R.id.switchRecord); rvLogs = findViewById(R.id.rvDashboardLogs);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();
        tvVersion.setText("Ver: 1.2.0");
        rvLogs.setLayoutManager(new LinearLayoutManager(this)); adapter = new LogAdapter(); rvLogs.setAdapter(adapter);
        findViewById(R.id.btnPrevDay).setOnClickListener(v -> changeDate(-1)); findViewById(R.id.btnNextDay).setOnClickListener(v -> changeDate(1));
        ((RadioGroup)findViewById(R.id.rgUnit)).setOnCheckedChangeListener((g, id) -> {
            isHourUnit = (id == R.id.rbHour); String u = isHourUnit ? "(時)" : "(分)"; tvHeaderIn.setText("IN" + u); tvHeaderOut.setText("OUT" + u); refreshData();
        });
        switchRecord.setOnClickListener(v -> { if (switchRecord.isChecked()) { startGpsService(); } else { stopGpsService(); } });
        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        tvStatusBanner.setOnClickListener(v -> {
            android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE);
            if (prefs.getBoolean("is_mock", false)) { prefs.edit().putBoolean("is_mock", false).apply(); Toast.makeText(this, "ワープ解除しました", Toast.LENGTH_SHORT).show(); }
        });
        updateRunnable = new Runnable() { @Override public void run() { if (switchRecord.isChecked() && isToday()) refreshData(); updateHandler.postDelayed(this, 1000); } };
        updateDateDisplay();
    }

    private void changeDate(int amount) { displayDate.add(Calendar.DATE, amount); updateDateDisplay(); }
    private void updateDateDisplay() { tvDate.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(displayDate.getTime())); refreshData(); }
    private boolean isToday() { Calendar today = Calendar.getInstance(); return today.get(Calendar.YEAR) == displayDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == displayDate.get(Calendar.DAY_OF_YEAR); }
    private void refreshData() {
        List<LocationEntity> freshData = db.locationDao().getAll(); masterLocations.clear(); long now = System.currentTimeMillis();
        if (freshData != null && !freshData.isEmpty()) {
            for (LocationEntity loc : freshData) {
                if (db.locationDao().getLatestLog(loc.id, now) == null) { LocationLogEntity dummy = new LocationLogEntity(); dummy.locationId = loc.id; dummy.entryTime = now; dummy.exitTime = now; dummy.stayDuration = 0; db.locationDao().insertLog(dummy); }
                masterLocations.add(loc);
            }
            tvEmpty.setVisibility(View.GONE); rvLogs.setVisibility(View.VISIBLE);
        } else { tvEmpty.setVisibility(View.VISIBLE); rvLogs.setVisibility(View.GONE); }
        adapter.notifyDataSetChanged();
    }

    private String formatDuration(long ms) { 
        if (ms < 0) ms = 0;
        long sec = ms / 1000; 
        if (isHourUnit) { 
            long min = sec / 60; 
            return min == 0 ? "0.00" : String.format(Locale.JAPAN, "%.2f", (double) min / 60.0); 
        } 
        return String.format(Locale.JAPAN, "%d:%02d", sec / 60, sec % 60); 
    }
    
    @Override protected void onResume() { super.onResume(); updateUI(isServiceRunning(GpsLoggingService.class)); refreshData(); updateHandler.post(updateRunnable); IntentFilter filter = new IntentFilter("GPS_LOCATION_UPDATE"); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED); } else { registerReceiver(locationReceiver, filter); } }
    @Override protected void onPause() { super.onPause(); updateHandler.removeCallbacks(updateRunnable); unregisterReceiver(locationReceiver); }
    private void updateUI(boolean run) { switchRecord.setChecked(run); if (run) { String mockText = isCurrentlyMocking ? " 【ワープ中】" : ""; tvStatusBanner.setText("オンライン：自動記録中" + mockText + "\n[現在地] " + currentLocationStr); tvStatusBanner.setBackgroundColor(Color.parseColor("#4CAF50")); } else { tvStatusBanner.setText("オフライン\n[現在地] 記録停止中"); tvStatusBanner.setBackgroundColor(Color.parseColor("#9E9E9E")); } }
    private void startGpsService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { switchRecord.setChecked(false); ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1); return; }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) { switchRecord.setChecked(false); ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2); return; }
        try { currentLocationStr = "取得中..."; startForegroundService(new Intent(this, GpsLoggingService.class)); updateUI(true); } catch (Exception e) { switchRecord.setChecked(false); }
    }
    private void stopGpsService() { stopService(new Intent(this, GpsLoggingService.class)); updateUI(false); long now = System.currentTimeMillis(); List<LocationEntity> locs = db.locationDao().getAll(); if (locs != null) { for (LocationEntity loc : locs) { LocationLogEntity activeLog = db.locationDao().getActiveLog(loc.id); if (activeLog != null) { activeLog.exitTime = now; activeLog.stayDuration = now - activeLog.entryTime; db.locationDao().updateLog(activeLog); } } } refreshData(); }
    private boolean isServiceRunning(Class<?> sc) { ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE); for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) { if (sc.getName().equals(s.service.getClassName())) return true; } return false; }

    class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) { return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false)); }
        @Override public void onBindViewHolder(@NonNull LogViewHolder h, int pos) {
            LocationEntity loc = masterLocations.get(pos); h.name.setText(loc.name); h.location.setText(String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude));
            Calendar cal = (Calendar) displayDate.clone(); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0); long start = cal.getTimeInMillis();
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999); long end = cal.getTimeInMillis();
            boolean isOnline = switchRecord.isChecked(), today = isToday(); long now = today ? System.currentTimeMillis() : end;
            List<LocationLogEntity> logs = db.locationDao().getLogsForDay(loc.id, start, end); long totalIn = 0, totalOut = 0; boolean isActive = false;
            if (logs != null && !logs.isEmpty()) {
                for (int i = 0; i < logs.size(); i++) {
                    LocationLogEntity log = logs.get(i);
                    if (log.exitTime == 0) { if (today) totalIn += (now - log.entryTime); isActive = true; } else { totalIn += log.stayDuration; }
                    if (i > 0) { long d = log.entryTime - logs.get(i-1).exitTime; if (d > 0) totalOut += d; }
                }
                LocationLogEntity last = logs.get(logs.size() - 1);
                if (last.exitTime != 0 && today) { long d = now - last.exitTime; if (d > 0) totalOut += d; }
            }
            h.in.setText(formatDuration(totalIn)); h.in.setBackgroundColor((isActive && isOnline && today) ? Color.parseColor("#C8E6C9") : Color.TRANSPARENT);
            h.out.setText(formatDuration(totalOut)); h.out.setBackgroundColor((isOnline && today && !isActive) ? Color.parseColor("#FFCDD2") : Color.TRANSPARENT);
            h.itemView.setOnLongClickListener(v -> {
                CharSequence[] items = {"この日の時間をクリア", "上へ移動", "下へ移動", "削除", "📍 ここにワープ"};
                new AlertDialog.Builder(MainActivity.this).setTitle(loc.name + " の操作").setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: db.locationDao().deleteLogsInRange(loc.id, start, end); refreshData(); Toast.makeText(MainActivity.this, "クリアしました", Toast.LENGTH_SHORT).show(); break;
                        case 1: if (pos > 0) swap(pos, pos - 1); break;
                        case 2: if (pos < masterLocations.size() - 1) swap(pos, pos + 1); break;
                        case 3: db.locationDao().deleteLogsByLocationId(loc.id); db.locationDao().delete(loc); refreshData(); break;
                        case 4: android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE); prefs.edit().putBoolean("is_mock", true).putLong("mock_lat", Double.doubleToRawLongBits(loc.latitude)).putLong("mock_lng", Double.doubleToRawLongBits(loc.longitude)).apply(); Toast.makeText(MainActivity.this, "ワープしました", Toast.LENGTH_SHORT).show(); break;
                    }
                }).show(); return true;
            });
        }
        @Override public int getItemCount() { return masterLocations.size(); }
    }
    private void swap(int f, int t) { LocationEntity from = masterLocations.get(f), to = masterLocations.get(t); int temp = from.displayOrder; from.displayOrder = to.displayOrder; to.displayOrder = temp; db.locationDao().update(from); db.locationDao().update(to); refreshData(); }
    static class LogViewHolder extends RecyclerView.ViewHolder { TextView name, location, in, out; LogViewHolder(View v) { super(v); name = v.findViewById(R.id.tvLogName); location = v.findViewById(R.id.tvLogLocation); in = v.findViewById(R.id.tvLogInTime); out = v.findViewById(R.id.tvLogOutTime); } }
}
