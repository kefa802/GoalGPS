cd /workspaces/GoalGPS

cat << 'EOF' > bc.sh
#!/bin/bash
LOG_FILE="last_build_error.log"
echo "=== Build Started at $(date) ===" > $LOG_FILE

export ANDROID_HOME="/workspaces/GoalGPS/my-android-sdk"
export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# ✅ Gitの影響を100%受けない一時領域（/tmp）の変数を使ってカウントアップ
NUM_FILE="/tmp/goalgps_build_num"
if [ ! -f "$NUM_FILE" ]; then
    echo "1" > "$NUM_FILE"
fi
GOALGPS_BUILD_NUM=$(cat "$NUM_FILE")
GOALGPS_BUILD_NUM=$((GOALGPS_BUILD_NUM + 1))
echo "$GOALGPS_BUILD_NUM" > "$NUM_FILE"

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

echo "🚀 Building GoalGPS (Ver: 1.0.$GOALGPS_BUILD_NUM)..." | tee -a $LOG_FILE

# ✅ Gradleに変数（-PbuildNum）を直接注入してビルド！
./gradlew :app:assembleDebug -PbuildNum=$GOALGPS_BUILD_NUM --no-daemon -Dorg.gradle.java.home=$JAVA_HOME --stacktrace >> $LOG_FILE 2>&1

if [ $? -eq 0 ]; then
    echo "✅ SUCCESS! ビルドが完了しました！" | tee -a $LOG_FILE
    APK_NAME="GoalGPS_v${GOALGPS_BUILD_NUM}.apk"
    APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
    cp "$APK" "./$APK_NAME"
    echo "🎉 $APK_NAME (Ver: 1.0.$GOALGPS_BUILD_NUM) をダウンロードしてください！"
else
    echo "❌ BUILD FAILED! エラーログを確認してください。" | tee -a $LOG_FILE
    grep -A 50 "What went wrong:" $LOG_FILE
fi
exit 0
EOF

chmod +x bc.sh
# 過去の不要な設定ファイルを削除
rm -f version.properties GoalGPS_latest.apk
./bc.sh
