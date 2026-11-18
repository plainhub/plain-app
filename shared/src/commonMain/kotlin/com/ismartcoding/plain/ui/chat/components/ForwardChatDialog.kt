package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.forward_note_optional
import com.ismartcoding.plain.i18n.forward_unconfirmed
import com.ismartcoding.plain.ui.base.PTextField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ForwardChatDialog(
    messageId: String,
    onDismiss: () -> Unit,
    onForward: suspend (List<ChatTarget>, String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val submission = rememberSaveable(messageId, saver = ForwardSubmission.saver) { ForwardSubmission() }
    var note by rememberSaveable(messageId) { mutableStateOf("") }
    ForwardTargetDialog(
        onDismiss = { if (!submission.sending) onDismiss() },
        onTargetsSelected = { targets ->
            val draft = note
            scope.launch {
                if (submission.submit { onForward(targets, draft) }) onDismiss()
            }
        },
        enabled = submission.editable,
        sending = submission.sending,
        beforeConfirm = {
            Column(Modifier.padding(horizontal = 16.dp)) {
                PTextField(
                    readOnly = !submission.editable,
                    value = note,
                    label = stringResource(Res.string.forward_note_optional),
                    singleLine = false,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (submission.unconfirmed) Text(
                    stringResource(Res.string.forward_unconfirmed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

internal class ForwardSubmission private constructor(initial: Phase) {
    constructor() : this(Phase.EDITABLE)

    private var phase by mutableStateOf(initial)
    val editable get() = phase == Phase.EDITABLE
    val sending get() = phase == Phase.SENDING
    val unconfirmed get() = phase == Phase.UNCONFIRMED

    suspend fun submit(send: suspend () -> Unit): Boolean {
        if (!editable) return false
        phase = Phase.SENDING
        try {
            send()
        } catch (error: CancellationException) {
            phase = Phase.UNCONFIRMED
            throw error
        } catch (_: Exception) {
            // A missing response does not prove the batch was not committed.
            phase = Phase.UNCONFIRMED
            return false
        }
        phase = Phase.COMMITTED
        return true
    }

    private enum class Phase { EDITABLE, SENDING, UNCONFIRMED, COMMITTED }

    companion object {
        val saver = Saver<ForwardSubmission, String>(
            save = { it.phase.name },
            restore = {
                val phase = Phase.valueOf(it)
                ForwardSubmission(if (phase == Phase.SENDING) Phase.UNCONFIRMED else phase)
            },
        )
    }
}
