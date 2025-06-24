package com.ismartcoding.plain.ui.page.root.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preferences.HomeFeaturesPreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeaturesSelectionPage(
    navController: NavHostController,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val featureItems = remember { FeatureItem.getList(navController) }

    // Get selected features from preferences
    val selectedFeatures = remember {
        context.dataStore.dataFlow.map {
            HomeFeaturesPreference.get(it)
        }
    }.collectAsStateValue(initial = HomeFeaturesPreference.default)

    // Create a mutable state to track selected items
    var selectedItems by remember { mutableStateOf(selectedFeatures) }

    // Update state when preferences change
    LaunchedEffect(selectedFeatures) {
        selectedItems = selectedFeatures
    }

    PModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column {
            PBottomSheetTopAppBar(
                titleContent = {
                    Text(
                        text = stringResource(id = R.string.customize_home_features),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(featureItems) { feature ->
                    val featureId = feature.type.name
                    val isSelected = selectedItems.contains(featureId)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val newSelectedItems = selectedItems.toMutableSet()
                                if (isSelected) {
                                    newSelectedItems.remove(featureId)
                                } else {
                                    newSelectedItems.add(featureId)
                                }
                                selectedItems = newSelectedItems

                                scope.launch(Dispatchers.IO) {
                                    HomeFeaturesPreference.putAsync(context, newSelectedItems)
                                }
                            },
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
                                onCheckedChange = { checked ->
                                    val newSelectedItems = selectedItems.toMutableSet()
                                    if (checked) {
                                        newSelectedItems.add(featureId)
                                    } else {
                                        newSelectedItems.remove(featureId)
                                    }
                                    selectedItems = newSelectedItems

                                    scope.launch(Dispatchers.IO) {
                                        HomeFeaturesPreference.putAsync(context, newSelectedItems)
                                    }
                                }
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
                                text = stringResource(
                                    feature.titleRes
                                ),
                                style = MaterialTheme.typography.listItemTitle()
                            )
                        }
                    }
                }
            }
        }
    }
} 