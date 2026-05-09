#!/bin/bash
# GitHubから最新のソースコードを取得するだけのスクリプト

echo "📥 GitHubから最新のコードを取得（同期）します..."

# 強制的にGitHubの状態に合わせる設定（コンフリクト回避）
git fetch origin
git reset --hard origin/main

# 実行権限を念のため付け直す
chmod +x pull.sh build.sh

echo "✅ 同期が完了しました！"
echo "🚀 ビルドするには ./build.sh を実行してください。"
