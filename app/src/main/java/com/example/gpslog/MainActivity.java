package com.example.gpslog;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 先ほど作ったXMLレイアウトを画面にセット
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);
        Button btnRegister = findViewById(R.id.btnRegister);

        // 各ボタンを押したときの処理（今はメッセージが出るだけ）
        btnStart.setOnClickListener(v -> {
            tvStatus.setText("GPS取得中...");
            Toast.makeText(this, "バックグラウンド取得を開始します（予定）", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            tvStatus.setText("GoalGPS 待機中");
            Toast.makeText(this, "取得を停止しました", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            Toast.makeText(this, "ここにマップ画面が開きます", Toast.LENGTH_SHORT).show();
        });
    }
}
