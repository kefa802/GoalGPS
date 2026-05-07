// MapActivity.java 内の btnJump クリック時
btnJump.setOnClickListener(v -> {
    if (!isServiceRunning(GpsLoggingService.class)) {
        Toast.makeText(this, "現在地を表示するには、OnlineをONにしてください", Toast.LENGTH_LONG).show();
        return;
    }
    // ...位置取得の処理...
});
