package com.example.gpslog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import android.app.AlertDialog;

public class MainActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvStatus;
    private double currentLat = 0.0;
    private double currentLon = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setupCrashHandler();

        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tvStatus);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnHistory = findViewById(R.id.btnHistory); // ✅ 追加
        TextView tvVersion = findViewById(R.id.tvVersion);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnStart.setOnClickListener(v -> {
            tvStatus.setText("GPS取得をリクエスト中...");
            checkPermissionAndGetLocation();
        });

        btnStop.setOnClickListener(v -> {
            tvStatus.setText("GoalGPS 待機中");
            Toast.makeText(this, "停止しました", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("LAT", currentLat);
            intent.putExtra("LON", currentLon);
            startActivity(intent);
        });

        // ✅ 履歴ボタンの処理を追加
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void checkPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLon = location.getLongitude();
                    tvStatus.setText(String.format("緯度: %f\n経度: %f", currentLat, currentLon));
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }
    }

    private void setupCrashHandler() {
        try {
            final File crashFile = new File(getExternalFilesDir(null), "crash_log.txt");
            if (crashFile.exists()) {
                StringBuilder crashLog = new StringBuilder();
                BufferedReader br = new BufferedReader(new FileReader(crashFile));
                String line;
                while ((line = br.readLine()) != null) crashLog.append(line).append("\n");
                br.close();
                new AlertDialog.Builder(this).setTitle("🚨 クラッシュレポート").setMessage(crashLog.toString())
                    .setPositiveButton("消去", (d, w) -> crashFile.delete()).show();
            }
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                try {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    FileWriter writer = new FileWriter(crashFile, false);
                    writer.write("発生日時: " + new Date() + "\n\n" + sw.toString());
                    writer.close();
                } catch (Exception e) {}
                System.exit(1);
            });
        } catch (Exception e) {}
    }
}
