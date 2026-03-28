package com.ismartcoding.plain.ui.page.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.listItemTitle

@Composable
internal fun FeatureSelectionCard(
    feature: FeatureItem,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.cardBackgroundNormal
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle,
            )

            HorizontalSpace(8.dp)

            PIcon(
                icon = painterResource(feature.iconRes),
                contentDescription = stringResource(feature.titleRes),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            HorizontalSpace(12.dp)

            Text(
                text = stringResource(feature.titleRes),
                style = MaterialTheme.typography.listItemTitle()
            )
        }
    }
}
