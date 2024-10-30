package com.ismartcoding.plain.features

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.hasPermission
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.preference.ApiPermissionsPreference
import com.ismartcoding.plain.services.PNotificationListenerService
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
    READ_MEDIA_IMAGES,
    READ_MEDIA_VIDEOS,
    READ_MEDIA_AUDIO,
    NOTIFICATION_LISTENER,
    READ_PHONE_STATE,
    READ_PHONE_NUMBERS,
    SCHEDULE_EXACT_ALARM,
    NONE
    ;

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

            this == NOTIFICATION_LISTENER -> {
                val componentName = ComponentName(context, PNotificationListenerService::class.java)
                val enabledListeners =
                    Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                return enabledListeners?.contains(componentName.flattenToString()) == true
            }

            else -> context.hasPermission(this.toSysPermission())
        }
    }

    fun grant(context: Context): Boolean {
        if (can(context)) {
            return true
        } else {
            sendEvent(RequestPermissionsEvent(this))
        }

        return false
    }

    companion object {
        fun getEnableNotificationIntent(context: Context): Intent {
            return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
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

    fun request(
        context: Context,
        launcher: ActivityResultLauncher<String>?,
        intentLauncher: ActivityResultLauncher<Intent>?,
    ) {
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
                    DialogHelper.showMessage(
                        "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION",
                    )
                }
            }
        } else if (this == WRITE_SETTINGS) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:${context.packageName}")
            if (intent.resolveActivity(packageManager) != null) {
                intentLauncher?.launch(intent)
            } else {
                DialogHelper.showMessage(
                    "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.action.MANAGE_WRITE_SETTINGS",
                )
            }
        } else if (this == NOTIFICATION_LISTENER) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                intentLauncher?.launch(intent)
            } else {
                DialogHelper.showMessage(
                    "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS",
                )
            }
        } else if (this == SYSTEM_ALERT_WINDOW) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            if (intent.resolveActivity(packageManager) != null) {
                intentLauncher?.launch(intent)
            } else {
                DialogHelper.showMessage(
                    "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.action.ACTION_MANAGE_OVERLAY_PERMISSION",
                )
            }
        } else if (this == POST_NOTIFICATIONS) {
            val permission = this.toSysPermission()
            val activity = MainActivity.instance.get()!!
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) || !isTPlus()) {
                val intent = Permission.getEnableNotificationIntent(context)
                if (intent.resolveActivity(packageManager) != null) {
                    intentLauncher?.launch(intent)
                } else {
                    DialogHelper.showMessage(
                        "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.ACTION_APP_NOTIFICATION_SETTINGS",
                    )
                }
            } else {
                launcher?.launch(permission)
            }
        } else if (this == SCHEDULE_EXACT_ALARM) {
            if (isSPlus()) {
                val intent = Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                if (intent.resolveActivity(packageManager) != null) {
                    intentLauncher?.launch(intent)
                } else {
                    DialogHelper.showMessage(
                        "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM",
                    )
                }
            }
        } else {
            launcher?.launch(this.toSysPermission())
        }
    }
}

data class PermissionItem(val icon: Int?, val permission: Permission, val permissions: Set<Permission>, var granted: Boolean = false) {

    companion object {
        fun create(context: Context, icon: Int?, permission: Permission, permissions: Set<Permission> = setOf(permission)): PermissionItem {
            return PermissionItem(icon, permission, permissions).apply {
                granted = permissions.all { it.can(context) }
            }
        }
    }
}

object Permissions {
    private val launcherMap = mutableMapOf<Permission, ActivityResultLauncher<String>>()
    private val events = mutableListOf<Job>()
    private val intentLauncherMap = mutableMapOf<Permission, ActivityResultLauncher<Intent>>()
    private lateinit var multipleLauncher: ActivityResultLauncher<Array<String>>

    suspend fun checkAsync(context: Context, permissions: Set<Permission>) {
        val apiPermissions = ApiPermissionsPreference.getAsync(context).toMutableSet()
        if (apiPermissions.contains(Permission.WRITE_CONTACTS.toString())) {
            apiPermissions.add(Permission.READ_CONTACTS.toString())
        }
        if (apiPermissions.contains(Permission.WRITE_CALL_LOG.toString())) {
            apiPermissions.add(Permission.READ_CALL_LOG.toString())
        }
        for (item in permissions.map { it.toString() }) {
            if (!apiPermissions.contains(item)) {
                throw Exception("no_permission")
            }
        }
    }

    fun allCan(context: Context, permissions: Set<Permission>): Boolean {
        return permissions.all { it.can(context) }
    }

    fun getWebList(context: Context): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()
        list.add(
            PermissionItem.create(
                context, R.drawable.folder, Permission.WRITE_EXTERNAL_STORAGE
            )
        )
        if (AppFeatureType.NOTIFICATIONS.has()) {
            list.add(
                PermissionItem.create(context, R.drawable.bell, Permission.NOTIFICATION_LISTENER)
            )
        }
        list.add(
            PermissionItem.create(context, R.drawable.book_user, Permission.WRITE_CONTACTS, setOf(Permission.READ_CONTACTS, Permission.WRITE_CONTACTS))
        )

        if (AppFeatureType.SOCIAL.has()) {
            list.add(PermissionItem.create(context, R.drawable.message_square_text, Permission.READ_SMS))
            list.add(PermissionItem.create(context, R.drawable.logs, Permission.WRITE_CALL_LOG, setOf(Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG)))
        }
        list.add(
            PermissionItem.create(context, R.drawable.phone, Permission.CALL_PHONE)
        )
        list.add(
            PermissionItem.create(context, R.drawable.file_digit, Permission.READ_PHONE_NUMBERS, setOf(Permission.READ_PHONE_STATE, Permission.READ_PHONE_NUMBERS))
        )
        list.add(PermissionItem(null, Permission.NONE, setOf(Permission.NONE)))
        return list
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
            Permission.READ_MEDIA_IMAGES,
            Permission.READ_MEDIA_VIDEOS,
            Permission.READ_MEDIA_AUDIO,
            Permission.READ_PHONE_STATE,
            Permission.READ_PHONE_NUMBERS,
            Permission.SCHEDULE_EXACT_ALARM,
        ).forEach { permission ->
            launcherMap[permission] =
                activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    canContinue = true
                    val map = mapOf(permission.toSysPermission() to permission.can(MainApp.instance))
                    sendEvent(PermissionsResultEvent(map))
                }
        }

        setOf(
            Permission.WRITE_SETTINGS,
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.SYSTEM_ALERT_WINDOW,
            Permission.POST_NOTIFICATIONS,
            Permission.NOTIFICATION_LISTENER,
            Permission.SCHEDULE_EXACT_ALARM,
        ).forEach { permission ->
            intentLauncherMap[permission] =
                activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    canContinue = true
                    val map = mapOf(permission.toSysPermission() to permission.can(MainApp.instance))
                    sendEvent(PermissionsResultEvent(map))
                }
        }

        multipleLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            canContinue = true
            sendEvent(PermissionsResultEvent(permissions))
        }

        events.add(
            receiveEventHandler<RequestPermissionsEvent> { event ->
                if (event.permissions.size == 1) {
                    val permission = event.permissions.first()
                    permission.request(MainApp.instance, launcherMap[permission], intentLauncherMap[permission])
                } else {
                    multipleLauncher.launch(event.permissions.map { it.toSysPermission() }.toTypedArray())
                }
            },
        )
    }

    private var canContinue = false

    suspend fun ensureNotificationAsync(context: Context): Boolean {
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
            sendEvent(RequestPermissionsEvent(permission))
            return false
        }

        return true
    }

    fun checkNotification(
        context: Context,
        stringKey: Int,
        callback: () -> Unit,
    ) {
        val permission = Permission.POST_NOTIFICATIONS
        if (permission.can(context)) {
            callback()
        } else {
            DialogHelper.showConfirmDialog(getString(R.string.confirm), getString(stringKey), confirmButton = Pair(getString(R.string.ok)) {
                coIO {
                    ensureNotificationAsync(context)
                    callback()
                }
            })
        }
    }

    fun release() {
        events.forEach {
            it.cancel()
        }
    }
}
