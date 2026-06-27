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
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import android.os.storage.StorageManager
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.plain.lib.extensions.getSystemServiceCompat
import com.ismartcoding.plain.lib.isSPlus

val contentResolver: ContentResolver by lazy { appContext.contentResolver }

val packageManager: PackageManager by lazy { appContext.packageManager }

val clipboardManager: ClipboardManager by lazy {
    appContext.getSystemServiceCompat(ClipboardManager::class.java)
}

val inputMethodManager: InputMethodManager by lazy {
    appContext.getSystemServiceCompat(InputMethodManager::class.java)
}

val notificationManager: NotificationManagerCompat by lazy {
    NotificationManagerCompat.from(appContext)
}

val powerManager: PowerManager by lazy {
    appContext.getSystemServiceCompat(PowerManager::class.java)
}

val wifiManager: WifiManager by lazy {
    appContext.getSystemServiceCompat(WifiManager::class.java)
}

val connectivityManager: ConnectivityManager by lazy {
    appContext.getSystemServiceCompat(ConnectivityManager::class.java)
}

val mediaProjectionManager: MediaProjectionManager by lazy {
    appContext.getSystemServiceCompat(MediaProjectionManager::class.java)
}

val storageStatsManager: StorageStatsManager by lazy {
    appContext.getSystemServiceCompat(StorageStatsManager::class.java)
}

val storageManager: StorageManager by lazy {
    appContext.getSystemServiceCompat(StorageManager::class.java)
}

val activityManager: ActivityManager by lazy {
    appContext.getSystemServiceCompat(ActivityManager::class.java)
}

val batteryManager: BatteryManager by lazy {
    appContext.getSystemServiceCompat(android.os.BatteryManager::class.java)
}

val subscriptionManager: SubscriptionManager by lazy {
    appContext.getSystemServiceCompat(SubscriptionManager::class.java)
}

val telephonyManager: TelephonyManager by lazy {
    appContext.getSystemServiceCompat(TelephonyManager::class.java)
}

val alarmManager: AlarmManager by lazy {
    appContext.getSystemServiceCompat(AlarmManager::class.java)
}

val audioManager: android.media.AudioManager by lazy {
    appContext.getSystemServiceCompat(android.media.AudioManager::class.java)
}

val uiModeManager: UiModeManager by lazy {
    appContext.getSystemServiceCompat(UiModeManager::class.java)
}

val smsManager: SmsManager by lazy {
    if (isSPlus()) {
        appContext.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
    } else {
        SmsManager.getDefault()
    }
}





