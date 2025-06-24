package com.ismartcoding.plain

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.UiModeManager
import android.app.usage.StorageStatsManager
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import android.os.storage.StorageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.lib.extensions.getSystemServiceCompat

val contentResolver: ContentResolver by lazy { MainApp.instance.contentResolver }

val packageManager: PackageManager by lazy { MainApp.instance.packageManager }

val clipboardManager: ClipboardManager by lazy {
    MainApp.instance.getSystemServiceCompat(ClipboardManager::class.java)
}

val inputMethodManager: InputMethodManager by lazy {
    MainApp.instance.getSystemServiceCompat(InputMethodManager::class.java)
}

val notificationManager: NotificationManagerCompat by lazy {
    NotificationManagerCompat.from(MainApp.instance)
}

val nsdManager: NsdManager by lazy {
    MainApp.instance.getSystemServiceCompat(NsdManager::class.java)
}

val powerManager: PowerManager by lazy {
    MainApp.instance.getSystemServiceCompat(PowerManager::class.java)
}

val storageManager: StorageManager by lazy {
    MainApp.instance.getSystemServiceCompat(StorageManager::class.java)
}

val wifiManager: WifiManager by lazy {
    MainApp.instance.getSystemServiceCompat(WifiManager::class.java)
}

val connectivityManager: ConnectivityManager by lazy {
    MainApp.instance.getSystemServiceCompat(ConnectivityManager::class.java)
}

val mediaProjectionManager: MediaProjectionManager by lazy {
    MainApp.instance.getSystemServiceCompat(MediaProjectionManager::class.java)
}

val storageStatsManager: StorageStatsManager by lazy {
    MainApp.instance.getSystemServiceCompat(StorageStatsManager::class.java)
}

val activityManager: ActivityManager by lazy {
    MainApp.instance.getSystemServiceCompat(ActivityManager::class.java)
}

val batteryManager: BatteryManager by lazy {
    MainApp.instance.getSystemServiceCompat(android.os.BatteryManager::class.java)
}

val subscriptionManager: SubscriptionManager by lazy {
    MainApp.instance.getSystemServiceCompat(SubscriptionManager::class.java)
}

val telephonyManager: TelephonyManager by lazy {
    MainApp.instance.getSystemServiceCompat(TelephonyManager::class.java)
}

val alarmManager: AlarmManager by lazy {
    MainApp.instance.getSystemServiceCompat(AlarmManager::class.java)
}

val audioManager: android.media.AudioManager by lazy {
    MainApp.instance.getSystemServiceCompat(android.media.AudioManager::class.java)
}

val uiModeManager: UiModeManager by lazy {
    MainApp.instance.getSystemServiceCompat(UiModeManager::class.java)
}




