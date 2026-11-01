package com.ismartcoding.plain.ui.files.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.ismartcoding.plain.i18n.expanded
import com.ismartcoding.plain.i18n.collapsed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.system_and_apps
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_right
import com.ismartcoding.plain.ui.resources.expand_more
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SystemFilesCard(count: Int, expanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val expansionState = stringResource(if (expanded) Res.string.expanded else Res.string.collapsed)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().semantics { stateDescription = expansionState }.clickable(role = Role.Button, onClick = onToggle).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(if (expanded) UiRes.drawable.expand_more else UiRes.drawable.chevron_right), contentDescription = null)
                Text("${stringResource(Res.string.system_and_apps)} ($count)")
            }
            content()
        }
    }
}
