package com.example.gpslog;

import android.app.ActivityManager;
import android.content.Context;
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
        
        // ✅ 追加：指2本でのピンチイン・ピンチアウト（ズーム）を有効化
        mapView.setMultiTouchControls(true); 

        etName = findViewById(R.id.etLocationName);
        Button btnJump = findViewById(R.id.btnJumpToCurrent);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build();

        loadMarkers();
        GeoPoint p = new GeoPoint(35.7295, 139.7109);
        mapView.getController().setZoom(17.0);
        mapView.getController().setCenter(p);

        currentMarker = new Marker(mapView);
        currentMarker.setPosition(p);
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(currentMarker);

        btnJump.setOnClickListener(v -> {
            if (!isServiceRunning(GpsLoggingService.class)) {
                Toast.makeText(this, "現在地を表示するには、OnlineをONにしてください", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(loc -> {
                    if (loc != null) {
                        GeoPoint my = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                        mapView.getController().animateTo(my);
                        currentMarker.setPosition(my);
                        mapView.invalidate();
                    }
                });
            } catch (SecurityException e) { e.printStackTrace(); }
        });

        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override public boolean longPressHelper(GeoPoint p) { currentMarker.setPosition(p); mapView.invalidate(); return true; }
        }));
    }

    private void loadMarkers() {
        List<LocationEntity> all = db.locationDao().getAll();
        for (LocationEntity loc : all) {
            Marker m = new Marker(mapView);
            m.setPosition(new GeoPoint(loc.latitude, loc.longitude));
            m.setTitle(loc.name);
            // 既存のアイコン設定をそのまま使用
            m.setIcon(getResources().getDrawable(android.R.drawable.btn_star_big_on));
            mapView.getOverlays().add(m);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.map_menu, menu); return true; }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_save) {
            LocationEntity e = new LocationEntity();
            e.name = etName.getText().toString().isEmpty() ? "無題" : etName.getText().toString();
            e.latitude = currentMarker.getPosition().getLatitude();
            e.longitude = currentMarker.getPosition().getLongitude();
            e.displayOrder = db.locationDao().getAll().size();
            db.locationDao().insert(e);
            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
            loadMarkers();
            mapView.invalidate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isServiceRunning(Class<?> s) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo r : am.getRunningServices(Integer.MAX_VALUE)) {
            if (s.getName().equals(r.service.getClassName())) return true;
        }
        return false;
    }
    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }
}
