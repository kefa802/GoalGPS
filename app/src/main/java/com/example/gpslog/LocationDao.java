package com.example.gpslog;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
@Dao
public interface LocationDao {
    @Insert void insert(LocationEntity location);
    @Query("SELECT * FROM locations") List<LocationEntity> getAll();
    @Insert void insertLog(LocationLogEntity log);
    @Update void updateLog(LocationLogEntity log);
    @Query("SELECT * FROM visit_logs ORDER BY entryTime DESC") List<LocationLogEntity> getAllLogs();
    @Query("SELECT * FROM visit_logs WHERE locationId = :locId AND exitTime = 0 LIMIT 1")
    LocationLogEntity getActiveLog(int locId);
}
