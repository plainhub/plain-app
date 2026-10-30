package com.ismartcoding.plain.ui.apps.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.platform.system.PackageType
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.models.Package as AppPackage
import com.ismartcoding.plain.ui.theme.listItemDescription
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PackageListItem(
    item: AppPackage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    onLongClick: (() -> Unit)? = null,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val icon = if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        com.ismartcoding.plain.ui.navigation.rememberContentResource(source, "pkgicon://${item.id}").value?.getOrNull()
    } else item
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
            AsyncImage(
                modifier =
                Modifier
                    .padding(end = 16.dp)
                    .size(48.dp),
                model = icon,
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
                    text = stringResource(if (item.type == PackageType.SYSTEM) Res.string.app_type_system else Res.string.app_type_user) + " " + item.size.formatBytes(),
                    style = MaterialTheme.typography.listItemDescription(),
                )
                VerticalSpace(dp = 8.dp)
                Text(
                    text = stringResource(Res.string.updated_at) + "  " + item.updatedAt.formatDateTime(),
                    style = MaterialTheme.typography.listItemSubtitle()
                )
            }
        }
    }
}
