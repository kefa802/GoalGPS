package com.example.gpslog;
import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LocationEntity.class, LocationLogEntity.class, DailyAccumulator.class, VisitHistory.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
}
