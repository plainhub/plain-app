package com.ismartcoding.plain.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.all
import com.ismartcoding.plain.i18n.more
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_right
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val firstTabs = listOf(GlobalSearchDomain.FILES, GlobalSearchDomain.IMAGES, GlobalSearchDomain.VIDEOS)

@Composable
internal fun GlobalSearchTabs(
    selected: GlobalSearchDomain?,
    domains: List<GlobalSearchDomain>,
    onSelect: (GlobalSearchDomain?) -> Unit,
    deviceFilter: (@Composable () -> Unit)?,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val tabs = listOf(null) + firstTabs.filter { it in domains } + domains.filterNot { it in firstTabs }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        deviceFilter?.invoke()
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(scroll).selectableGroup()
                .padding(start = 8.dp, end = 8.dp),
        ) {
            tabs.forEach { domain ->
                val active = selected == domain
                val colors = MaterialTheme.colorScheme
                Box(
                    modifier = Modifier.minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.small)
                        .selectable(selected = active, role = Role.Tab, onClick = { onSelect(domain) })
                        .padding(horizontal = 8.dp)
                        .width(IntrinsicSize.Max),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(domain?.labelRes ?: Res.string.all),
                        modifier = Modifier.padding(vertical = 8.dp),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) colors.primary else colors.onSurfaceVariant,
                    )
                    if (active) {
                        Box(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                                .fillMaxWidth().height(2.dp).background(colors.primary),
                        )
                    }
                }
            }
        }
        if (scroll.maxValue in 1 until Int.MAX_VALUE) {
            Box(Modifier.width(32.dp)) {
                if (scroll.canScrollForward) {
                    PIconButton(
                        icon = UiRes.drawable.chevron_right,
                        modifier = Modifier.scale(scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f, scaleY = 1f),
                        iconSize = 16.dp,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = stringResource(Res.string.more),
                    ) { scope.launch { scroll.animateScrollBy(scroll.viewportSize.toFloat()) } }
                }
            }
        }
    }
}
