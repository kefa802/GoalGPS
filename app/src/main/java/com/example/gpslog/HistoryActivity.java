package com.example.gpslog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // データベースから全件取得
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "goal_gps_db")
                .allowMainThreadQueries().build();
        List<LocationEntity> list = db.locationDao().getAll();

        // 表示用のアダプターをセット
        rv.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
                return new ViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                LocationEntity item = list.get(position);
                holder.tvName.setText(item.name);
                holder.tvCoord.setText(String.format("緯度: %.6f, 経度: %.6f", item.latitude, item.longitude));
            }

            @Override
            public int getItemCount() { return list.size(); }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCoord;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvLocationName);
            tvCoord = v.findViewById(R.id.tvCoordinates);
        }
    }
}
