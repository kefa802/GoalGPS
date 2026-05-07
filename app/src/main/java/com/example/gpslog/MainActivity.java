// (中略：onCreateなどの基本構造は維持)

            h.itemView.setOnLongClickListener(v -> {
                CharSequence[] items = {"この日の時間をクリア", "上へ移動", "下へ移動", "削除", "📍 ここにワープ(テスト用)"};
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle(loc.name + " の操作")
                    .setItems(items, (dialog, which) -> {
                        switch (which) {
                            case 0: // ✅ 修正：表示中の日付のみを削除する
                                Calendar startCal = (Calendar) displayDate.clone();
                                startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);
                                long start = startCal.getTimeInMillis();

                                Calendar endCal = (Calendar) displayDate.clone();
                                endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999);
                                long end = endCal.getTimeInMillis();

                                db.locationDao().deleteLogsInRange(loc.id, start, end);
                                refreshData();
                                String dateStr = new SimpleDateFormat("MM/dd", Locale.JAPAN).format(displayDate.getTime());
                                Toast.makeText(MainActivity.this, dateStr + " の時間をリセットしました", Toast.LENGTH_SHORT).show();
                                break;
// (以下、ケース1〜4はそのまま)
