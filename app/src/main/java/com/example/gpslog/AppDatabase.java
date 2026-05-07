package com.example.gpslog;
import androidx.room.Database;
import androidx.room.RoomDatabase;

// versionを 3 に変更 ✅
@Database(entities = {LocationEntity.class, LocationLogEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
}
