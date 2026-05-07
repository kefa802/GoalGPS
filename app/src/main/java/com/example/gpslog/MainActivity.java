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
        <Switch
            android:id="@+id/switchRecord"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Online "
            android:layout_alignParentRight="true" />
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            <Button android:id="@+id/btnStart" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="スタート" android:layout_marginRight="4dp"/>
            <Button android:id="@+id/btnStop" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="ストップ" android:layout_marginRight="4dp"/>
            <Button android:id="@+id/btnRegister" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="地点登録" />
        </LinearLayout>
    </RelativeLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:padding="8dp">
        <TextView android:id="@+id/tvDate" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="2026/05/07" android:textSize="18sp" android:textStyle="bold" />
    </LinearLayout>

    <RadioGroup
        android:id="@+id/rgUnit"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_gravity="right"
        android:layout_marginRight="16dp">
        <RadioButton android:id="@+id/rbMin" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="分" android:checked="true" />
        <RadioButton android:id="@+id/rbHour" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="時" />
    </RadioGroup>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#E0E0E0"
        android:padding="8dp">
        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="2" android:text="登録地点" android:gravity="center" />
        <TextView android:id="@+id/tvHeaderIn" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="IN時間(分)" android:gravity="center" />
        <TextView android:id="@+id/tvHeaderOut" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="OUT時間(分)" android:gravity="center" />
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvDashboardLogs"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</LinearLayout>
