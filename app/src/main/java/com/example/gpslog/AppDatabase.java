// @Database の entities に LocationLogEntity.class を追加
@Database(entities = {LocationEntity.class, LocationLogEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
}
