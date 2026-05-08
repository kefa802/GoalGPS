package com.example.gpslog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_totals", primaryKeys = {"locationId", "date"})
public class DailyAccumulator {
    public int locationId;
    public String date; // "yyyy-MM-dd"
    public long totalInMs;
    public long totalOutMs;
}
