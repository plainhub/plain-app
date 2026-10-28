package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.htmlToPlainText
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CancellationException

import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManagedDocumentPage(navController: NavHostController, device: ManagedDeviceViewModel, route: Routing.ManagedDocument) {
    val vm = viewModel { ManagedDocumentViewModel(device, AppFeatureType.valueOf(route.feature), route.id, route.path) }
    val state by device.state.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(vm) {
        try { vm.load() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: failure.toString() }
    }
    PScaffold(topBar = {
        PTopAppBar(
            title = stringResource(if (vm.feature == AppFeatureType.NOTES) Res.string.notes else if (vm.feature == AppFeatureType.FEEDS) Res.string.feeds else Res.string.files),
            subtitle = state.info?.deviceName.orEmpty(),
            onNavigateBack = { navController.navigateUp() },
            actions = {
                if (vm.feature != AppFeatureType.FEEDS) PTextButton(
                    text = stringResource(Res.string.save), enabled = !vm.loading.value && !saving && error == null,
                    onClick = {
                        saving = true
                        scope.launchSafe(onDone = { saving = false }) { vm.save(); navController.navigateUp() }
                    },
                )
            },
        )
    }) { padding ->
        when {
            error != null -> ManagedError(error!!, Modifier.padding(padding)) {
                scope.launchSafe(onError = { error = it.message ?: it.toString() }) { error = null; vm.load() }
            }
            vm.loading.value -> NoDataColumn(loading = true, modifier = Modifier.fillMaxSize().padding(padding))
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (vm.feature == AppFeatureType.FEEDS) {
                    Text(vm.title.value)
                    Text(vm.content.value.htmlToPlainText())
                } else {
                    if (vm.feature == AppFeatureType.NOTES) PTextField(
                        readOnly = false, value = vm.title.value, onValueChange = { vm.title.value = it }, label = stringResource(Res.string.title),
                    )
                    PTextField(
                        readOnly = false, value = vm.content.value, onValueChange = { vm.content.value = it },
                        label = stringResource(Res.string.managed_content), singleLine = false,
                    )
                }
            }
        }
    }
}
