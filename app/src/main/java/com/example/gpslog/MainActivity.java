<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F5F5F5">

    <TextView
        android:id="@+id/tvStatusBanner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="オフライン"
        android:textColor="#FFFFFF"
        android:padding="12dp"
        android:gravity="center"
        android:background="#9E9E9E" />

    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="16dp"
        android:background="#FFFFFF">

        <Button
            android:id="@+id/btnRegister"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="地点登録"
            android:layout_alignParentLeft="true"
            android:layout_centerVertical="true"
            android:backgroundTint="#2196F3" />

        <TextView
            android:id="@+id/tvVersion"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Ver: 1.0.0"
            android:textColor="#888888"
            android:textSize="12sp"
            android:layout_alignParentRight="true" />

        <Switch
            android:id="@+id/switchRecord"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Online "
            android:layout_alignParentRight="true"
            android:layout_below="@id/tvVersion"
            android:layout_marginTop="8dp"/>
    </RelativeLayout>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#FFF9C4"
        android:padding="16dp">
        
        <TextView
            android:id="@+id/tvRawLog"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="#000000"
            android:textSize="16sp"
            android:text="ここに生のデータが出力されます..." />
    </ScrollView>

</LinearLayout>
