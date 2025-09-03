package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.VPackage
import com.ismartcoding.plain.ui.theme.listItemDescription
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.packageManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PackageListItem(
    item: VPackage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = Color.Unspecified,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = remember(item.id) {
                try {
                    packageManager.getApplicationIcon(item.id)
                } catch (e: Exception) {
                    null
                }
            }
            Image(
                modifier =
                Modifier
                    .padding(end = 16.dp)
                    .size(48.dp),
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = item.name,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = item.name + " (${item.version})",
                    style = MaterialTheme.typography.listItemTitle(),
                )
                VerticalSpace(dp = 8.dp)
                Text(
                    text = item.id,
                    style = MaterialTheme.typography.listItemDescription(),
                )
                VerticalSpace(dp = 8.dp)
                Text(
                    text = stringResource(id = LocaleHelper.getStringIdentifier("app_type_" + item.type)) + " " + item.size.formatBytes(),
                    style = MaterialTheme.typography.listItemDescription(),
                )
                VerticalSpace(dp = 8.dp)
                Text(
                    text = stringResource(id = R.string.updated_at) + "  " + item.updatedAt.formatDateTime(),
                    style = MaterialTheme.typography.listItemSubtitle()
                )
            }
        }
    }
}
