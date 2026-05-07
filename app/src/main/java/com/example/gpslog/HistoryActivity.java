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
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private LocationDao dao;
    private List<LocationEntity> list;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        dao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db").allowMainThreadQueries().build().locationDao();
        refresh();
    }

    private void refresh() {
        list = dao.getAll();
        rv.setAdapter(new RecyclerView.Adapter<VH>() {
            @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
                return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_location, p, false));
            }
            @Override public void onBindViewHolder(@NonNull VH h, int p) {
                LocationEntity loc = list.get(p);
                h.name.setText(loc.name);
                h.btnDel.setOnClickListener(v -> { dao.delete(loc); refresh(); });
                h.btnUp.setOnClickListener(v -> { if (p > 0) swap(p, p - 1); });
                h.btnDown.setOnClickListener(v -> { if (p < list.size() - 1) swap(p, p + 1); });
            }
            @Override public int getItemCount() { return list.size(); }
        });
    }

    private void swap(int f, int t) {
        LocationEntity from = list.get(f);
        LocationEntity to = list.get(t);
        int temp = from.displayOrder;
        from.displayOrder = to.displayOrder;
        to.displayOrder = temp;
        dao.update(from); dao.update(to);
        refresh();
    }
    static class VH extends RecyclerView.ViewHolder {
        TextView name; Button btnUp, btnDown, btnDel;
        VH(View v) { super(v); name = v.findViewById(R.id.tvLocationName); btnUp = v.findViewById(R.id.btnUp); btnDown = v.findViewById(R.id.btnDown); btnDel = v.findViewById(R.id.btnDelete); }
    }
}
