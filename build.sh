#!/bin/bash
export ANDROID_HOME="/workspaces/GoalGPS/my-android-sdk"
export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "🧹 過去のゴミAPKファイルを掃除中..."
rm -f GoalGPS*.apk
rm -f build_error.log
rm -rf app/build build

echo "🚀 Building GoalGPS..."
# ✅ 画面に出力しつつ、build_error.log にも保存する
./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.java.home=$JAVA_HOME 2>&1 | tee build_error.log

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo "✅ SUCCESS!"
    APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
    cp "$APK" "./GoalGPS.apk"
    echo "🎉 GoalGPS.apk をダウンロードして上書きインストールしてください！"
    # 成功時はログファイルを削除（必要なら残す設定にもできます）
    rm -f build_error.log
else
    echo "❌ BUILD FAILED!"
    echo "⚠️ エラー内容は build_error.log を確認してください。"
fi
exit 0
