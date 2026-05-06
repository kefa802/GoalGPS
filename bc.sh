#!/bin/bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

echo "🚀 Build & Commit (GoalGPS)..."
gradle :app:assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Success! Saving to GitHub..."
    git add .
    git commit -m "GoalGPS update: $(date +'%Y-%m-%d %H:%M:%S')"
    git push origin main
    echo "🎉 Done! APK is ready."
else
    echo "❌ Build Failed. Check the errors above."
fi
