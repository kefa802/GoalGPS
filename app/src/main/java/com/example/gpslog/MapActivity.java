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
    private Marker currentMarker;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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

        // 1. 保存済みの地点をすべて表示（ただし 0,0 の「海」は無視する）
        List<LocationEntity> allLocations = db.locationDao().getAll();
        for (LocationEntity loc : allLocations) {
            if (loc.latitude == 0.0 && loc.longitude == 0.0) continue; // 海はスキップ
            
            Marker oldMarker = new Marker(mapView);
            oldMarker.setPosition(new GeoPoint(loc.latitude, loc.longitude));
            oldMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            oldMarker.setTitle(loc.name);
            mapView.getOverlays().add(oldMarker);
        }

        // 2. 位置情報の取得と「池袋」強制リセット
        double lat = getIntent().getDoubleExtra("LAT", 35.7295);
        double lon = getIntent().getDoubleExtra("LON", 139.7109);
        
        // 届いたデータが 0.0（未取得）なら、有無を言わさず池袋駅にする
        if (lat == 0.0 && lon == 0.0) {
            lat = 35.7295;
            lon = 139.7109;
        }

        GeoPoint startPoint = new GeoPoint(lat, lon);
        mapView.getController().setZoom(17.0); // ズームもしっかり固定
        mapView.getController().setCenter(startPoint);

        // 新規登録用のマーカー
        currentMarker = new Marker(mapView);
        currentMarker.setPosition(startPoint);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentMarker.setTitle("ここを登録");
        mapView.getOverlays().add(currentMarker);

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
            finish();
        });
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }
}
