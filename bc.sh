cd /workspaces/GoalGPS

# 1. 確実にJava 17をインストール（すでに入っていれば数秒で終わります）
sudo apt-get update && sudo apt-get install -y openjdk-17-jdk

# 2. bc.sh を「Java 17絶対使うマン」に書き換え
cat << 'EOF' > bc.sh
#!/bin/bash
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

# Java 17を強制的に使用する設定
export JAVA_HOME=$(find /usr/lib/jvm -name "java-17*" -type d | head -n 1)
export PATH=$JAVA_HOME/bin:$PATH

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
EOF

# 3. 再び実行権限を付与
chmod +x bc.sh
