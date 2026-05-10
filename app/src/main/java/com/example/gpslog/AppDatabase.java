package com.example.gpslog;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import java.util.Calendar;
import java.util.List;

@Database(entities = {LocationEntity.class, LocationLogEntity.class, DailyAccumulator.class, VisitHistory.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();

    // ✅ 究極の計算エンジン：履歴ログだけを見て時間を算出する
    public static long[] calcTimes(LocationDao dao, int locId, Calendar targetDate, boolean isToday, long now) {
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(targetDate.getTime());
        List<VisitHistory> history = dao.getHistoryForDateAsc(locId, dateStr);

        Calendar cal = (Calendar) targetDate.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long endOfDay = startOfDay + 86400000L;

        long totalInMs = 0;
        long lastInTs = -1;

        for (VisitHistory h : history) {
            if (h.isEntry) {
                lastInTs = h.timestamp;
            } else {
                if (lastInTs != -1) { totalInMs += (h.timestamp - lastInTs); lastInTs = -1; } 
                else { totalInMs += (h.timestamp - startOfDay); }
            }
        }

        if (lastInTs != -1) {
            totalInMs += (isToday ? (now - lastInTs) : (endOfDay - lastInTs));
        }

        if (history.isEmpty()) {
            VisitHistory lastH = dao.getLastHistoryBefore(locId, startOfDay);
            if (lastH != null && lastH.isEntry) {
                totalInMs = isToday ? (now - startOfDay) : 86400000L;
            }
        }

        long totalDayMs = isToday ? (now - startOfDay) : 86400000L;
        if (totalDayMs < 0) totalDayMs = 0;
        long totalOutMs = totalDayMs - totalInMs;
        if (totalOutMs < 0) totalOutMs = 0;

        return new long[]{totalInMs, totalOutMs};
    }
}
