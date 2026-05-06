#!/bin/bash
echo "📥 最新のコードをGitHubからお迎えします..."
git pull origin main
chmod +x p.sh bc.sh
echo "✅ 完了！これからビルド(bc.sh)を開始します..."
./bc.sh
