#!/bin/bash
export ANDROID_HOME=/workspaces/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "🚀 Build & Commit (GoalGPS)..."
gradle :app:assembleDebug -Dorg.gradle.java.home=$JAVA_HOME

if [ $? -eq 0 ]; then
    echo "✅ Success! Saving to GitHub..."
    git add .
    git commit -m "GoalGPS update: v1.0.1 - $(date +'%Y-%m-%d %H:%M:%S')"
    git push origin main
    
    # 一番手前にアプリをコピーします
    cp app/build/outputs/apk/debug/app-debug.apk ./GoalGPS_latest.apk
    echo "🎉 Done! [GoalGPS_latest.apk] をファイル一覧の直下からダウンロードしてください！"
else
    echo "❌ Build Failed. Check the errors above."
fi
