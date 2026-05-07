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

    // ✅ displayOrder の順に取得するように変更
    @Query("SELECT * FROM locations ORDER BY displayOrder ASC") 
    List<LocationEntity> getAll();

    @Update void update(LocationEntity location); // ✅ 追加
    @Update void updateAll(List<LocationEntity> locations); // ✅ 追加
    @Delete void delete(LocationEntity location); // ✅ 追加

    @Insert void insertLog(LocationLogEntity log);
    @Update void updateLog(LocationLogEntity log);
    @Query("SELECT * FROM visit_logs ORDER BY entryTime DESC") List<LocationLogEntity> getAllLogs();
    @Query("SELECT * FROM visit_logs WHERE locationId = :locId AND exitTime = 0 LIMIT 1")
    LocationLogEntity getActiveLog(int locId);
}
