package com.example.gpslog;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.Delegate;
import androidx.room.RoomOpenHelper.ValidationResult;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LocationDao _locationDao;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `displayOrder` INTEGER NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `visit_logs` (`logId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `locationId` INTEGER NOT NULL, `locationName` TEXT, `entryTime` INTEGER NOT NULL, `exitTime` INTEGER NOT NULL, `stayDuration` INTEGER NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `daily_totals` (`locationId` INTEGER NOT NULL, `date` TEXT NOT NULL, `totalInMs` INTEGER NOT NULL, `totalOutMs` INTEGER NOT NULL, PRIMARY KEY(`locationId`, `date`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `visit_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `locationId` INTEGER NOT NULL, `date` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isEntry` INTEGER NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd319938dbff1b6d05716cd3862a69804')");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `locations`");
        _db.execSQL("DROP TABLE IF EXISTS `visit_logs`");
        _db.execSQL("DROP TABLE IF EXISTS `daily_totals`");
        _db.execSQL("DROP TABLE IF EXISTS `visit_history`");
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onDestructiveMigration(_db);
          }
        }
      }

      @Override
      public void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      @Override
      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      @Override
      public void onPreMigrate(SupportSQLiteDatabase _db) {
        DBUtil.dropFtsSyncTriggers(_db);
      }

      @Override
      public void onPostMigrate(SupportSQLiteDatabase _db) {
      }

      @Override
      public RoomOpenHelper.ValidationResult onValidateSchema(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsLocations = new HashMap<String, TableInfo.Column>(5);
        _columnsLocations.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocations.put("displayOrder", new TableInfo.Column("displayOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocations = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocations = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocations = new TableInfo("locations", _columnsLocations, _foreignKeysLocations, _indicesLocations);
        final TableInfo _existingLocations = TableInfo.read(_db, "locations");
        if (! _infoLocations.equals(_existingLocations)) {
          return new RoomOpenHelper.ValidationResult(false, "locations(com.example.gpslog.LocationEntity).\n"
                  + " Expected:\n" + _infoLocations + "\n"
                  + " Found:\n" + _existingLocations);
        }
        final HashMap<String, TableInfo.Column> _columnsVisitLogs = new HashMap<String, TableInfo.Column>(6);
        _columnsVisitLogs.put("logId", new TableInfo.Column("logId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitLogs.put("locationId", new TableInfo.Column("locationId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitLogs.put("locationName", new TableInfo.Column("locationName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitLogs.put("entryTime", new TableInfo.Column("entryTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitLogs.put("exitTime", new TableInfo.Column("exitTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitLogs.put("stayDuration", new TableInfo.Column("stayDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVisitLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVisitLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVisitLogs = new TableInfo("visit_logs", _columnsVisitLogs, _foreignKeysVisitLogs, _indicesVisitLogs);
        final TableInfo _existingVisitLogs = TableInfo.read(_db, "visit_logs");
        if (! _infoVisitLogs.equals(_existingVisitLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "visit_logs(com.example.gpslog.LocationLogEntity).\n"
                  + " Expected:\n" + _infoVisitLogs + "\n"
                  + " Found:\n" + _existingVisitLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyTotals = new HashMap<String, TableInfo.Column>(4);
        _columnsDailyTotals.put("locationId", new TableInfo.Column("locationId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyTotals.put("date", new TableInfo.Column("date", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyTotals.put("totalInMs", new TableInfo.Column("totalInMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyTotals.put("totalOutMs", new TableInfo.Column("totalOutMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyTotals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyTotals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyTotals = new TableInfo("daily_totals", _columnsDailyTotals, _foreignKeysDailyTotals, _indicesDailyTotals);
        final TableInfo _existingDailyTotals = TableInfo.read(_db, "daily_totals");
        if (! _infoDailyTotals.equals(_existingDailyTotals)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_totals(com.example.gpslog.DailyAccumulator).\n"
                  + " Expected:\n" + _infoDailyTotals + "\n"
                  + " Found:\n" + _existingDailyTotals);
        }
        final HashMap<String, TableInfo.Column> _columnsVisitHistory = new HashMap<String, TableInfo.Column>(5);
        _columnsVisitHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitHistory.put("locationId", new TableInfo.Column("locationId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitHistory.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVisitHistory.put("isEntry", new TableInfo.Column("isEntry", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVisitHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVisitHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVisitHistory = new TableInfo("visit_history", _columnsVisitHistory, _foreignKeysVisitHistory, _indicesVisitHistory);
        final TableInfo _existingVisitHistory = TableInfo.read(_db, "visit_history");
        if (! _infoVisitHistory.equals(_existingVisitHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "visit_history(com.example.gpslog.VisitHistory).\n"
                  + " Expected:\n" + _infoVisitHistory + "\n"
                  + " Found:\n" + _existingVisitHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "d319938dbff1b6d05716cd3862a69804", "38eeafa8ecacfff450ca33fcfffb30da");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "locations","visit_logs","daily_totals","visit_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `locations`");
      _db.execSQL("DELETE FROM `visit_logs`");
      _db.execSQL("DELETE FROM `daily_totals`");
      _db.execSQL("DELETE FROM `visit_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LocationDao.class, LocationDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  public List<Migration> getAutoMigrations(
      @NonNull Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecsMap) {
    return Arrays.asList();
  }

  @Override
  public LocationDao locationDao() {
    if (_locationDao != null) {
      return _locationDao;
    } else {
      synchronized(this) {
        if(_locationDao == null) {
          _locationDao = new LocationDao_Impl(this);
        }
        return _locationDao;
      }
    }
  }
}
