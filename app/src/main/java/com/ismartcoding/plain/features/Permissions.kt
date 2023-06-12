package com.ismartcoding.plain.features

import android.Manifest
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
import com.ismartcoding.lib.isTIRAMISUPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
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
    NONE;

    fun getText(): String {
        if (this == NONE) {
            return getString(R.string.open_permission_settings)
        }

        return getString("feature_$name")
    }

    fun isEnabled(): Boolean {
        return LocalStorage.apiPermissions.contains(this.toString())
    }

    fun setEnabled(enabled: Boolean) {
        val mutate = LocalStorage.apiPermissions.toMutableSet()
        if (enabled) {
            mutate.add(this.toString())
        } else {
            mutate.remove(this.toString())
        }
        LocalStorage.apiPermissions = mutate
    }

    fun toSysPermission(): String {
        return "android.permission.${this.name}"
    }

    fun can(): Boolean {
        val context = MainApp.instance
        if (this == WRITE_EXTERNAL_STORAGE) {
            return FileHelper.hasStoragePermission(context)
        } else if (this == WRITE_SETTINGS) {
            return Settings.System.canWrite(context)
        }

        if (this == POST_NOTIFICATIONS) {
            return if (isTIRAMISUPlus()) {
                context.hasPermission(this.toSysPermission())
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }

        return context.hasPermission(this.toSysPermission())
    }

    fun grant(): Boolean {
        if (can()) {
            return true
        } else {
            if (this == POST_NOTIFICATIONS) {
                if (isTIRAMISUPlus()) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.instance.get()!!, this.toSysPermission())) {
                        getEnableNotificationIntent()
                    } else {
                        sendEvent(RequestPermissionEvent(this.toSysPermission()))
                    }
                } else {
                    getEnableNotificationIntent()
                }
            } else {
                sendEvent(RequestPermissionEvent(this.toSysPermission()))
            }
        }

        return false
    }

    companion object {
        fun getEnableNotificationIntent(): Intent {
            val context = MainActivity.instance.get()!!
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

    fun check() {
        if (!isEnabled()) {
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
}

object Permissions {
    private val map = mutableMapOf<String, ActivityResultLauncher<String>>()
    private val events = mutableListOf<Job>()
    private lateinit var fileStorageActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var pushNotificationActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var writeSettingsActivityLauncher: ActivityResultLauncher<Intent>

    fun getWebList(context: Context): List<Permission> {
        val permissions = mutableListOf(Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_CONTACTS, Permission.WRITE_CONTACTS)
        if (context.allowSensitivePermissions()) {
            permissions.addAll(listOf(Permission.READ_SMS, Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG))
        }
        permissions.addAll(listOf(Permission.CALL_PHONE, Permission.NONE))

        return permissions
    }

    fun init(activity: AppCompatActivity) {
        setOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS,
        ).forEach { permission ->
            map[permission] = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                canContinue = true
                sendEvent(PermissionResultEvent(permission))
            }
        }

        fileStorageActivityLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            sendEvent(PermissionResultEvent(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }

        pushNotificationActivityLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            canContinue = true
            sendEvent(PermissionResultEvent(Manifest.permission.POST_NOTIFICATIONS))
        }

        writeSettingsActivityLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            sendEvent(PermissionResultEvent(Manifest.permission.WRITE_SETTINGS))
        }
        events.add(receiveEventHandler<RequestPermissionEvent> { event ->
            if (event.permission == Manifest.permission.WRITE_EXTERNAL_STORAGE && isRPlus()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse("package:${MainApp.instance.packageName}")
                    fileStorageActivityLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    fileStorageActivityLauncher.launch(intent)
                }
            } else if (event.permission == Manifest.permission.WRITE_SETTINGS) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:${MainApp.instance.packageName}")
                if (intent.resolveActivity(packageManager) != null) {
                    writeSettingsActivityLauncher.launch(intent)
                } else {
                    DialogHelper.showMessage("ActivityNotFoundException: No Activity found to handle Intent act=android.settings.action.MANAGE_WRITE_SETTINGS")
                }
            } else if (event.permission == Manifest.permission.POST_NOTIFICATIONS) {
                if (isTIRAMISUPlus()) {
                    val context = MainActivity.instance.get()!!
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(context, event.permission)) {
                        pushNotificationActivityLauncher.launch(Permission.getEnableNotificationIntent())
                    } else {
                        map[event.permission]?.launch(event.permission)
                    }
                } else {
                    pushNotificationActivityLauncher.launch(Permission.getEnableNotificationIntent())
                }
            } else {
                map[event.permission]?.launch(event.permission)
            }
        })
    }

    private var canContinue = false

    private suspend fun ensureNotificationAsync(): Boolean {
        val permission = Permission.POST_NOTIFICATIONS
        val ready = isNotificationPermissionReadyWithRequest()
        if (!ready) {
            canContinue = false
            while (true) {
                LogCat.d("waiting for push notification permission accepted or denied")
                if (canContinue) {
                    return permission.can()
                }
                delay(500)
            }
        }

        return true
    }

    private fun isNotificationPermissionReadyWithRequest(): Boolean {
        val permission = Permission.POST_NOTIFICATIONS
        if (!permission.can()) {
            sendEvent(RequestPermissionEvent(permission.toSysPermission()))
            return false
        }

        return true
    }

    fun checkNotification(stringKey: Int, callback: () -> Unit) {
        val permission = Permission.POST_NOTIFICATIONS
        if (permission.can()) {
            callback()
        } else {
            val context = MainActivity.instance.get()!!
            DialogHelper.showConfirmDialog(context, getString(R.string.confirm), getString(stringKey)) {
                coIO {
                    ensureNotificationAsync()
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

