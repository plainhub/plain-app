package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.grey
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeerListItem(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    icon: Int,
    online: Boolean = false,
) {
    Surface(
        modifier =
            modifier,
        color = Color.Unspecified,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp, 8.dp, 8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(1.dp)
                        .clip(CircleShape)
                        .background(
                            if (online)
                                MaterialTheme.colorScheme.green
                            else
                                MaterialTheme.colorScheme.grey
                        )
                        .align(Alignment.BottomEnd)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.listItemTitle(),
                )
                VerticalSpace(dp = 8.dp)
                Text(
                    text = desc,
                    style = MaterialTheme.typography.listItemSubtitle(),
                )
            }
        }
    }
}
