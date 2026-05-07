#!/bin/bash
LOG_FILE="last_build_error.log"
echo "=== Build Started at $(date) ===" > $LOG_FILE

# 環境設定
export ANDROID_HOME="/workspaces/GoalGPS/my-android-sdk"
export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# ✅ バージョン管理ファイルの初期設定（マージした処理）
if [ ! -f "version.properties" ]; then
    echo "🆕 version.properties を初期化中..." | tee -a $LOG_FILE
    echo -e "VERSION_CODE=1\nVERSION_NAME=1.0.0" > version.properties
    echo "✅ version.properties を作成しました。" | tee -a $LOG_FILE
fi

# 📦 SDKの準備
echo "📦 必要なSDKコンポーネントを確認中..." | tee -a $LOG_FILE
yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME >> $LOG_FILE 2>&1
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0" >> $LOG_FILE 2>&1

# 🏗️ gradlew 生成
if [ ! -f "./gradlew" ]; then
    echo "🛠️ gradlew を生成中..." | tee -a $LOG_FILE
    gradle wrapper >> $LOG_FILE 2>&1
fi
chmod +x gradlew 2>/dev/null

# 🧹 クリーン
echo "🧹 過去のビルドゴミを掃除中..." | tee -a $LOG_FILE
rm -rf app/build build >> $LOG_FILE 2>&1
./gradlew clean -Dorg.gradle.java.home=$JAVA_HOME >> $LOG_FILE 2>&1

echo "🚀 Building GoalGPS (Clean Build)..." | tee -a $LOG_FILE

# ビルド実行
./gradlew :app:assembleDebug -Dorg.gradle.java.home=$JAVA_HOME --stacktrace >> $LOG_FILE 2>&1

# 結果判定
if [ $? -eq 0 ]; then
    echo "✅ SUCCESS! 完璧な状態でビルドが完了しました！" | tee -a $LOG_FILE
    grep "VERSION_CODE" version.properties | tee -a $LOG_FILE
    APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
    cp "$APK" ./GoalGPS_latest.apk
    echo "🎉 GoalGPS_latest.apk をダウンロードして上書きインストールしてください！"
else
    echo "❌ BUILD FAILED! エラーログを確認してください。" | tee -a $LOG_FILE
    grep -A 50 "What went wrong:" $LOG_FILE
fi
exit 0
