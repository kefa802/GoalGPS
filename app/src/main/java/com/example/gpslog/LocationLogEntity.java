package com.example.gpslog;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "visit_logs")
public class LocationLogEntity {
    @PrimaryKey(autoGenerate = true) public int logId;
    public int locationId;
    public String locationName;
    public long entryTime;
    public long exitTime;
    public long stayDuration;
}
