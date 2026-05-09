#!/bin/bash
echo "📤 Saving current changes to GitHub (Skip Build)..."

# Gitに今の変更をすべて追加
git add .

# コミット（メッセージには現在時刻を付記）
git commit -m "GoalGPS manual update: $(date +'%Y-%m-%d %H:%M:%S')"

# GitHubへ送信
git push origin main

echo "🎉 Done! You can now edit the files directly on GitHub."
