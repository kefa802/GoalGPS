package com.example.gpslog;

import androidx.room.Database;
import androidx.room.RoomDatabase;

// ✅ バージョンを「4」に上げることで、クラウドから復元された過去の矛盾データを強制リセットします
@Database(entities = {LocationEntity.class, LocationLogEntity.class, DailyAccumulator.class, VisitHistory.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
}
