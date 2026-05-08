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
    private List<LocationEntity> masterLocations = new ArrayList<>();
    private LogAdapter adapter;
    private String currentLocationStr = "取得中...";
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private boolean isCurrentlyMocking = false;

    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            currentLat = intent.getDoubleExtra("lat", 0);
            currentLng = intent.getDoubleExtra("lng", 0);
            isCurrentlyMocking = intent.getBooleanExtra("is_mock", false); // ✅ 復活
            currentLocationStr = String.format(Locale.US, "%.5f, %.5f", currentLat, currentLng);
            updateUI(isServiceRunning(GpsLoggingService.class));
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatusBanner = findViewById(R.id.tvStatusBanner); tvDate = findViewById(R.id.tvDate); tvHeaderIn = findViewById(R.id.tvHeaderIn); tvHeaderOut = findViewById(R.id.tvHeaderOut); tvVersion = findViewById(R.id.tvVersion); tvEmpty = findViewById(R.id.tvEmpty); switchRecord = findViewById(R.id.switchRecord); rvLogs = findViewById(R.id.rvDashboardLogs);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        tvVersion.setText("Ver: 1.2.8"); // ✅ バージョン更新
        rvLogs.setLayoutManager(new LinearLayoutManager(this)); adapter = new LogAdapter(); rvLogs.setAdapter(adapter);
        findViewById(R.id.btnPrevDay).setOnClickListener(v -> changeDate(-1)); findViewById(R.id.btnNextDay).setOnClickListener(v -> changeDate(1));
        ((RadioGroup)findViewById(R.id.rgUnit)).setOnCheckedChangeListener((g, id) -> { isHourUnit = (id == R.id.rbHour); refreshData(); });
        switchRecord.setOnClickListener(v -> { if (switchRecord.isChecked()) { startGpsService(); } else { stopGpsService(); } });
        findViewById(R.id.btnRegister).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        
        // ワープ解除のタップイベント
        tvStatusBanner.setOnClickListener(v -> {
            android.content.SharedPreferences prefs = getSharedPreferences("gps_mock", Context.MODE_PRIVATE);
            if (prefs.getBoolean("is_mock", false)) {
                prefs.edit().putBoolean("is_mock", false).apply();
                Toast.makeText(this, "ワープ解除しました", Toast.LENGTH_SHORT).show();
            }
        });

        refreshData();
        new Handler().postDelayed(new Runnable() { @Override public void run() { refreshData(); new Handler().postDelayed(this, 1000); } }, 1000);
    }

    private void changeDate(int amount) { displayDate.add(Calendar.DATE, amount); tvDate.setText(new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(displayDate.getTime())); refreshData(); }
    private void refreshData() {
        List<LocationEntity> freshData = db.locationDao().getAll(); masterLocations.clear();
        if (freshData != null && !freshData.isEmpty()) { masterLocations.addAll(freshData); tvEmpty.setVisibility(View.GONE); rvLogs.setVisibility(View.VISIBLE); } else { tvEmpty.setVisibility(View.VISIBLE); rvLogs.setVisibility(View.GONE); }
        adapter.notifyDataSetChanged();
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { switchRecord.setChecked(false); ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1); return; }
        startForegroundService(new Intent(this, GpsLoggingService.class)); updateUI(true);
    }
    private void stopGpsService() { stopService(new Intent(this, GpsLoggingService.class)); updateUI(false); }
    private boolean isServiceRunning(Class<?> sc) { ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE); for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) { if (sc.getName().equals(s.service.getClassName())) return true; } return false; }
    
    @Override protected void onResume() { 
        super.onResume(); 
        updateUI(isServiceRunning(GpsLoggingService.class)); 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, new IntentFilter("GPS_LOCATION_UPDATE"), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(locationReceiver, new IntentFilter("GPS_LOCATION_UPDATE"));
        }
    }
    @Override protected void onPause() { super.onPause(); unregisterReceiver(locationReceiver); }

    class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        @NonNull @Override public LogViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) { return new LogViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_log, p, false)); }
        @Override public void onBindViewHolder(@NonNull LogViewHolder h, int pos) {
            LocationEntity loc = masterLocations.get(pos); h.name.setText(loc.name); h.location.setText(String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude));
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(displayDate.getTime());
            DailyAccumulator d = db.locationDao().getDaily(loc.id, date);
            long in = (d != null) ? d.totalInMs : 0, out = (d != null) ? d.totalOutMs : 0;
            
            if (isHourUnit) {
                h.in.setText(String.format(Locale.JAPAN, "%.2f", (double)(in/60000)/60.0));
                h.out.setText(String.format(Locale.JAPAN, "%.2f", (double)(out/60000)/60.0));
            } else {
                h.in.setText(String.format(Locale.JAPAN, "%d:%02d", (in/1000)/60, (in/1000)%60));
                h.out.setText(String.format(Locale.JAPAN, "%d:%02d", (out/1000)/60, (out/1000)%60));
            }

            boolean isOnline = switchRecord.isChecked();
            Calendar todayCal = Calendar.getInstance();
            boolean isTodayDate = (todayCal.get(Calendar.YEAR) == displayDate.get(Calendar.YEAR) && 
                                   todayCal.get(Calendar.DAY_OF_YEAR) == displayDate.get(Calendar.DAY_OF_YEAR));
            boolean isCurrentlyIn = false;

            if (isOnline && isTodayDate && currentLat != 0.0 && currentLng != 0.0) {
                float[] dist = new float[1];
                android.location.Location.distanceBetween(currentLat, currentLng, loc.latitude, loc.longitude, dist);
                if (dist[0] <= 20.0f) {
                    isCurrentlyIn = true;
                }
            }

            if (isOnline && isTodayDate) {
                h.in.setBackgroundColor(isCurrentlyIn ? Color.parseColor("#C8E6C9") : Color.TRANSPARENT);
                h.out.setBackgroundColor(!isCurrentlyIn ? Color.parseColor("#FFCDD2") : Color.TRANSPARENT);
            } else {
                h.in.setBackgroundColor(Color.TRANSPARENT);
                h.out.setBackgroundColor(Color.TRANSPARENT);
            }
            
            h.itemView.setOnLongClickListener(v -> {
                // ✅ 復活：すべてのメニュー項目
                CharSequence[] items = {"この日をクリア", "上へ移動", "下へ移動", "削除", "📍 ここにワープ"};
                new AlertDialog.Builder(MainActivity.this).setTitle(loc.name).setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: 
                            db.locationDao().deleteDaily(loc.id, date); 
                            refreshData(); 
                            break;
                        case 1: 
                            if (pos > 0) swap(pos, pos - 1); 
                            break;
                        case 2: 
                            if (pos < masterLocations.size() - 1) swap(pos, pos + 1); 
                            break;
                        case 3: 
                            db.locationDao().deleteLogsByLocationId(loc.id); 
                            db.locationDao().deleteAllDailyForLocation(loc.id);
                            db.locationDao().delete(loc); 
                            refreshData(); 
                            break;
                        case 4: 
                            getSharedPreferences("gps_mock", Context.MODE_PRIVATE).edit()
                                .putBoolean("is_mock", true)
                                .putLong("mock_lat", Double.doubleToRawLongBits(loc.latitude))
                                .putLong("mock_lng", Double.doubleToRawLongBits(loc.longitude))
                                .apply(); 
                            Toast.makeText(MainActivity.this, "ワープしました！\n（上の緑バナーをタップで解除）", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }).show(); return true;
            });
        }
        @Override public int getItemCount() { return masterLocations.size(); }
    }
    
    // ✅ 復活：入れ替え処理メソッド
    private void swap(int f, int t) { 
        LocationEntity from = masterLocations.get(f), to = masterLocations.get(t); 
        int temp = from.displayOrder; 
        from.displayOrder = to.displayOrder; 
        to.displayOrder = temp; 
        db.locationDao().update(from); 
        db.locationDao().update(to); 
        refreshData(); 
    }
    
    static class LogViewHolder extends RecyclerView.ViewHolder { TextView name, location, in, out; LogViewHolder(View v) { super(v); name = v.findViewById(R.id.tvLogName); location = v.findViewById(R.id.tvLogLocation); in = v.findViewById(R.id.tvLogInTime); out = v.findViewById(R.id.tvLogOutTime); } }
}
