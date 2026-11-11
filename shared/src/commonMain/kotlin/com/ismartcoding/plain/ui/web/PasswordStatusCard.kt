package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ismartcoding.plain.features.settings.PasswordType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.theme.greenText
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.circle_check as ui_drawable_circle_check
import com.ismartcoding.plain.ui.resources.lock as ui_drawable_lock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PasswordStatusCard(
    state: PasswordSettingsState,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val settings = state.settings
    if (settings == null) {
        PAlert(
            description = stringResource(if (state.busy) Res.string.password_status_loading else Res.string.password_status_unavailable),
            type = if (state.busy) AlertType.INFO else AlertType.ERROR,
            actions = if (state.busy) null else ({ POutlinedButton(stringResource(Res.string.retry), onClick = onRetry) }),
        )
        return
    }
    val configured = settings.passwordConfigured
    val enabled = settings.passwordType != PasswordType.NONE
    val loginLabel = stringResource(Res.string.use_password_login)
    PCard {
        PListItem(
            title = loginLabel,
            subtitle = stringResource(Res.string.password_login_purpose),
        ) {
            Switch(
                modifier = Modifier.semantics { contentDescription = loginLabel },
                checked = enabled, onCheckedChange = onToggle, enabled = !state.busy,
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val statusColor = if (configured) MaterialTheme.colorScheme.greenText else MaterialTheme.colorScheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painterResource(if (configured) UiRes.drawable.ui_drawable_circle_check else UiRes.drawable.ui_drawable_lock),
                    contentDescription = null, tint = statusColor,
                )
                Text(
                    text = stringResource(if (configured) Res.string.password_saved else Res.string.password_not_set),
                    color = statusColor, style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(when {
                    !configured -> Res.string.password_not_set_description
                    !enabled -> Res.string.password_saved_disabled_description
                    else -> Res.string.password_write_only_hint
                }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (configured) {
                POutlinedButton(
                    text = stringResource(Res.string.change_login_password), onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), enabled = !state.busy,
                )
            } else {
                PFilledButton(
                    text = stringResource(Res.string.set_login_password), onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), enabled = !state.busy,
                )
            }
        }
    }
}
