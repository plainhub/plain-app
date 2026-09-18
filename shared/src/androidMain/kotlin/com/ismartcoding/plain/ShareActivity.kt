package com.ismartcoding.plain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.plain.chat.ShareSendHelper
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cannot_get_content
import com.ismartcoding.plain.i18n.note_saved
import com.ismartcoding.plain.i18n.saved_to_downloads
import com.ismartcoding.plain.i18n.send_failed
import com.ismartcoding.plain.i18n.sent_to_n_targets
import com.ismartcoding.plain.lib.extensions.parcelable
import com.ismartcoding.plain.lib.extensions.parcelableArrayList
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.preferences.SettingsProvider
import com.ismartcoding.plain.ui.models.ShareViewModel
import com.ismartcoding.plain.ui.page.share.ShareSheet
import com.ismartcoding.plain.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Translucent share target: hosts the share sheet over the calling app and
 * finishes back to it after (or instead of) the picked action. Owns the
 * SEND / SEND_MULTIPLE intent filters, so shared content URIs stay readable
 * while this activity is alive.
 */
class ShareActivity : ComponentActivity() {
    private val vm: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!vm.sending) finish()
            }
        })
        parseIntent(intent)
        setContent {
            SettingsProvider {
                AppTheme(useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)) {
                    ShareSheet(
                        vm,
                        onDismiss = { finish() },
                        onSend = { sendAsync() },
                        onOpen = { openFile() },
                        onSaveToFiles = { saveToFilesAsync() },
                        onSaveAsNote = { saveAsNoteAsync() },
                        onOpenAsText = { openTextAsFile() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.reset()
        parseIntent(intent)
    }

    private fun parseIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val stream = intent.parcelable(Intent.EXTRA_STREAM) as? Uri
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (stream != null) {
                    vm.load(null, listOf(stream.toString()), caption = sharedText.orEmpty())
                } else if (!sharedText.isNullOrBlank()) {
                    vm.load(sharedText, emptyList())
                } else {
                    finishWithError()
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM).orEmpty().map { it.toString() }
                if (uris.isEmpty()) finishWithError() else vm.load(null, uris)
            }
            else -> finish()
        }
    }

    private fun finishWithError() {
        finishWithErrorToast()
        finish()
    }

    private fun sendAsync() {
        if (vm.sending) return
        val targets = vm.selectedIds.map { ChatTarget.parseId(it) }
        if (targets.isEmpty()) return
        val uris = vm.fileInfos.orEmpty().map { it.uri }
        if (uris.isEmpty() && vm.text.isNullOrBlank()) return
        vm.sending = true
        lifecycleScope.launch {
            try {
                val ok = ShareSendHelper.sendAsync(targets, uris, vm.text, vm.caption.ifBlank { null })
                if (ok) {
                    toast(LocaleHelper.getPluralStringAsync(Res.plurals.sent_to_n_targets, targets.size))
                    finish()
                } else {
                    vm.sending = false
                    toast(LocaleHelper.getString(Res.string.send_failed))
                }
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
                vm.sending = false
                toast(LocaleHelper.getString(Res.string.send_failed))
            }
        }
    }

    private fun openFile() {
        val info = vm.fileInfos?.firstOrNull() ?: return
        val uri = Uri.parse(info.uri)
        when {
            info.mimeType.startsWith("audio/") || info.mimeType.startsWith("video/") -> {
                lifecycleScope.launch {
                    val dest = withContext(Dispatchers.IO) {
                        copyToShareOpenCache(uri, info.name)
                    }
                    startActivity(Intent(this@ShareActivity, MainActivity::class.java).apply {
                        action = AppIntents.ACTION_PLAY_MEDIA
                        putExtra(Constants.EXTRA_MEDIA_PATH, dest.absolutePath)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    finish()
                }
            }
            info.mimeType.startsWith("image/") -> {
                lifecycleScope.launch {
                    val dest = withContext(Dispatchers.IO) {
                        copyToShareOpenCache(uri, info.name)
                    }
                    startActivity(Intent(this@ShareActivity, MainActivity::class.java).apply {
                        putExtra(IntentExtras.SHARE_IMAGE_PATH, dest.absolutePath)
                        putExtra(IntentExtras.SHARE_IMAGE_NAME, info.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    finish()
                }
            }
            else -> openInMain(uri, info.mimeType)
        }
    }

    private fun saveToFilesAsync() {
        val infos = vm.fileInfos ?: return
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                infos.count { info ->
                    runCatching {
                        FileHelper.copyFileToDownloads(this@ShareActivity, Uri.parse(info.uri)).isNotEmpty()
                    }.getOrDefault(false)
                }
            }
            if (saved > 0) {
                toast(LocaleHelper.getString(Res.string.saved_to_downloads))
                finish()
            } else {
                toast(LocaleHelper.getString(Res.string.cannot_get_content))
            }
        }
    }

    private fun saveAsNoteAsync() {
        val text = vm.text ?: return
        lifecycleScope.launch {
            NoteHelper.addOrUpdateAsync("") {
                title = text.lineSequence().firstOrNull()?.take(50).orEmpty()
                content = text
            }
            toast(LocaleHelper.getString(Res.string.note_saved))
            finish()
        }
    }

    private fun openTextAsFile() {
        val text = vm.text ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val f = shareOpenFile("shared_text_${System.currentTimeMillis()}.txt")
            f.writeText(text)
            withContext(Dispatchers.Main) { openInMain(Uri.fromFile(f), "text/plain") }
        }
    }

    private fun openInMain(uri: Uri, mimeType: String) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }

    private fun shareOpenFile(name: String): File =
        File(File(cacheDir, "share_open").apply { mkdirs() }, name.replace('/', '_'))

    private suspend fun copyToShareOpenCache(uri: Uri, name: String): File {
        val f = shareOpenFile(name.ifBlank { "file_${System.currentTimeMillis()}" })
        FileHelper.copyFile(this, uri, f.absolutePath)
        return f
    }

    private fun finishWithErrorToast() {
        toast(LocaleHelper.getString(Res.string.cannot_get_content))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
