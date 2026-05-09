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
    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1") LocationEntity getLocationById(int id);

    @Query("SELECT * FROM visit_logs ORDER BY entryTime DESC") List<LocationLogEntity> getAllLogs();
    @Query("DELETE FROM visit_logs WHERE locationId = :locId") void deleteLogsByLocationId(int locId);

    @Insert(onConflict = OnConflictStrategy.IGNORE) void insertDaily(DailyAccumulator item);
    @Query("SELECT * FROM daily_totals WHERE locationId = :locId AND date = :date LIMIT 1") DailyAccumulator getDaily(int locId, String date);
    @Update void updateDaily(DailyAccumulator item);
    @Query("DELETE FROM daily_totals WHERE locationId = :locId AND date = :date") void deleteDaily(int locId, String date);
    @Query("DELETE FROM daily_totals WHERE locationId = :locId") void deleteAllDailyForLocation(int locId);

    // ✅ 新規：履歴用の命令
    @Insert void insertHistory(VisitHistory history);
    @Query("SELECT * FROM visit_history WHERE date = :date ORDER BY timestamp DESC") List<VisitHistory> getHistoryForDate(String date);
    @Query("SELECT * FROM visit_history WHERE locationId = :locId ORDER BY timestamp DESC LIMIT 1") VisitHistory getLastHistory(int locId);
    @Query("DELETE FROM visit_history WHERE locationId = :locId AND date = :date") void deleteHistoryDaily(int locId, String date);
    @Query("DELETE FROM visit_history WHERE locationId = :locId") void deleteAllHistoryForLocation(int locId);
}
