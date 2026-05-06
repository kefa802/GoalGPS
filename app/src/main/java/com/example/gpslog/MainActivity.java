package com.example.gpslog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnHistory = findViewById(R.id.btnHistory);

        // スタートボタン：自動記録サービスを開始
        btnStart.setOnClickListener(v -> {
            if (checkPermissions()) {
                Intent serviceIntent = new Intent(this, GpsLoggingService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                tvStatus.setText("自動記録モード：稼働中");
                Toast.makeText(this, "自動打刻を開始しました", Toast.LENGTH_SHORT).show();
            }
        });

        // ストップボタン：サービスを停止
        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, GpsLoggingService.class));
            tvStatus.setText("GoalGPS 待機中");
            Toast.makeText(this, "自動打刻を停止しました", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    private boolean checkPermissions() {
        // Android 14では通知と位置情報の権限が必要です
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            }, 1);
            return false;
        }
        return true;
    }
}
