<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp">
        <EditText
            android:id="@+id/etLocationName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="地点の名前を入力"/>
        <Button
            android:id="@+id/btnSaveLocation"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="登録"/>
    </LinearLayout>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="地図を長押ししてピンを立ててください"
        android:textAlignment="center"
        android:padding="4dp"
        android:background="#EEEEEE"/>

    <org.osmdroid.views.MapView
        android:id="@+id/mapView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</LinearLayout>
