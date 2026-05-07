#!/bin/bash
LOG_FILE="last_build_error.log"
echo "=== Build Started at $(date) ===" > $LOG_FILE

export ANDROID_HOME="/workspaces/GoalGPS/my-android-sdk"
export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

if [ ! -f "version.properties" ]; then
    echo "🆕 version.properties を初期化中..." | tee -a $LOG_FILE
    echo -e "VERSION_CODE=1\nVERSION_NAME=1.0.0" > version.properties
fi

echo "📦 必要なSDKコンポーネントを確認中..." | tee -a $LOG_FILE
yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME >> $LOG_FILE 2>&1
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0" >> $LOG_FILE 2>&1

if [ ! -f "./gradlew" ]; then
    gradle wrapper >> $LOG_FILE 2>&1
fi
chmod +x gradlew 2>/dev/null

echo "🧹 過去のビルドゴミを掃除中..." | tee -a $LOG_FILE
rm -rf app/build build >> $LOG_FILE 2>&1
./gradlew clean --no-daemon -Dorg.gradle.java.home=$JAVA_HOME >> $LOG_FILE 2>&1

echo "🚀 Building GoalGPS (Clean Build)..." | tee -a $LOG_FILE
./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.java.home=$JAVA_HOME --stacktrace >> $LOG_FILE 2>&1

if [ $? -eq 0 ]; then
    echo "✅ SUCCESS! 完璧な状態でビルドが完了しました！" | tee -a $LOG_FILE
    
    # ✅ 修正：バージョン番号を取得してユニークなファイル名を作成
    V_CODE=$(grep "VERSION_CODE" version.properties | cut -d'=' -f2)
    APK_NAME="GoalGPS_v${V_CODE}.apk"
    
    APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
    cp "$APK" "./$APK_NAME"
    echo "🎉 $APK_NAME をダウンロードしてインストールしてください！"
else
    echo "❌ BUILD FAILED! エラーログを確認してください。" | tee -a $LOG_FILE
    grep -A 50 "What went wrong:" $LOG_FILE
fi
exit 0
