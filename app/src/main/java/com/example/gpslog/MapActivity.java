package com.example.gpslog;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker currentMarker;
    private AppDatabase db;
    private EditText etName;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.map);
        etName = findViewById(R.id.etLocationName);
        Button btnJump = findViewById(R.id.btnJumpToCurrent);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db")
                .allowMainThreadQueries().build();

        loadSavedMarkers();
        GeoPoint startPoint = new GeoPoint(35.7295, 139.7109);
        mapView.getController().setZoom(17.0);
        mapView.getController().setCenter(startPoint);

        currentMarker = new Marker(mapView);
        currentMarker.setPosition(startPoint);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(currentMarker);

        // ◎ボタン：クリックしやすく、確実に現在地へ
        btnJump.setOnClickListener(v -> {
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint myLoc = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapView.getController().animateTo(myLoc);
                            currentMarker.setPosition(myLoc);
                            mapView.invalidate();
                        }
                    });
            } catch (SecurityException e) { e.printStackTrace(); }
        });

        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override public boolean longPressHelper(GeoPoint p) {
                currentMarker.setPosition(p);
                mapView.invalidate();
                return true;
            }
        }));
    }

    private void loadSavedMarkers() {
        List<LocationEntity> allLocations = db.locationDao().getAll();
        for (LocationEntity loc : allLocations) {
            if (loc.latitude == 0.0) continue;
            Marker m = new Marker(mapView);
            m.setPosition(new GeoPoint(loc.latitude, loc.longitude));
            m.setTitle(loc.name);
            mapView.getOverlays().add(m);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_save) {
            LocationEntity entity = new LocationEntity();
            entity.name = etName.getText().toString().isEmpty() ? "無題" : etName.getText().toString();
            entity.latitude = currentMarker.getPosition().getLatitude();
            entity.longitude = currentMarker.getPosition().getLongitude();
            db.locationDao().insert(entity);
            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }
}
