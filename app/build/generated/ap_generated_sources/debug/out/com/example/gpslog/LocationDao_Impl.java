package com.example.gpslog;

import android.database.Cursor;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
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

  private final EntityInsertionAdapter<DailyAccumulator> __insertionAdapterOfDailyAccumulator;

  private final EntityInsertionAdapter<VisitHistory> __insertionAdapterOfVisitHistory;

  private final EntityDeletionOrUpdateAdapter<LocationEntity> __deletionAdapterOfLocationEntity;

  private final EntityDeletionOrUpdateAdapter<LocationEntity> __updateAdapterOfLocationEntity;

  private final EntityDeletionOrUpdateAdapter<DailyAccumulator> __updateAdapterOfDailyAccumulator;

  private final SharedSQLiteStatement __preparedStmtOfDeleteLogsByLocationId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteDaily;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllDailyForLocation;

  private final SharedSQLiteStatement __preparedStmtOfDeleteHistoryDaily;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllHistoryForLocation;

  private final SharedSQLiteStatement __preparedStmtOfDeleteHistoryById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllHistoryForDate;

  public LocationDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocationEntity = new EntityInsertionAdapter<LocationEntity>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `locations` (`id`,`name`,`latitude`,`longitude`,`displayOrder`) VALUES (nullif(?, 0),?,?,?,?)";
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
        stmt.bindLong(5, value.displayOrder);
      }
    };
    this.__insertionAdapterOfDailyAccumulator = new EntityInsertionAdapter<DailyAccumulator>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR IGNORE INTO `daily_totals` (`locationId`,`date`,`totalInMs`,`totalOutMs`) VALUES (?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, DailyAccumulator value) {
        stmt.bindLong(1, value.locationId);
        if (value.date == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.date);
        }
        stmt.bindLong(3, value.totalInMs);
        stmt.bindLong(4, value.totalOutMs);
      }
    };
    this.__insertionAdapterOfVisitHistory = new EntityInsertionAdapter<VisitHistory>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `visit_history` (`id`,`locationId`,`date`,`timestamp`,`isEntry`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, VisitHistory value) {
        stmt.bindLong(1, value.id);
        stmt.bindLong(2, value.locationId);
        if (value.date == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.date);
        }
        stmt.bindLong(4, value.timestamp);
        final int _tmp = value.isEntry ? 1 : 0;
        stmt.bindLong(5, _tmp);
      }
    };
    this.__deletionAdapterOfLocationEntity = new EntityDeletionOrUpdateAdapter<LocationEntity>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `locations` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, LocationEntity value) {
        stmt.bindLong(1, value.id);
      }
    };
    this.__updateAdapterOfLocationEntity = new EntityDeletionOrUpdateAdapter<LocationEntity>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `locations` SET `id` = ?,`name` = ?,`latitude` = ?,`longitude` = ?,`displayOrder` = ? WHERE `id` = ?";
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
        stmt.bindLong(5, value.displayOrder);
        stmt.bindLong(6, value.id);
      }
    };
    this.__updateAdapterOfDailyAccumulator = new EntityDeletionOrUpdateAdapter<DailyAccumulator>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `daily_totals` SET `locationId` = ?,`date` = ?,`totalInMs` = ?,`totalOutMs` = ? WHERE `locationId` = ? AND `date` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, DailyAccumulator value) {
        stmt.bindLong(1, value.locationId);
        if (value.date == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.date);
        }
        stmt.bindLong(3, value.totalInMs);
        stmt.bindLong(4, value.totalOutMs);
        stmt.bindLong(5, value.locationId);
        if (value.date == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.date);
        }
      }
    };
    this.__preparedStmtOfDeleteLogsByLocationId = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM visit_logs WHERE locationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteDaily = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM daily_totals WHERE locationId = ? AND date = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllDailyForLocation = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM daily_totals WHERE locationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteHistoryDaily = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM visit_history WHERE locationId = ? AND date = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllHistoryForLocation = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM visit_history WHERE locationId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteHistoryById = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM visit_history WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllHistoryForDate = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM visit_history WHERE date = ?";
        return _query;
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
  public void insertDaily(final DailyAccumulator item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfDailyAccumulator.insert(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertHistory(final VisitHistory history) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfVisitHistory.insert(history);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final LocationEntity location) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfLocationEntity.handle(location);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final LocationEntity location) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfLocationEntity.handle(location);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateDaily(final DailyAccumulator item) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfDailyAccumulator.handle(item);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteLogsByLocationId(final int locId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteLogsByLocationId.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, locId);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteLogsByLocationId.release(_stmt);
    }
  }

  @Override
  public void deleteDaily(final int locId, final String date) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteDaily.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, locId);
    _argIndex = 2;
    if (date == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, date);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteDaily.release(_stmt);
    }
  }

  @Override
  public void deleteAllDailyForLocation(final int locId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllDailyForLocation.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, locId);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAllDailyForLocation.release(_stmt);
    }
  }

  @Override
  public void deleteHistoryDaily(final int locId, final String date) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteHistoryDaily.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, locId);
    _argIndex = 2;
    if (date == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, date);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteHistoryDaily.release(_stmt);
    }
  }

  @Override
  public void deleteAllHistoryForLocation(final int locId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllHistoryForLocation.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, locId);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAllHistoryForLocation.release(_stmt);
    }
  }

  @Override
  public void deleteHistoryById(final int id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteHistoryById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteHistoryById.release(_stmt);
    }
  }

  @Override
  public void deleteAllHistoryForDate(final String date) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllHistoryForDate.acquire();
    int _argIndex = 1;
    if (date == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, date);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAllHistoryForDate.release(_stmt);
    }
  }

  @Override
  public List<LocationEntity> getAll() {
    final String _sql = "SELECT * FROM locations ORDER BY displayOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
      final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
      final int _cursorIndexOfDisplayOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "displayOrder");
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
        _item.displayOrder = _cursor.getInt(_cursorIndexOfDisplayOrder);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LocationEntity getLocationById(final int id) {
    final String _sql = "SELECT * FROM locations WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
      final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
      final int _cursorIndexOfDisplayOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "displayOrder");
      final LocationEntity _result;
      if(_cursor.moveToFirst()) {
        _result = new LocationEntity();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfName)) {
          _result.name = null;
        } else {
          _result.name = _cursor.getString(_cursorIndexOfName);
        }
        _result.latitude = _cursor.getDouble(_cursorIndexOfLatitude);
        _result.longitude = _cursor.getDouble(_cursorIndexOfLongitude);
        _result.displayOrder = _cursor.getInt(_cursorIndexOfDisplayOrder);
      } else {
        _result = null;
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
  public DailyAccumulator getDaily(final int locId, final String date) {
    final String _sql = "SELECT * FROM daily_totals WHERE locationId = ? AND date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, locId);
    _argIndex = 2;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfTotalInMs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalInMs");
      final int _cursorIndexOfTotalOutMs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalOutMs");
      final DailyAccumulator _result;
      if(_cursor.moveToFirst()) {
        _result = new DailyAccumulator();
        _result.locationId = _cursor.getInt(_cursorIndexOfLocationId);
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _result.date = null;
        } else {
          _result.date = _cursor.getString(_cursorIndexOfDate);
        }
        _result.totalInMs = _cursor.getLong(_cursorIndexOfTotalInMs);
        _result.totalOutMs = _cursor.getLong(_cursorIndexOfTotalOutMs);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<VisitHistory> getHistoryForDate(final String date) {
    final String _sql = "SELECT * FROM visit_history WHERE date = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (date == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, date);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfIsEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isEntry");
      final List<VisitHistory> _result = new ArrayList<VisitHistory>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final VisitHistory _item;
        _item = new VisitHistory();
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _item.locationId = _cursor.getInt(_cursorIndexOfLocationId);
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _item.date = null;
        } else {
          _item.date = _cursor.getString(_cursorIndexOfDate);
        }
        _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsEntry);
        _item.isEntry = _tmp != 0;
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public VisitHistory getLastHistory(final int locId) {
    final String _sql = "SELECT * FROM visit_history WHERE locationId = ? ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, locId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfLocationId = CursorUtil.getColumnIndexOrThrow(_cursor, "locationId");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfIsEntry = CursorUtil.getColumnIndexOrThrow(_cursor, "isEntry");
      final VisitHistory _result;
      if(_cursor.moveToFirst()) {
        _result = new VisitHistory();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        _result.locationId = _cursor.getInt(_cursorIndexOfLocationId);
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _result.date = null;
        } else {
          _result.date = _cursor.getString(_cursorIndexOfDate);
        }
        _result.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsEntry);
        _result.isEntry = _tmp != 0;
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
