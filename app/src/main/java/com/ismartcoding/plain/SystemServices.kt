package com.ismartcoding.plain

import android.app.usage.StorageStatsManager
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.os.storage.StorageManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
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
