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
        setContentView(R.layout.activity_map);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView = findViewById(R.id.map);
        mapView.setMultiTouchControls(true);
        EditText etName = findViewById(R.id.etLocationName);
        Button btnSave = findViewById(R.id.btnSaveLocation);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();

        double lat = getIntent().getDoubleExtra("LAT", 35.6812);
        double lon = getIntent().getDoubleExtra("LON", 139.7671);
        GeoPoint startPoint = new GeoPoint(lat, lon);
        mapView.getController().setZoom(18.0);
        mapView.getController().setCenter(startPoint);

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (currentMarker != null) mapView.getOverlays().remove(currentMarker);
                currentMarker = new Marker(mapView);
                currentMarker.setPosition(p);
                currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(currentMarker);
                mapView.invalidate();
                return true;
            }
        };
        mapView.getOverlays().add(new MapEventsOverlay(mReceive));

        btnSave.setOnClickListener(v -> {
            if (currentMarker == null) {
                Toast.makeText(this, "地図を長押しして場所を選んでください", Toast.LENGTH_SHORT).show();
                return;
            }
            LocationEntity entity = new LocationEntity();
            entity.name = etName.getText().toString();
            entity.latitude = currentMarker.getPosition().getLatitude();
            entity.longitude = currentMarker.getPosition().getLongitude();
            db.locationDao().insert(entity);
            Toast.makeText(this, entity.name + " を登録しました！", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
