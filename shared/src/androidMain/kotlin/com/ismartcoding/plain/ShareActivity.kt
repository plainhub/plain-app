package com.ismartcoding.plain

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.app.AppIntents
import com.ismartcoding.plain.app.IntentExtras
import com.ismartcoding.plain.rust.RustRuntime
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.plain.features.chat.sendPickedAttachments
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.ui.theme.DarkTheme
import com.ismartcoding.plain.features.notes.NotesClient
import com.ismartcoding.plain.features.files.FilesClient
import com.ismartcoding.plain.platform.files.saveFileToDownloads
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cannot_get_content
import com.ismartcoding.plain.i18n.note_saved
import com.ismartcoding.plain.i18n.saved_to_downloads
import com.ismartcoding.plain.i18n.send_failed
import com.ismartcoding.plain.i18n.sent_to_n_targets
import com.ismartcoding.plain.lib.extensions.parcelable
import com.ismartcoding.plain.lib.extensions.parcelableArrayList
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.shares.ShareViewModel
import com.ismartcoding.plain.ui.shares.send.ShareSheet
import com.ismartcoding.plain.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.CancellationException

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
            AppTheme(
                useDarkTheme = DarkTheme.isDarkTheme(Prefs.darkTheme.collectAsStateValue()),
                amoledDarkTheme = Prefs.amoledDarkTheme.collectAsStateValue(),
            ) {
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
                val ok = sendPickedAttachments(targets, uris, vm.text, vm.caption.ifBlank { null })
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
                    try {
                        saveFileToDownloads(info.uri, info.name).isNotEmpty()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        LogCat.e("Save shared content failed", error)
                        false
                    }
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
            NotesClient.save(
                id = "",
                title = text.lineSequence().firstOrNull()?.take(50).orEmpty(),
                content = text,
            )
            toast(LocaleHelper.getString(Res.string.note_saved))
            finish()
        }
    }

    private fun openTextAsFile() {
        val text = vm.text ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val f = shareOpenFile("shared_text_${System.currentTimeMillis()}.txt")
                GraphqlApi.execute(
                    Mutation.saveSharedText(checkNotNull(f.parent), f.path, text),
                )
                withContext(Dispatchers.Main) { openInMain(Uri.fromFile(f), "text/plain") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogCat.e("Prepare shared text failed", error)
                withContext(Dispatchers.Main) { toast(LocaleHelper.getString(Res.string.cannot_get_content)) }
            }
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
        File(RustRuntime.sharedPreviewDir, name.replace('/', '_'))

    private suspend fun copyToShareOpenCache(uri: Uri, name: String): File {
        val f = shareOpenFile(name.ifBlank { "file_${System.currentTimeMillis()}" })
        val saved = FilesClient.copyToDirectory(uri.toString(), checkNotNull(f.parent), f.name)
        check(saved.isNotEmpty()) { "Unable to copy shared content" }
        return File(saved)
    }

    private fun finishWithErrorToast() {
        toast(LocaleHelper.getString(Res.string.cannot_get_content))
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
