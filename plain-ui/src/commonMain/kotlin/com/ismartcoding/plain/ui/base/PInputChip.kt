package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.close as ui_drawable_close
import com.ismartcoding.plain.ui.resources.delete as ui_string_delete
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** A null primary action makes the whole chip a remove action. */
@Composable
fun PInputChip(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    closeContentDescription: String = "${stringResource(UiRes.string.ui_string_delete)} $text",
    onClose: (() -> Unit)? = null,
) {
    val action = requireNotNull(onClick ?: onClose) { "Input chips require a primary or remove action" }
    val removeOnly = onClick == null
    InputChip(
        selected = false,
        onClick = action,
        label = { Text(text = text) },
        modifier = modifier.semantics { if (removeOnly) contentDescription = closeContentDescription },
        enabled = enabled,
        leadingIcon = if (icon != null) {
            {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else null,
        trailingIcon = if (onClose != null) {
            {
                Box(
                    modifier = if (removeOnly) Modifier.size(20.dp) else Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = enabled, role = Role.Button, onClickLabel = closeContentDescription, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    PIcon(
                        icon = painterResource(UiRes.drawable.ui_drawable_close),
                        contentDescription = if (removeOnly) null else closeContentDescription,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else null,
        colors = InputChipDefaults.inputChipColors().copy(
            containerColor = MaterialTheme.colorScheme.cardBackgroundNormal,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = null,
    )
}
