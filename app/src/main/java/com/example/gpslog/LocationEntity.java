package com.example.gpslog;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "locations")
public class LocationEntity {
    @PrimaryKey(autoGenerate = true) public int id;
    public String name;
    public double latitude;
    public double longitude;
}
