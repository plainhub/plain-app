package com.ismartcoding.plain.ui.page.search

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.GlobalSearchDomain
import com.ismartcoding.plain.ui.models.GlobalSearchHit
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.searchHighlight
import org.jetbrains.compose.resources.stringResource

/** Marks every case-insensitive occurrence of [query] in [text] bold on a highlight background. */
fun highlightQuery(text: String, query: String, highlight: androidx.compose.ui.graphics.Color): AnnotatedString {
    if (text.isEmpty() || query.isEmpty()) return AnnotatedString(text)
    val lower = text.lowercase()
    val ql = query.lowercase()
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val hit = lower.indexOf(ql, i)
            if (hit < 0) {
                append(text.substring(i))
                break
            }
            append(text.substring(i, hit))
            val end = hit + ql.length
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, background = highlight),
                this.length,
                this.length + (end - hit),
            )
            append(text.substring(hit, end))
            i = end
        }
    }
}

/** Sticky scope selector: All + every domain when idle, All + hit domains while searching. */
@Composable
fun GlobalSearchScopeChips(
    selected: GlobalSearchDomain?,
    chips: List<GlobalSearchDomain?>,
    onSelect: (GlobalSearchDomain?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            PFilterChip(
                selected = selected == chip,
                onClick = { onSelect(chip) },
                label = {
                    if (chip == null) {
                        Text(stringResource(Res.string.all))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PIcon(icon = chip.iconRes, contentDescription = stringResource(chip.labelRes), modifier = Modifier.size(16.dp))
                            HorizontalSpace(6.dp)
                            Text(stringResource(chip.labelRes))
                        }
                    }
                },
            )
        }
    }
}

/** Recent search terms as tappable chips with an individual delete button. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchRecentSection(
    recent: List<String>,
    onSearch: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recent.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.recent_searches),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        PTextButton(text = stringResource(Res.string.clear), onClick = onClear)
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recent.forEach { term ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.cardBackgroundNormal)
                    .clickable { onSearch(term) }
                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PIcon(icon = Res.drawable.history, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalSpace(6.dp)
                Text(text = term, style = MaterialTheme.typography.labelLarge)
                HorizontalSpace(2.dp)
                PIcon(
                    icon = Res.drawable.close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onRemove(term) }
                        .padding(6.dp),
                )
            }
        }
    }
}

/** A single search result row: thumbnail/icon, highlighted title, optional snippet and subtitle. */
@Composable
fun GlobalSearchHitRow(
    hit: GlobalSearchHit,
    query: String,
    onOpen: (GlobalSearchHit) -> Unit,
) {
    val highlight = MaterialTheme.colorScheme.searchHighlight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onOpen(hit) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = if (hit.snippet.isEmpty()) Alignment.CenterVertically else Alignment.Top,
    ) {
        GlobalSearchThumb(hit)
        HorizontalSpace(12.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
        ) {
            if (hit.snippet.isEmpty()) {
                Text(
                    text = remember(hit.key, query) { highlightQuery(hit.title, query, highlight) },
                    style = MaterialTheme.typography.listItemTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hit.subtitle.isNotEmpty()) {
                    VerticalSpace(2.dp)
                    Text(
                        text = hit.subtitle,
                        style = MaterialTheme.typography.listItemSubtitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = remember(hit.key, query) { highlightQuery(hit.title, query, highlight) },
                        style = MaterialTheme.typography.listItemTitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (hit.subtitle.isNotEmpty()) {
                        HorizontalSpace(8.dp)
                        Text(
                            text = hit.subtitle,
                            style = MaterialTheme.typography.listItemSubtitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                VerticalSpace(2.dp)
                Text(
                    text = remember(hit.key + "s", query) { highlightQuery(hit.snippet, query, highlight) },
                    style = MaterialTheme.typography.listItemSubtitle(),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun GlobalSearchThumb(hit: GlobalSearchHit) {
    val shape = if (hit.roundThumb) CircleShape else RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.cardBackgroundNormal),
        contentAlignment = Alignment.Center,
    ) {
        if (hit.thumbUri != null) {
            AsyncImage(
                model = hit.thumbUri,
                contentDescription = hit.title,
                modifier = Modifier.size(40.dp),
            )
        } else {
            PIcon(
                icon = hit.iconRes ?: Res.drawable.search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
