package com.example.gpslog;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "daily_totals", primaryKeys = {"locationId", "date"})
public class DailyAccumulator {
    public int locationId;
    @NonNull
    public String date; // "yyyy-MM-dd"
    public long totalInMs;
    public long totalOutMs;
}
