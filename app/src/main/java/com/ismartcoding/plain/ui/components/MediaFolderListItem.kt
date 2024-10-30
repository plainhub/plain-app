package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundActive
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@Composable
fun MediaFolderListItem(
    folder: DMediaBucket,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.cardBackgroundActive
                else MaterialTheme.colorScheme.cardBackgroundNormal
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.folder),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            HorizontalSpace(16.dp)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.listItemTitle(),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )

                VerticalSpace(8.dp)

                var subtitle = pluralStringResource(
                    R.plurals.items,
                    folder.itemCount,
                    folder.itemCount
                )
                if (folder.size > 0) {
                    subtitle += " • " + folder.size.formatBytes()
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.listItemSubtitle()
                )
            }
        }
    }
} 