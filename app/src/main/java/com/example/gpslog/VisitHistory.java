package com.example.gpslog;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "visit_history")
public class VisitHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int locationId;
    @NonNull
    public String date; // "yyyy-MM-dd"
    public long timestamp;
    public boolean isEntry; // true=IN, false=OUT
}
