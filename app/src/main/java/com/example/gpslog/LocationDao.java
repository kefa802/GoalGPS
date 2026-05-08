package com.example.gpslog;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface LocationDao {
    @Insert void insert(LocationEntity location);
    @Query("SELECT * FROM locations ORDER BY displayOrder ASC") List<LocationEntity> getAll();
    @Update void update(LocationEntity location);
    @Delete void delete(LocationEntity location);

    // ✅ 単純加算ロジック用の命令
    @Insert(onConflict = OnConflictStrategy.IGNORE) void insertDaily(DailyAccumulator item);
    @Query("SELECT * FROM daily_totals WHERE locationId = :locId AND date = :date LIMIT 1") DailyAccumulator getDaily(int locId, String date);
    @Update void updateDaily(DailyAccumulator item);
    @Query("DELETE FROM daily_totals WHERE locationId = :locId AND date = :date") void deleteDaily(int locId, String date);
}
