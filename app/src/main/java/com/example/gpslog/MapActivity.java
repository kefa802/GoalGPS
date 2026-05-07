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
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatusBanner, tvDate;
    private Switch switchRecord;
    private RecyclerView rvLogs;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI部品の紐付け
        tvStatusBanner = findViewById(R.id.tvStatusBanner);
        tvDate = findViewById(R.id.tvDate);
        switchRecord = findViewById(R.id.switchRecord);
        rvLogs = findViewById(R.id.rvDashboardLogs);
        Button btnRegister = findViewById(R.id.btnRegister);

        // データベース準備
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db")
                .allowMainThreadQueries().build();

        // 日付をセット（スケッチ通り当日を表示）
        String today = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(new Date());
        tvDate.setText(today);

        // サービスが動いているかチェックしてUIを初期化
        updateUI(isServiceRunning(GpsLoggingService.class));

        // 自動記録スイッチの切り替え処理
        switchRecord.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (checkPermissions()) {
                    startGpsService();
                } else {
                    switchRecord.setChecked(false);
                }
            } else {
                stopGpsService();
            }
        });

        // 地点登録ボタン
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

        // ログリストの表示設定
        setupRecyclerView();
    }

    private void updateUI(boolean isRunning) {
        switchRecord.setChecked(isRunning);
        if (isRunning) {
            tvStatusBanner.setText("オンライン：自動記録中");
            tvStatusBanner.setBackgroundColor(Color.parseColor("#4CAF50")); // 緑色
        } else {
            tvStatusBanner.setText("オフライン");
            tvStatusBanner.setBackgroundColor(Color.parseColor("#9E9E9E")); // グレー
        }
    }

    private void startGpsService() {
        Intent intent = new Intent(this, GpsLoggingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        updateUI(true);
    }

    private void stopGpsService() {
        stopService(new Intent(this, GpsLoggingService.class));
        updateUI(false);
    }

    private void setupRecyclerView() {
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        List<LocationLogEntity> logs = db.locationDao().getAllLogs();
        
        rvLogs.setAdapter(new RecyclerView.Adapter<LogViewHolder>() {
            @NonNull
            @Override
            public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
                return new LogViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
                LocationLogEntity log = logs.get(position);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPAN);
                
                holder.tvName.setText(log.locationName);
                holder.tvIn.setText(sdf.format(new Date(log.entryTime)));
                holder.tvOut.setText(log.exitTime == 0 ? "--:--" : sdf.format(new Date(log.exitTime)));
            }

            @Override
            public int getItemCount() { return logs.size(); }
        });
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIn, tvOut;
        LogViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvLogName);
            tvIn = v.findViewById(R.id.tvLogInTime);
            tvOut = v.findViewById(R.id.tvLogOutTime);
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS}, 1);
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupRecyclerView(); // 画面に戻るたびにリストを更新
    }
}
