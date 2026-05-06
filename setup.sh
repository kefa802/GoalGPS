#!/bin/bash

# プロジェクトのディレクトリ構造を作成
mkdir -p app/src/main/java/com/example/gpslog
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p app/src/main/xml

# 1. ビルド設定ファイル (build.gradle)
cat <<EOF > build.gradle
buildscript {
    repositories { google(); mavenCentral() }
    dependencies { classpath "com.android.tools.build:gradle:8.1.0" }
}
allprojects {
    repositories { google(); mavenCentral() }
}
EOF

# 2. アプリ用ビルド設定 (app/build.gradle)
cat <<EOF > app/build.gradle
plugins { id 'com.android.application' }
android {
    namespace 'com.example.gpslog'
    compileSdk 33
    defaultConfig {
        applicationId "com.example.gpslog"
        minSdk 26
        targetSdk 33
        versionCode 1
        versionName "1.0"
    }
}
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
}
EOF

# 3. マニフェスト (権限設定)
cat <<EOF > app/src/main/AndroidManifest.xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <application android:label="GPSログアプリ" android:theme="@style/Theme.AppCompat.Light">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF

# 4. メインプログラム (MainActivity.java)
cat <<EOF > app/src/main/java/com/example/gpslog/MainActivity.java
package com.example.gpslog;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setTextSize(20);
        tv.setPadding(50, 50, 50, 50);
        tv.setText("工藤さん、アプリの土台が完成しました！\n\nGPSログを記録し、登録地点でぶるっと震える機能を実装していきます。");
        setContentView(tv);
    }
}
EOF

# Gradleラッパーの準備（ビルドに必要）
gradle wrapper --gradle-version 8.1
chmod +x gradlew

echo "--- 構築完了！ ---"
echo "次に './gradlew assembleDebug' を実行してください。"
