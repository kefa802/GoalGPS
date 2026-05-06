package com.example.gpslog;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setTextSize(24);
        tv.setPadding(50, 50, 50, 50);
        tv.setText("工藤さん、ビルド成功おめでとうございます！\n\nこれが「当五郎」アプリの記念すべき第一歩です！");
        setContentView(tv);
    }
}
