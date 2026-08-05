package com.ismartcoding.plain.ui.page.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.clients_online
import com.ismartcoding.plain.ui.base.StatusIndicator
import com.ismartcoding.plain.ui.theme.greenDot
import com.ismartcoding.plain.ui.theme.greenPill
import com.ismartcoding.plain.ui.theme.greenText
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnlineSessionsIndicator(count: Int, onClick: () -> Unit) {
    StatusIndicator(
        text = stringResource(Res.string.clients_online, count),
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
        pillColor = MaterialTheme.colorScheme.greenPill,
        dotColor = MaterialTheme.colorScheme.greenDot,
        textColor = MaterialTheme.colorScheme.greenText,
    )
}
