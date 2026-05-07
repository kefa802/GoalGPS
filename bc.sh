cd /workspaces/GoalGPS

cat << 'EOF' > bc.sh
#!/bin/bash
export ANDROID_HOME="/workspaces/GoalGPS/my-android-sdk"
export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.10-ms"
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

echo "🧹 過去のゴミAPKファイルを掃除中..."
rm -f GoalGPS_v*.apk
rm -rf app/build build

echo "🚀 Building GoalGPS..."
./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.java.home=$JAVA_HOME

if [ $? -eq 0 ]; then
    echo "✅ SUCCESS!"
    APK=$(find app/build/outputs/apk/debug/ -name "*.apk" | head -n 1)
    cp "$APK" "./GoalGPS_latest.apk"
    echo "🎉 GoalGPS_latest.apk をダウンロードして上書きインストールしてください！"
else
    echo "❌ BUILD FAILED!"
fi
exit 0
EOF

chmod +x bc.sh
./bc.sh
