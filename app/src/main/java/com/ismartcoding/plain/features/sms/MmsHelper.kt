package com.ismartcoding.plain.features.sms

import android.content.Intent
import android.provider.Telephony
import androidx.core.content.FileProvider
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.ui.MainActivity
import java.io.File

/**
 * Helper for sending MMS by launching the default SMS/MMS app via an Intent.
 *
 * Since only the default SMS app can send MMS on Android, we construct an Intent
 * with the pre-filled recipient, body, and attachments, then hand it off to the
 * user's default messaging app for confirmation and sending.
 */
object MmsHelper {

    /**
     * Launch the default SMS app with pre-filled recipient, body, and attachment(s).
     * The user confirms and sends from the phone.
     *
     * @param to          Destination phone number
     * @param body        Optional text body
     * @param attachments List of (filePath, mimeType) pairs; files must exist on device
     */
    fun launchDefaultSmsApp(
        to: String,
        body: String,
        attachments: List<Pair<String, String>>, // (filePath, mimeType)
    ) {
        val context = MainApp.instance
        val activity = MainActivity.instance.get()
            ?: throw IllegalStateException("Activity not available")

        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)

        val attachmentUris = attachments.map { (filePath, _) ->
            FileProvider.getUriForFile(context, Constants.AUTHORITY, File(filePath))
        }

        val mimeType = attachments.firstOrNull()?.second ?: "*/*"

        val intent = if (attachmentUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, attachmentUris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachmentUris))
            }
        }

        intent.apply {
            `package` = defaultSmsPackage
            putExtra("address", to)
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Grant URI read access to the default SMS app
        for (uri in attachmentUris) {
            context.grantUriPermission(defaultSmsPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(intent)
    }
}
