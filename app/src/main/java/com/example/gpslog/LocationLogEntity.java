package com.example.gpslog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "visit_logs")
public class LocationLogEntity {
    @PrimaryKey(autoGenerate = true)
    public int logId;
    public int locationId;    // どの場所か
    public String locationName;
    public long entryTime;    // 入った時刻（ミリ秒）
    public long exitTime;     // 出た時刻（ミリ秒）
    public long stayDuration; // 滞在時間（ミリ秒）
}
