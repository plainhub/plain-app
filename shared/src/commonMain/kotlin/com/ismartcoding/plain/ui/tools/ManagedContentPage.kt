package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.models.File as FileEntry
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.openFileExternal
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.more_horiz
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManagedContentPage(navController: NavHostController, device: ManagedDeviceViewModel, route: Routing.ManagedContent) {
    val type = remember(route.feature) { AppFeatureType.valueOf(route.feature) }
    val vm = viewModel { ManagedContentViewModel(device, type, route.path, route.feedId) }
    val state by vm.state.collectAsState()
    val deviceState by device.state.collectAsState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<IData?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val feature = managedFeatureItems(deviceState.info?.capabilities.orEmpty()) {}.find { it.type == type }
    LaunchedEffect(vm) { vm.load() }

    fun open(item: IData) {
        when {
            item is FileEntry && item.isDir -> navController.navigate(route.copy(path = item.path))
            item is Note -> navController.navigate(Routing.ManagedDocument(device.peerId, device.publicKey, AppFeatureType.NOTES.name, id = item.id))
            item is Feed -> navController.navigate(route.copy(feedId = item.id))
            item is FeedEntry -> navController.navigate(Routing.ManagedDocument(device.peerId, device.publicKey, AppFeatureType.FEEDS.name, id = item.id))
            item is Image -> navController.navigate(Routing.ManagedImage(device.peerId, device.publicKey, item.path, managedTitle(item)))
            managedPath(item).isTextFile() -> navController.navigate(Routing.ManagedDocument(device.peerId, device.publicKey, AppFeatureType.FILES.name, path = managedPath(item)))
            else -> {
                busy = true
                scope.launchSafe(onDone = { busy = false }) {
                    openFileExternal(device.client.await().cacheFile(managedPath(item), managedFileName(item)))
                }
            }
        }
    }

    if (showCreate) TextFieldDialog(
        title = stringResource(if (type == AppFeatureType.FEEDS) Res.string.add else Res.string.create_folder),
        readOnly = creating,
        validator = { !creating },
        onDismissRequest = { showCreate = false },
        onConfirm = { value ->
            creating = true
            scope.launchSafe(onDone = { creating = false }) {
                val client = device.client.await()
                if (type == AppFeatureType.FEEDS) client.feeds.create(value.trim(), "")
                else client.createDirectory(route.path, value.trim())
                showCreate = false
                vm.load()
            }
        },
    )

    selected?.let { item ->
        ManagedContentActions(item, device, type, vm.trash.value,
            scope = scope, onBusyChange = { busy = it },
            onDismiss = { selected = null },
            onOpen = { selected = null; open(item) },
            onChanged = { scope.launchSafe { vm.load() } },
        )
    }

    PScaffold(topBar = {
        PTopAppBar(
            title = feature?.let { stringResource(it.titleRes) } ?: stringResource(Res.string.tools),
            subtitle = deviceState.info?.deviceName.orEmpty(),
            onNavigateBack = { navController.navigateUp() },
            actions = {
                PTextButton(text = stringResource(Res.string.refresh), enabled = !state.loading && !busy, onClick = { scope.launchSafe { vm.load() } })
                when {
                    type == AppFeatureType.NOTES -> PTextButton(text = stringResource(Res.string.add), onClick = {
                        navController.navigate(Routing.ManagedDocument(device.peerId, device.publicKey, AppFeatureType.NOTES.name))
                    })
                    type == AppFeatureType.FILES && route.path.isNotEmpty() -> PTextButton(text = stringResource(Res.string.create_folder), onClick = { showCreate = true })
                    type == AppFeatureType.FEEDS && route.feedId.isEmpty() -> PTextButton(text = stringResource(Res.string.add), onClick = { showCreate = true })
                }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (busy) PAlert(description = stringResource(Res.string.managed_loading), type = AlertType.INFO)
            if (!(type == AppFeatureType.FILES && route.path.isEmpty())) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val search = { vm.query = query; scope.launchSafe { vm.load() }; Unit }
                    PTextField(
                        readOnly = false, value = query, onValueChange = { query = it },
                        placeholder = stringResource(Res.string.search), showPasteAction = false,
                        modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                    )
                    PTextButton(text = stringResource(Res.string.search), enabled = !state.loading, onClick = search)
                }
            }
            if (type == AppFeatureType.NOTES) PTextButton(
                text = stringResource(if (vm.trash.value) Res.string.notes else Res.string.trash),
                enabled = !state.loading,
                onClick = { vm.trash.value = !vm.trash.value; scope.launchSafe { vm.load() } },
            )
            when {
                state.error != null -> ManagedError(state.error!!) { scope.launchSafe { vm.load() } }
                state.loading && state.items.isEmpty() && state.roots.isEmpty() -> NoDataColumn(loading = true, modifier = Modifier.fillMaxSize())
                state.items.isEmpty() && state.roots.isEmpty() -> NoDataColumn(modifier = Modifier.fillMaxSize())
                else -> LazyColumn(Modifier.weight(1f)) {
                    items(state.roots, key = { it.path }) { root ->
                        PListItem(title = root.name, subtitle = root.path, modifier = Modifier.combinedClickable(
                            onClick = { navController.navigate(route.copy(path = root.path)) },
                        ))
                    }
                    items(state.items, key = { it.id }) { item ->
                        PListItem(
                            title = { Text(managedTitle(item)) },
                            subtitle = { Text(managedDescription(item), maxLines = 2) },
                            modifier = Modifier.combinedClickable(onClick = { if (!busy) open(item) }, onLongClick = { selected = item }),
                            action = { PIconButton(icon = UiRes.drawable.more_horiz, contentDescription = stringResource(Res.string.more), click = { selected = item }) },
                        )
                    }
                    if (state.hasMore) item {
                        PTextButton(text = stringResource(Res.string.more), enabled = !state.loading, onClick = { scope.launchSafe { vm.load(more = true) } })
                    }
                    item { VerticalSpace(16.dp) }
                }
            }
        }
    }
}
