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

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvStatus;
    private double currentLat = 0.0;
    private double currentLon = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvVersion = findViewById(R.id.tvVersion);

        // システムからバージョン名を取得して右上にセット
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getLocation();
        }

        btnStart.setOnClickListener(v -> {
            tvStatus.setText("GPS取得中...");
            Toast.makeText(this, "バックグラウンド取得を開始します（予定）", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            tvStatus.setText("GoalGPS 待機中");
            Toast.makeText(this, "取得を停止しました", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            if (currentLat != 0.0) {
                intent.putExtra("LAT", currentLat);
                intent.putExtra("LON", currentLon);
            }
            startActivity(intent);
        });
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLon = location.getLongitude();
                    tvStatus.setText(String.format("緯度: %f\n経度: %f", currentLat, currentLon));
                } else {
                    tvStatus.setText("位置情報が取得できません");
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
}
