#!/bin/bash
LOG_FILE="last_build_error.log"
echo "=== Build Started at $(date) ===" > $LOG_FILE

# 環境設定（さっき作った自分専用のSDKを指定）
export ANDROID_HOME="/workspaces/GoalGPS/my-android-sdk"
export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "🎯 Using Java: $JAVA_HOME" | tee -a $LOG_FILE
echo "📱 Using SDK: $ANDROID_HOME" | tee -a $LOG_FILE

# 📦 Android開発に必要なパーツ（android-34など）を自前SDKの中にインストール
echo "📦 必要なSDKコンポーネントをインストール中（少し時間がかかります）..." | tee -a $LOG_FILE
yes | sdkmanager --licenses --sdk_root=$ANDROID_HOME >> $LOG_FILE 2>&1
sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0" >> $LOG_FILE 2>&1

# 🏗️ gradlew がなければ生成
if [ ! -f "./gradlew" ]; then
    echo "🛠️ gradlew を生成中..." | tee -a $LOG_FILE
    gradle wrapper >> $LOG_FILE 2>&1
fi

echo "🚀 Building GoalGPS..." | tee -a $LOG_FILE

# ビルド実行
chmod +x gradlew 2>/dev/null
./gradlew :app:assembleDebug -Dorg.gradle.java.home=$JAVA_HOME --stacktrace >> $LOG_FILE 2>&1

# 結果判定とログ出力
if [ $? -eq 0 ]; then
    echo "✅ SUCCESS! ついに完全決着です！" | tee -a $LOG_FILE
    APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
    cp "$APK" ./GoalGPS_latest.apk
    echo "🎉 GoalGPS_latest.apk をダウンロードしてください！"
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
# ターミナル落ち防止
exit 0
