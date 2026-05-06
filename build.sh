#!/bin/bash
# Codespacesが再起動してもSDKを見失わないように環境変数をセット
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

# ビルド実行！
gradle :app:assembleDebug
