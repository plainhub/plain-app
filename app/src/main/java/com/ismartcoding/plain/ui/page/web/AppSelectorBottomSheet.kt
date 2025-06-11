package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ismartcoding.plain.R
import com.ismartcoding.plain.packageManager
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NotificationSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorBottomSheet(
    vm: NotificationSettingsViewModel,
    onDismiss: () -> Unit,
    onAppsSelected: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val allAppsState by vm.allAppsFlow.collectAsState()
    val searchQuery by vm.searchQuery
    val filteredApps = remember(allAppsState, searchQuery) {
        if (searchQuery.isBlank()) {
            allAppsState
        } else {
            allAppsState.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            vm.loadAllAppsAsync(context)
        }
    }

    PModalBottomSheet(
        onDismissRequest = {
            vm.clearSelectedApps()
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column {
            PBottomSheetTopAppBar(
                titleContent = {
                    Text(
                        text = stringResource(R.string.add_app),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    PTopRightButton(
                        stringResource(R.string.add) +
                                if (vm.selectedAppIds.isNotEmpty()) " (${vm.selectedAppIds.size})" else "", click = {
                            if (vm.selectedAppIds.isNotEmpty()) {
                                onAppsSelected(vm.selectedAppIds.toList())
                                vm.clearSelectedApps()
                                onDismiss()
                            }
                        }, enabled = vm.selectedAppIds.isNotEmpty()
                    )
                }
            )

            OutlinedTextField(
                value = vm.searchQuery.value,
                onValueChange = { vm.searchQuery.value = it },
                label = { Text(stringResource(R.string.search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            if (!vm.appsLoaded.value) {
                NoDataColumn(loading = true)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredApps.filter { !vm.filterData.value.apps.contains(it.id) }, key = { it.id }) { app ->
                        AppSelectorItem(
                            app = app,
                            isSelected = vm.selectedAppIds.contains(app.id),
                            onToggleSelection = { vm.toggleAppSelection(app.id) }
                        )
                    }
                    item {
                        BottomSpace()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectorItem(
    app: com.ismartcoding.plain.data.DPackage,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    val appIcon = remember(app.id) {
        packageManager.getApplicationIcon(app.appInfo)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = appIcon,
            contentDescription = app.name,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholder = rememberDrawablePainter(appIcon)
        )

        HorizontalSpace(dp = 16.dp)

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            VerticalSpace(dp = 2.dp)
            Text(
                text = app.id,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalSpace(dp = 12.dp)

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() }
        )
    }
} 