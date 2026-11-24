package com.ismartcoding.plain.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_down
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GlobalSearchDeviceFilter(
    peers: List<GlobalSearchPeer>,
    selected: Set<String>,
    onConfirm: (Set<String>) -> Unit,
) {
    val local = ContentSource.Local.key
    val thisDevice = stringResource(Res.string.this_device)
    val allLabel = stringResource(Res.string.search_all_devices)
    val all = searchDeviceIds(peers)
    val label = when {
        selected == all -> allLabel
        selected.size == 1 -> if (local in selected) thisDevice else peers.first { it.id in selected }.name
        else -> stringResource(Res.string.search_selected_device_count, selected.size)
    }
    var open by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(start = 8.dp).widthIn(max = 144.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape).background(colors.primaryContainer)
            .clickable(role = Role.Button) { open = true }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label, Modifier.weight(1f, fill = false),
            color = colors.primary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        PIcon(
            modifier = Modifier.size(16.dp),
            icon = painterResource(UiRes.drawable.chevron_down),
            contentDescription = null,
            tint = colors.primary,
        )
    }
    if (open) {
        SearchDeviceSheet(peers, selected, all, thisDevice, allLabel, { open = false }) {
            open = false
            onConfirm(it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDeviceSheet(
    peers: List<GlobalSearchPeer>,
    selected: Set<String>,
    all: Set<String>,
    thisDevice: String,
    allLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var draft by remember { mutableStateOf(selected) }
    val checked = draft.intersect(all)
    val allChecked = when {
        checked.isEmpty() -> ToggleableState.Off
        checked == all -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val local = ContentSource.Local.key
    fun toggle(id: String) { draft = if (id in checked) checked - id else checked + id }

    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.search_choose_devices))
        SearchDeviceChoice(allLabel, allChecked, onClick = { draft = if (checked == all) emptySet() else all })
        LazyColumn(Modifier.weight(1f, fill = false)) {
            item { SearchDeviceChoice(thisDevice, ToggleableState(local in checked)) { toggle(local) } }
            items(peers, key = { it.id }) { peer ->
                SearchDeviceChoice(
                    peer.name,
                    ToggleableState(peer.id in checked),
                    subtitle = if (peer.online) "" else stringResource(Res.string.offline),
                ) { toggle(peer.id) }
            }
        }
        PFilledButton(
            text = stringResource(Res.string.ok),
            enabled = checked.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            onClick = { onConfirm(checked) },
        )
    }
}

@Composable
private fun SearchDeviceChoice(
    title: String,
    state: ToggleableState,
    subtitle: String = "",
    onClick: () -> Unit,
) {
    PListItem(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .triStateToggleable(state = state, role = Role.Checkbox, onClick = onClick),
        title = title,
        subtitle = subtitle,
        start = { CheckCircle(state) },
    )
}
