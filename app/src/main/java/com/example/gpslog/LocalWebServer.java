package com.example.gpslog;

import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import java.util.List;

public class LocalWebServer extends NanoHTTPD {
    private AppDatabase db;
    private Gson gson;

    public LocalWebServer(int port, AppDatabase db) {
        super(port);
        this.db = db;
        this.gson = new Gson();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        
        // Webアプリ(JS)からアクセスされた時のためのCORS事前確認対応
        if (Method.OPTIONS.equals(session.getMethod())) {
            response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "");
        } 
        // /api/logs にアクセスが来たらデータを返す
        else if ("/api/logs".equals(session.getUri())) {
            List<LocationLogEntity> logs = db.locationDao().getAllLogs();
            String json = gson.toJson(logs); // データベースのログをJSONに変換
            response = newFixedLengthResponse(Response.Status.OK, "application/json", json);
        } 
        // それ以外のURL
        else {
            response = newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "GoalGPS API is running. Use /api/logs");
        }

        // ✅ どのWebアプリからでも受信できるようにCORS(セキュリティ壁の突破)を設定
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "origin, accept, content-type");
        response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        
        return response;
    }
}
