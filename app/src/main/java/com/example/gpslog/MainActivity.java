// MainActivity.java 内の onCreate 等に追記、またはボタンの設定を確認
findViewById(R.id.btnRegister).setOnLongClickListener(v -> {
    startActivity(new Intent(this, HistoryActivity.class));
    return true;
});
// もしくは「地点リスト」ボタンが別にある場合は、それを使って HistoryActivity を開いてください
