package com.example.gpslog;

import android.database.Cursor;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class LocationDao_Impl implements LocationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocationEntity> __insertionAdapterOfLocationEntity;

  private final EntityInsertionAdapter<LocationLogEntity> __insertionAdapterOfLocationLogEntity;

  private final EntityDeletionOrUpdateAdapter<LocationLogEntity> __updateAdapterOfLocationLogEntity;

  public LocationDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocationEntity = new EntityInsertionAdapter<LocationEntity>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `locations` (`id`,`name`,`latitude`,`longitude`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, LocationEntity value) {
        stmt.bindLong(1, value.id);
        if (value.name == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.name);
        }
        stmt.bindDouble(3, value.latitude);
        stmt.bindDouble(4, value.longitude);
      }
    };
    this.__insertionAdapterOfLocationLogEntity = new EntityInsertionAdapter<LocationLogEntity>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `visit_logs` (`logId`,`locationId`,`locationName`,`entryTime`,`exitTime`,`stayDuration`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, LocationLogEntity value) {
        stmt.bindLong(1, value.logId);
        stmt.bindLong(2, value.locationId);
        if (value.locationName == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.locationName);
        }
        stmt.bindLong(4, value.entryTime);
        stmt.bindLong(5, value.exitTime);
        stmt.bindLong(6, value.stayDuration);
      }
    };
    this.__updateAdapterOfLocationLogEntity = new EntityDeletionOrUpdateAdapter<LocationLogEntity>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `visit_logs` SET `logId` = ?,`locationId` = ?,`locationName` = ?,`entryTime` = ?,`exitTime` = ?,`stayDuration` = ? WHERE `logId` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, LocationLogEntity value) {
        stmt.bindLong(1, value.logId);
        stmt.bindLong(2, value.locationId);
        if (value.locationName == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.locationName);
        }
        stmt.bindLong(4, value.entryTime);
        stmt.bindLong(5, value.exitTime);
        stmt.bindLong(6, value.stayDuration);
        stmt.bindLong(7, value.logId);
      }
    };
  }

  @Override
  public void insert(final LocationEntity location) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLocationEntity.insert(location);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertLog(final LocationLogEntity log) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLocationLogEntity.insert(log);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateLog(final LocationLogEntity log) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfLocationLogEntity.handle(log);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<LocationEntity> getAll() {
    final String _sql = "SELECT * FROM locations";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
      final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
      final List<LocationEntity> _result = new ArrayList<LocationEntity>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final LocationEntity _item;
        _item = new LocationEntity();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfName)) {
          _item.name = null;
        } else {
          _item.name = _cursor.getString(_cursorIndexOfName);
        }
        _item.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _item.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<LocationLogEntity> getAllLogs() {
    final String _sql = "SELECT * FROM visit_logs ORDER BY entryTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfLogId = CursorUtil.getColumnIndexOrThrow(_cursor, "logId");
      final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
      final int _cursorIndexOfLocationName = CursorUtil.getColumnIndexOrThrow(_cursor, "locationName");
      final int _cursorIndexOfEntryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "entryTime");
      final int _cursorIndexOfExitTime = CursorUtil.getColumnIndexOrThrow(_cursor, "exitTime");
      final int _cursorIndexOfStayDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "stayDuration");
      final List<LocationLogEntity> _result = new ArrayList<LocationLogEntity>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final LocationLogEntity _item;
        _item = new LocationLogEntity();
        _item.logId = _cursor.getInt(_cursorIndexOfLogId);
        _item.locationId = _cursor.getInt(_cursorIndexOfLocationId);
        if (_cursor.isNull(_cursorIndexOfLocationName)) {
          _item.locationName = null;
        } else {
          _item.locationName = _cursor.getString(_cursorIndexOfLocationName);
        }
        _item.entryTime = _cursor.getLong(_cursorIndexOfEntryTime);
        _item.exitTime = _cursor.getLong(_cursorIndexOfExitTime);
        _item.stayDuration = _cursor.getLong(_cursorIndexOfStayDuration);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LocationLogEntity getActiveLog(final int locId) {
    final String _sql = "SELECT * FROM visit_logs WHERE locationId = ? AND exitTime = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, locId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfLogId = CursorUtil.getColumnIndexOrThrow(_cursor, "logId");
      final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
      final int _cursorIndexOfLocationName = CursorUtil.getColumnIndexOrThrow(_cursor, "locationName");
      final int _cursorIndexOfEntryTime = CursorUtil.getColumnIndexOrThrow(_cursor, "entryTime");
      final int _cursorIndexOfExitTime = CursorUtil.getColumnIndexOrThrow(_cursor, "exitTime");
      final int _cursorIndexOfStayDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "stayDuration");
      final LocationLogEntity _result;
      if(_cursor.moveToFirst()) {
        _result = new LocationLogEntity();
        _result.logId = _cursor.getInt(_cursorIndexOfLogId);
        _result.locationId = _cursor.getInt(_cursorIndexOfLocationId);
        if (_cursor.isNull(_cursorIndexOfLocationName)) {
          _result.locationName = null;
        } else {
          _result.locationName = _cursor.getString(_cursorIndexOfLocationName);
        }
        _result.entryTime = _cursor.getLong(_cursorIndexOfEntryTime);
        _result.exitTime = _cursor.getLong(_cursorIndexOfExitTime);
        _result.stayDuration = _cursor.getLong(_cursorIndexOfStayDuration);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
