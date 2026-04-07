package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPackage
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.red

@Composable
fun NotificationAppListItem(
    app: DPackage,
    onRemove: () -> Unit,
) {
    Row(
        modifier = PlainTheme.getCardModifier()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val appIcon = remember(app.id) {
            packageManager.getApplicationIcon(app.appInfo)
        }
        AsyncImage(
            model = appIcon,
            contentDescription = app.name,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
        )
        HorizontalSpace(dp = 12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.name, style = MaterialTheme.typography.listItemTitle())
            VerticalSpace(dp = 4.dp)
            Text(text = app.id, style = MaterialTheme.typography.listItemSubtitle())
        }
        TextButton(onClick = onRemove) {
            Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.red)
        }
    }
    VerticalSpace(dp = 8.dp)
}
