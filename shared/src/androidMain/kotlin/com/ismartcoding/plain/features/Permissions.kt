package com.ismartcoding.plain.features
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.Permission

import org.jetbrains.compose.resources.StringResource
import com.ismartcoding.plain.i18n.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ismartcoding.plain.lib.channel.receiveEventHandler
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.platform.isSPlus
import com.ismartcoding.plain.platform.isTPlus
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.mainActivity
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

fun getPermissionEnableNotificationIntent(context: Context): Intent {
    return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}

fun Permission.request(
    context: Context,
    launcher: ActivityResultLauncher<String>?,
    intentLauncher: ActivityResultLauncher<Intent>?,
) {
    if (this == Permission.WRITE_EXTERNAL_STORAGE && isRPlus()) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intentLauncher?.launch(intent)
        } catch (e: Exception) {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (appDetailsIntent.resolveActivity(packageManager) != null) {
                intentLauncher?.launch(appDetailsIntent)
            } else {
                DialogHelper.showMessage("Cannot open app settings to grant storage access.")
            }
        }
    } else if (this == Permission.NOTIFICATION_LISTENER) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(packageManager) != null) {
            intentLauncher?.launch(intent)
        } else {
            DialogHelper.showMessage(
                "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS",
            )
        }
    } else if (this == Permission.SYSTEM_ALERT_WINDOW) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(packageManager) != null) {
            intentLauncher?.launch(intent)
        } else {
            DialogHelper.showMessage(
                "ActivityNotFoundException: No Activity found to handle Intent act=android.settings.action.ACTION_MANAGE_OVERLAY_PERMISSION",
            )
        }
    } else if (this == Permission.POST_NOTIFICATIONS) {
        val permission = this.toSysPermission()
        val activity = mainActivity
        if (activity != null && (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) || !isTPlus())) {
            val intent = getPermissionEnableNotificationIntent(context)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    } else if (this == Permission.SCHEDULE_EXACT_ALARM) {
        if (isSPlus()) {
            val intent = Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

object Permissions {
    private val launcherMap = mutableMapOf<Permission, ActivityResultLauncher<String>>()
    private val events = mutableListOf<Job>()
    private val intentLauncherMap = mutableMapOf<Permission, ActivityResultLauncher<Intent>>()
    private lateinit var multipleLauncher: ActivityResultLauncher<Array<String>>

    fun init(activity: AppCompatActivity) {
        setOf(
            Permission.CAMERA,
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.CALL_PHONE,
            Permission.READ_CALL_LOG,
            Permission.WRITE_CALL_LOG,
            Permission.READ_CONTACTS,
            Permission.WRITE_CONTACTS,
            Permission.READ_SMS,
            Permission.SEND_SMS,
            Permission.POST_NOTIFICATIONS,
            Permission.NEARBY_WIFI_DEVICES,
            Permission.ACCESS_FINE_LOCATION,
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
                    val map = mapOf(permission.toSysPermission() to permission.isGranted())
                    sendEvent(PermissionsResultEvent(map))
                }
        }

        setOf(
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.SYSTEM_ALERT_WINDOW,
            Permission.POST_NOTIFICATIONS,
            Permission.NOTIFICATION_LISTENER,
            Permission.SCHEDULE_EXACT_ALARM,
        ).forEach { permission ->
            intentLauncherMap[permission] =
                activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    canContinue = true
                    if (permission == Permission.WRITE_EXTERNAL_STORAGE) {
                        coIO {
                            delay(1500)
                            val map = mapOf(permission.toSysPermission() to permission.isGranted())
                            sendEvent(PermissionsResultEvent(map))
                        }
                    } else {
                        val map = mapOf(permission.toSysPermission() to permission.isGranted())
                        sendEvent(PermissionsResultEvent(map))
                    }
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
                    permission.request(appContext, launcherMap[permission], intentLauncherMap[permission])
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
                    return permission.isGranted()
                }
                delay(500)
            }
        }

        return true
    }

    private fun isNotificationPermissionReadyWithRequest(context: Context): Boolean {
        val permission = Permission.POST_NOTIFICATIONS
        if (!permission.isGranted()) {
            sendEvent(RequestPermissionsEvent(permission))
            return false
        }

        return true
    }

    fun checkNotification(
        context: Context,
        stringResource: StringResource,
        callback: () -> Unit,
    ) {
        val permission = Permission.POST_NOTIFICATIONS
        if (permission.isGranted()) {
            callback()
        } else {
            coIO {
                val message = LocaleHelper.getStringAsync(stringResource)
                val okText = LocaleHelper.getStringAsync(Res.string.ok)
                val confirmText = LocaleHelper.getStringAsync(Res.string.confirm)
                DialogHelper.showConfirmDialog(confirmText, message, confirmButton = Pair(okText) {
                    coIO {
                        ensureNotificationAsync(context)
                        callback()
                    }
                })
            }
        }
    }

    fun release() {
        events.forEach {
            it.cancel()
        }
    }
}
