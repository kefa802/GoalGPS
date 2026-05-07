package com.example.gpslog;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface LocationDao {
    @Insert void insert(LocationEntity location);
    @Query("SELECT * FROM locations ORDER BY displayOrder ASC") List<LocationEntity> getAll();
    @Update void update(LocationEntity location);
    @Delete void delete(LocationEntity location);

    @Insert void insertLog(LocationLogEntity log);
    @Update void updateLog(LocationLogEntity log);
    @Query("SELECT * FROM visit_logs ORDER BY entryTime DESC") List<LocationLogEntity> getAllLogs();

    @Query("SELECT * FROM visit_logs WHERE locationId = :locId AND entryTime <= :endTime ORDER BY entryTime DESC LIMIT 1")
    LocationLogEntity getLatestLog(int locId, long endTime);

    @Query("SELECT * FROM visit_logs WHERE locationId = :locId AND exitTime = 0 LIMIT 1")
    LocationLogEntity getActiveLog(int locId);

    // ✅ 追加：特定の地点の時間をすべてリセット（削除）する命令
    @Query("DELETE FROM visit_logs WHERE locationId = :locId")
    void deleteLogsByLocationId(int locId);
}
