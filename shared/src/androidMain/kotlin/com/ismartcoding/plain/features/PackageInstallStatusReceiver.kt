package com.ismartcoding.plain.features

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.ismartcoding.plain.lib.extensions.parcelable
import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * Receives PackageInstaller session status for bundle installs. The only
 * status that requires action is PENDING_USER_ACTION: without starting the
 * confirmation intent the system never shows the install dialog.
 */
class PackageInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = intent.parcelable<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> LogCat.d("Bundle APK install succeeded")
            else -> LogCat.e(
                "Bundle APK install failed: ${
                    intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                }",
            )
        }
    }
}
