package com.example.gpslog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private LocationDao dao;
    private List<LocationEntity> locationList;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db")
                .allowMainThreadQueries().build();
        dao = db.locationDao();

        refreshList();
    }

    private void refreshList() {
        locationList = dao.getAll();
        rv.setAdapter(new LocationAdapter());
    }

    class LocationAdapter extends RecyclerView.Adapter<LocationViewHolder> {
        @NonNull @Override public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new LocationViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_location, p, false));
        }

        @Override public void onBindViewHolder(@NonNull LocationViewHolder h, int p) {
            LocationEntity loc = locationList.get(p);
            h.name.setText(loc.name);

            // 削除ボタン
            h.btnDelete.setOnClickListener(v -> {
                dao.delete(loc);
                refreshList();
            });

            // 上へ移動 ✅
            h.btnUp.setOnClickListener(v -> {
                if (p > 0) swap(p, p - 1);
            });

            // 下へ移動 ✅
            h.btnDown.setOnClickListener(v -> {
                if (p < locationList.size() - 1) swap(p, p + 1);
            });
        }

        private void swap(int from, int to) {
            LocationEntity fromLoc = locationList.get(from);
            LocationEntity toLoc = locationList.get(to);
            
            // displayOrderを入れ替えて保存
            int tempOrder = fromLoc.displayOrder;
            fromLoc.displayOrder = toLoc.displayOrder;
            toLoc.displayOrder = tempOrder;

            dao.update(fromLoc);
            dao.update(toLoc);
            refreshList();
        }

        @Override public int getItemCount() { return locationList.size(); }
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView name; Button btnUp, btnDown, btnDelete;
        LocationViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.tvLocationName);
            btnUp = v.findViewById(R.id.btnUp);
            btnDown = v.findViewById(R.id.btnDown);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
