package com.ismartcoding.plain.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.allowSensitivePermissions
import com.ismartcoding.lib.extensions.hasPermission
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.ApiPermissionsPreference
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

enum class Permission {
    WRITE_EXTERNAL_STORAGE,
    READ_SMS,
    SEND_SMS,
    READ_CONTACTS,
    WRITE_CONTACTS,
    READ_CALL_LOG,
    WRITE_CALL_LOG,
    CALL_PHONE,
    POST_NOTIFICATIONS,
    WRITE_SETTINGS,
    CAMERA,
    SYSTEM_ALERT_WINDOW,
    RECORD_AUDIO,
    NONE;

    fun getText(): String {
        if (this == NONE) {
            return getString(R.string.open_permission_settings)
        }

        return getString("feature_$name")
    }

    suspend fun isEnabledAsync(context: Context): Boolean {
        val apiPermissions = ApiPermissionsPreference.getAsync(context)
        return apiPermissions.contains(this.toString())
    }

    fun toSysPermission(): String {
        return "android.permission.${this.name}"
    }

    fun can(context: Context): Boolean {
        return when {
            this == WRITE_EXTERNAL_STORAGE -> {
                FileHelper.hasStoragePermission(context)
            }

            this == WRITE_SETTINGS -> {
                Settings.System.canWrite(context)
            }

            this == POST_NOTIFICATIONS -> {
                if (isTPlus()) {
                    context.hasPermission(this.toSysPermission())
                } else {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
            }

            this == SYSTEM_ALERT_WINDOW -> {
                Settings.canDrawOverlays(context)
            }

            else -> context.hasPermission(this.toSysPermission())
        }
    }

    fun grant(context: Context): Boolean {
        if (can(context)) {
            return true
        } else {
            if (this == POST_NOTIFICATIONS) {
                if (isTPlus()) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.instance.get()!!, this.toSysPermission())) {
                        getEnableNotificationIntent(context)
                    } else {
                        sendEvent(RequestPermissionEvent(this))
                    }
                } else {
                    getEnableNotificationIntent(context)
                }
            } else {
                sendEvent(RequestPermissionEvent(this))
            }
        }

        return false
    }

    companion object {
        fun getEnableNotificationIntent(context: Context): Intent {
            val intent = Intent()
            try {
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.applicationInfo.uid)
                intent.putExtra("app_package", context.packageName)
                intent.putExtra("app_uid", context.applicationInfo.uid)
            } catch (e: Exception) {
                e.printStackTrace()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", context.packageName, null)
            }
            return intent
        }
    }

    suspend fun checkAsync(context: Context) {
        if (!isEnabledAsync(context)) {
            throw Exception("no_permission")
        }
    }

    fun getGrantAccessText(): String {
        return when {
            this == READ_SMS -> {
                getString(R.string.need_sms_permission)
            }

            this == READ_CALL_LOG -> {
                getString(R.string.need_call_permission)
            }

            this == READ_CONTACTS -> {
                getString(R.string.need_contact_permission)
            }

            this == WRITE_EXTERNAL_STORAGE -> {
                getString(R.string.need_storage_permission)
            }

            else -> ""
        }
    }

    fun request(context: Context, launcher: ActivityResultLauncher<String>?, intentLauncher: ActivityResultLauncher<Intent>?) {
        if (this == WRITE_EXTERNAL_STORAGE && isRPlus()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${context.packageName}")
                intentLauncher?.launch(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                if (intent.resolveActivity(packageManager) != null) {
                    intentLauncher?.launch(intent)
                } else {
                    DialogHelper.showMessage("ActivityNotFoundException: No Activity found to handle Intent act=android.settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION")
                }
            }
        } else if (this == WRITE_SETTINGS) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:${context.packageName}")
            if (intent.resolveActivity(packageManager) != null) {
                intentLauncher?.launch(intent)
            } else {
                DialogHelper.showMessage("ActivityNotFoundException: No Activity found to handle Intent act=android.settings.action.MANAGE_WRITE_SETTINGS")
            }
        } else if (this == SYSTEM_ALERT_WINDOW) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            if (intent.resolveActivity(packageManager) != null) {
                intentLauncher?.launch(intent)
            } else {
                DialogHelper.showMessage("ActivityNotFoundException: No Activity found to handle Intent act=android.settings.action.ACTION_MANAGE_OVERLAY_PERMISSION")
            }
        } else if (this == POST_NOTIFICATIONS) {
            val permission = this.toSysPermission()
            if (isTPlus()) {
                val activity = MainActivity.instance.get()!!
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    intentLauncher?.launch(Permission.getEnableNotificationIntent(context))
                } else {
                    launcher?.launch(permission)
                }
            } else {
                intentLauncher?.launch(Permission.getEnableNotificationIntent(context))
            }
        } else {
            launcher?.launch(this.toSysPermission())
        }
    }
}

data class PermissionItem(val permission: Permission, val granted: Boolean)

object Permissions {
    private val map = mutableMapOf<Permission, ActivityResultLauncher<String>>()
    private val events = mutableListOf<Job>()
    private val intentLauncherMap = mutableMapOf<Permission, ActivityResultLauncher<Intent>>()

    fun getWebList(context: Context): List<PermissionItem> {
        val permissions = mutableListOf(Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_CONTACTS, Permission.WRITE_CONTACTS)
        if (context.allowSensitivePermissions()) {
            permissions.addAll(listOf(Permission.READ_SMS, Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG))
        }
        permissions.addAll(listOf(Permission.CALL_PHONE, Permission.SYSTEM_ALERT_WINDOW, Permission.NONE))

        return permissions.map { PermissionItem(it, it.can(context)) }
    }

    fun init(activity: AppCompatActivity) {
        setOf(
            Permission.CAMERA,
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.CALL_PHONE,
            Permission.WRITE_SETTINGS,
            Permission.READ_CALL_LOG,
            Permission.WRITE_CALL_LOG,
            Permission.READ_CONTACTS,
            Permission.WRITE_CONTACTS,
            Permission.READ_SMS,
            Permission.SEND_SMS,
            Permission.POST_NOTIFICATIONS,
            Permission.RECORD_AUDIO,
        ).forEach { permission ->
            map[permission] = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                canContinue = true
                sendEvent(PermissionResultEvent(permission))
            }
        }

        setOf(
            Permission.WRITE_SETTINGS, Permission.WRITE_EXTERNAL_STORAGE,
            Permission.SYSTEM_ALERT_WINDOW, Permission.POST_NOTIFICATIONS
        ).forEach { permission ->
            intentLauncherMap[permission] = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                canContinue = true
                sendEvent(PermissionResultEvent(permission))
            }
        }

        events.add(receiveEventHandler<RequestPermissionEvent> { event ->
            event.permission.request(MainApp.instance, map[event.permission], intentLauncherMap[event.permission])
        })
    }

    private var canContinue = false

    private suspend fun ensureNotificationAsync(context: Context): Boolean {
        val permission = Permission.POST_NOTIFICATIONS
        val ready = isNotificationPermissionReadyWithRequest(context)
        if (!ready) {
            canContinue = false
            while (true) {
                LogCat.d("waiting for push notification permission accepted or denied")
                if (canContinue) {
                    return permission.can(context)
                }
                delay(500)
            }
        }

        return true
    }

    private fun isNotificationPermissionReadyWithRequest(context: Context): Boolean {
        val permission = Permission.POST_NOTIFICATIONS
        if (!permission.can(context)) {
            sendEvent(RequestPermissionEvent(permission))
            return false
        }

        return true
    }

    fun checkNotification(context: Context, stringKey: Int, callback: () -> Unit) {
        val permission = Permission.POST_NOTIFICATIONS
        if (permission.can(context)) {
            callback()
        } else {
            DialogHelper.showConfirmDialog(context, getString(R.string.confirm), getString(stringKey)) {
                coIO {
                    ensureNotificationAsync(context)
                    callback()
                }
            }
        }
    }

    fun release() {
        events.forEach {
            it.cancel()
        }
    }
}

