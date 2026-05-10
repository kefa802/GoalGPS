package com.example.gpslog;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private TextView tvStatusBanner, tvDate, tvVersion, tvEmpty;
    private Switch switchRecord;
    private RecyclerView rvLogs, rvHistoryLogs;
    private AppDatabase db;
    private boolean isHourUnit = false;
    private Calendar displayDate = Calendar.getInstance();
    private List<LocationEntity> masterLocations = new ArrayList<>();
    private LogAdapter adapter;
    private List<VisitHistory> historyLogs = new ArrayList<>();
    private HistoryAdapter historyAdapter;
    private String currentLocationStr = "未取得";
    private double currentLat = 0.0, currentLng = 0.0;
    private boolean isCurrentlyMocking = false;

    private Handler updateHandler = new Handler();
    private Runnable updateRunnable = new Runnable() { @Override public void run() { refreshData(); updateHandler.postDelayed(this, 1000); } };

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            currentLat = intent.getDoubleExtra("lat", 0); currentLng = intent.getDoubleExtra("lng", 0);
            isCurrentlyMocking = intent.getBooleanExtra("is_mock", false); 
            currentLocationStr = String.format(Locale.US, "%.5f, %.5f", currentLat, currentLng);
            updateUI(isServiceRunning(GpsLoggingService.class));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String errorLog = throwable.toString() + "\n" + Log.getStackTraceString(throwable);
            getSharedPreferences("crash_log", Context.MODE_PRIVATE).edit().putString("error", errorLog).commit();
            System.exit(1);
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.content.SharedPreferences prefs = getSharedPreferences("crash_log", Context.MODE_PRIVATE);
        String savedError = prefs.getString("error", null);
        if (savedError != null) {
            new AlertDialog.Builder(this).setTitle("🚨 強制終了の原因を発見").setMessage("スクショ用ログ\n\n" + savedError).setPositiveButton("確認", null).show();
            prefs.edit().remove("error").apply();
        }

        tvStatusBanner = findViewById(R.id.tvStatusBanner); tvDate = findViewById(R.id.tvDate); tvVersion = findViewById(R.id.tvVersion); tvEmpty = findViewById(R.id.tvEmpty); switchRecord = findViewById(R.id.switchRecord); rvLogs = findViewById(R.id.rvDashboardLogs); rvHistoryLogs = findViewById(R.id.rvHistoryLogs);
        tvVersion.setText("Ver: 1.4.0");

        try { db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").fallbackToDestructiveMigration().allowMainThreadQueries().build(); } catch (Exception e) {}

        rvLogs.setLayoutManager(new LinearLayoutManager(this)); adapter = new LogAdapter(); rvLogs.setAdapter(adapter);
        rvHistoryLogs.setLayoutManager(new LinearLayoutManager(this)); historyAdapter = new HistoryAdapter(); rvHistoryLogs.setAdapter(historyAdapter);

        findViewById(R.id.btnPrevDay).setOnClickListener(v -> changeDate(-1)); findViewById(R.id.btnNextDay).setOnClickListener(v -> changeDate(1));
        ((RadioGroup)findViewById(R.id.rgUnit)).setOnCheckedChangeListener((g, id) -> { isHourUnit = (id == R.id.rbHour); refreshData(); });
        
        switchRecord.setOnClickListener(v -> { 
            if (switchRecord.isChecked()) {
                startGpsService(); 
            } else {
                stopGpsService(); 
                // ✅ 手動でOFFにした時、現在INの場所すべてにOUTを記録して時間を止める
                long now = System.currentTimeMillis();
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(now));
                for (LocationEntity loc : db.locationDao().getAll()) {
                    VisitHistory lastH = db.locationDao().getLastHistory(loc.id);
                    if (lastH != null && lastH.isEntry) {
                        VisitHistory vh = new VisitHistory(); vh.locationId = loc.id; vh.date = today; vh.timestamp = now; vh.isEntry = false;
                        db.locationDao().insertHistory(vh);
                    }
                }
                refreshData();
            }
        });
        
        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.btnClearHistory).setOnClickListener(v -> { new AlertDialog.Builder(this).setTitle("履歴クリア").setMessage("削除しますか？").setPositiveButton("削除", (d, w) -> { db.locationDao().deleteAllHistoryForDate(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(displayDate.getTime())); refreshData(); }).setNegativeButton("キャンセル", null).show(); });

        updateDateDisplay();
        updateHandler.postDelayed(updateRunnable, 1000);
    }

    private void updateDateDisplay() { tvDate.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(displayDate.getTime())); refreshData(); }
    private void changeDate(int amount) { displayDate.add(Calendar.DATE, amount); updateDateDisplay(); }

    private void refreshData() {
        if (db == null) return;
        try {
            String ds = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(displayDate.getTime());
            List<LocationEntity> fd = db.locationDao().getAll(); masterLocations.clear();
            if (fd != null && !fd.isEmpty()) { masterLocations.addAll(fd); tvEmpty.setVisibility(View.GONE); rvLogs.setVisibility(View.VISIBLE); } else { tvEmpty.setVisibility(View.VISIBLE); rvLogs.setVisibility(View.GONE); }
            adapter.notifyDataSetChanged();
            List<VisitHistory> hl = db.locationDao().getHistoryForDate(ds); historyLogs.clear(); if (hl != null) historyLogs.addAll(hl);
            historyAdapter.notifyDataSetChanged();
        } catch (Exception e) {}
    }

    private void updateUI(boolean run) {
        switchRecord.setChecked(run);
        if (run) { tvStatusBanner.setText("オンライン：自動記録中" + (isCurrentlyMocking ? " 【ワープ中】" : "") + "\n[現在地] " + currentLocationStr); tvStatusBanner.setBackgroundColor(Color.parseColor("#4CAF50")); } 
        else { tvStatusBanner.setText("オフライン\n[現在地] 記録停止中"); tvStatusBanner.setBackgroundColor(Color.parseColor("#9E9E9E")); }
    }

    private void startGpsService() {
        List<String> perms = new ArrayList<>(); perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        for (String p : perms) { if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), 1); switchRecord.setChecked(false); return; } }
        startForegroundService(new Intent(this, GpsLoggingService.class)); updateUI(true);
    }
    
    private void stopGpsService() { stopService(new Intent(this, GpsLoggingService.class)); updateUI(false); }
    private boolean isServiceRunning(Class<?> sc) { android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE); for (android.app.ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) { if (sc.getName().equals(s.service.getClassName())) return true; } return false; }
    @Override protected void onResume() { super.onResume(); updateUI(isServiceRunning(GpsLoggingService.class)); registerReceiver(locationReceiver, new IntentFilter("GPS_LOCATION_UPDATE"), (Build.VERSION.SDK_INT >= 33 ? Context.RECEIVER_EXPORTED : 0)); }
    @Override protected void onPause() { super.onPause(); unregisterReceiver(locationReceiver); }

    class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) { return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false)); }
        @Override public void onBindViewHolder(@NonNull LogViewHolder h, int pos) {
            LocationEntity loc = masterLocations.get(pos); h.name.setText(loc.name); h.location.setText(String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude));
            
            boolean isOnline = switchRecord.isChecked();
            Calendar today = Calendar.getInstance();
            boolean isT = (today.get(Calendar.YEAR) == displayDate.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == displayDate.get(Calendar.DAY_OF_YEAR));
            long now = System.currentTimeMillis();

            // ✅ DB計算エンジンから直接時間を取得
            long[] times = AppDatabase.calcTimes(db.locationDao(), loc.id, displayDate, isT, now);
            long in = times[0];
            long out = times[1];

            boolean isCurrentlyIn = false;
            if (isOnline && isT && currentLat != 0.0) {
                float[] dist = new float[1]; android.location.Location.distanceBetween(currentLat, currentLng, loc.latitude, loc.longitude, dist);
                if (dist[0] <= 20.0f) isCurrentlyIn = true;
            }

            if (isHourUnit) { h.in.setText(String.format(Locale.JAPAN, "%.2f", (double)(in/60000)/60.0)); h.out.setText(String.format(Locale.JAPAN, "%.2f", (double)(out/60000)/60.0)); }
            else { h.in.setText(String.format(Locale.JAPAN, "%d:%02d", (in/1000)/60, (in/1000)%60)); h.out.setText(String.format(Locale.JAPAN, "%d:%02d", (out/1000)/60, (out/1000)%60)); }
            
            h.in.setBackgroundColor((isOnline && isT && isCurrentlyIn) ? Color.parseColor("#C8E6C9") : Color.TRANSPARENT);
            h.out.setBackgroundColor((isOnline && isT && !isCurrentlyIn) ? Color.parseColor("#FFCDD2") : Color.TRANSPARENT);
            
            h.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this).setTitle(loc.name).setItems(new String[]{"上へ移動", "下へ移動", "削除", "📍 ここにワープ"}, (dialog, which) -> {
                    if (which == 0) { if (pos > 0) swap(pos, pos - 1); }
                    else if (which == 1) { if (pos < masterLocations.size() - 1) swap(pos, pos + 1); }
                    else if (which == 2) { db.locationDao().deleteLogsByLocationId(loc.id); db.locationDao().deleteAllDailyForLocation(loc.id); db.locationDao().deleteAllHistoryForLocation(loc.id); db.locationDao().delete(loc); refreshData(); }
                    else { getSharedPreferences("gps_mock", 0).edit().putBoolean("is_mock", true).putLong("mock_lat", Double.doubleToRawLongBits(loc.latitude)).putLong("mock_lng", Double.doubleToRawLongBits(loc.longitude)).apply(); }
                }).show(); return true;
            });
        }
        @Override public int getItemCount() { return masterLocations.size(); }
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
        @NonNull @Override public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) { return new HistoryViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_history, p, false)); }
        @Override public void onBindViewHolder(@NonNull HistoryViewHolder h, int pos) {
            VisitHistory history = historyLogs.get(pos); String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(new Date(history.timestamp));
            h.time.setText(timeStr); h.action.setText(history.isEntry ? "IN" : "OUT"); h.action.setTextColor(history.isEntry ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
            LocationEntity loc = db.locationDao().getLocationById(history.locationId); String locName = loc != null ? loc.name : "不明"; h.name.setText(locName);
            h.itemView.setOnLongClickListener(v -> { new AlertDialog.Builder(MainActivity.this).setTitle("履歴削除").setMessage("削除しますか？").setPositiveButton("削除", (d, w) -> { db.locationDao().deleteHistoryById(history.id); refreshData(); }).setNegativeButton("キャンセル", null).show(); return true; });
        }
        @Override public int getItemCount() { return historyLogs.size(); }
    }
    static class HistoryViewHolder extends RecyclerView.ViewHolder { TextView time, action, name; HistoryViewHolder(View v) { super(v); time = v.findViewById(R.id.tvHistoryTime); action = v.findViewById(R.id.tvHistoryAction); name = v.findViewById(R.id.tvHistoryName); } }
    private void swap(int f, int t) { LocationEntity from = masterLocations.get(f), to = masterLocations.get(t); int temp = from.displayOrder; from.displayOrder = to.displayOrder; to.displayOrder = temp; db.locationDao().update(from); db.locationDao().update(to); refreshData(); }
    static class LogViewHolder extends RecyclerView.ViewHolder { TextView name, location, in, out; LogViewHolder(View v) { super(v); name = v.findViewById(R.id.tvLogName); location = v.findViewById(R.id.tvLogLocation); in = v.findViewById(R.id.tvLogInTime); out = v.findViewById(R.id.tvLogOutTime); } }
}
