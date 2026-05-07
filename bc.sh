#!/bin/bash
LOG_FILE="last_build_error.log"
echo "=== Build Started at $(date) ===" > $LOG_FILE

# 環境設定（工藤さん専用SDKパスを維持）
export ANDROID_HOME="/workspaces/GoalGPS/my-android-sdk"
export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "🎯 Using Java: $JAVA_HOME" | tee -a $LOG_FILE
echo "📱 Using SDK: $ANDROID_HOME" | tee -a $LOG_FILE

# 📦 SDKの準備
echo "📦 必要なSDKコンポーネントをインストール中..." | tee -a $LOG_FILE
yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME >> $LOG_FILE 2>&1
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0" >> $LOG_FILE 2>&1

# 🏗️ gradlew 生成
if [ ! -f "./gradlew" ]; then
    echo "🛠️ gradlew を生成中..." | tee -a $LOG_FILE
    gradle wrapper >> $LOG_FILE 2>&1
fi
chmod +x gradlew 2>/dev/null

# 🧹 ✅ 追加：過去のキャッシュとビルドフォルダを物理削除
echo "🧹 過去のビルドゴミを掃除中..." | tee -a $LOG_FILE
rm -rf app/build build >> $LOG_FILE 2>&1
./gradlew clean -Dorg.gradle.java.home=$JAVA_HOME >> $LOG_FILE 2>&1

echo "🚀 Building GoalGPS (Clean Build)..." | tee -a $LOG_FILE

# ビルド実行
./gradlew :app:assembleDebug -Dorg.gradle.java.home=$JAVA_HOME --stacktrace >> $LOG_FILE 2>&1

# 結果判定とログ出力
if [ $? -eq 0 ]; then
    echo "✅ SUCCESS! 完璧な状態でビルドが完了しました！" | tee -a $LOG_FILE
    APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
    cp "$APK" ./GoalGPS_latest.apk
    echo "🎉 GoalGPS_latest.apk をダウンロードして上書きインストールしてください！"
else
    echo "❌ BUILD FAILED!" | tee -a $LOG_FILE
    echo "--------------------------------------------------"
    if grep -q "What went wrong:" $LOG_FILE; then
        sed -n '/What went wrong:/,$p' $LOG_FILE | head -n 50
    else
        tail -n 30 $LOG_FILE
    fi
    echo "--------------------------------------------------"
fi
exit 0
