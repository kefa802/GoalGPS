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

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker currentMarker;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 📜 osmdroidの設定
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());

        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.map);
        if (mapView == null) { finish(); return; }

        mapView.setMultiTouchControls(true);
        EditText etName = findViewById(R.id.etLocationName);
        Button btnSave = findViewById(R.id.btnSaveLocation);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db")
                .allowMainThreadQueries().build();

        // 渡された座標を取得
        double lat = getIntent().getDoubleExtra("LAT", 0.0);
        double lon = getIntent().getDoubleExtra("LON", 0.0);
        
        // ✅ GPSが取得できていない場合のデフォルトを「池袋駅」に設定
        if (lat == 0.0) lat = 35.7295; 
        if (lon == 0.0) lon = 139.7109;

        GeoPoint startPoint = new GeoPoint(lat, lon);
        mapView.getController().setZoom(17.0);
        mapView.getController().setCenter(startPoint);

        // 現在地にマーカーを表示
        currentMarker = new Marker(mapView);
        currentMarker.setPosition(startPoint);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle("現在地");
        mapView.getOverlays().add(currentMarker);

        // 長押しでマーカー地点を変更
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                currentMarker.setPosition(p);
                currentMarker.setTitle("登録地点");
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
            Toast.makeText(this, entity.name + " を登録しました", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
}
