package com.example.gpslog;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker currentMarker; // これから登録する地点のマーカー
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // osmdroidの設定
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.map);
        if (mapView == null) { finish(); return; }

        mapView.setMultiTouchControls(true);
        EditText etName = findViewById(R.id.etLocationName);
        Button btnSave = findViewById(R.id.btnSaveLocation);

        // データベースの準備
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db")
                .allowMainThreadQueries().build();

        // --- ✅ ここから追加：保存済みの全地点を地図に表示 ---
        List<LocationEntity> allLocations = db.locationDao().getAll();
        for (LocationEntity loc : allLocations) {
            Marker oldMarker = new Marker(mapView);
            oldMarker.setPosition(new GeoPoint(loc.latitude, loc.longitude));
            oldMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            oldMarker.setTitle(loc.name);
            // 保存済みのピンは少し色を変える（デフォルトは赤ですが、区別しやすくします）
            oldMarker.setSubDescription("保存済み");
            mapView.getOverlays().add(oldMarker);
        }
        // --- ここまで ---

        // MainActivityから送られてきた現在地の座標を取得
        double lat = getIntent().getDoubleExtra("LAT", 35.7295); // デフォルト池袋駅
        double lon = getIntent().getDoubleExtra("LON", 139.7109);
        
        GeoPoint startPoint = new GeoPoint(lat, lon);
        mapView.getController().setZoom(17.0);
        mapView.getController().setCenter(startPoint);

        // これから登録する場所のための新しいマーカーを表示
        currentMarker = new Marker(mapView);
        currentMarker.setPosition(startPoint);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle("新規登録地点");
        mapView.getOverlays().add(currentMarker);

        // 長押しで新規登録地点を移動させる
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                currentMarker.setPosition(p);
                mapView.invalidate();
                return true;
            }
        });
        mapView.getOverlays().add(eventsOverlay);

        btnSave.setOnClickListener(v -> {
            LocationEntity entity = new LocationEntity();
            entity.name = etName.getText().toString().isEmpty() ? "無題の地点" : etName.getText().toString();
            entity.latitude = currentMarker.getPosition().getLatitude();
            entity.longitude = currentMarker.getPosition().getLongitude();
            db.locationDao().insert(entity);
            Toast.makeText(this, entity.name + " を保存しました", Toast.LENGTH_SHORT).show();
            finish(); // 登録したら画面を閉じる
        });
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
}
