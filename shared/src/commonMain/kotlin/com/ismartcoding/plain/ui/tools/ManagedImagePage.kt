package com.ismartcoding.plain.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.ismartcoding.plain.platform.files.fileToUriString
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.navigation.Routing
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManagedImagePage(navController: NavHostController, device: ManagedDeviceViewModel, route: Routing.ManagedImage) {
    val state by device.state.collectAsState()
    var cached by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(route.path, retry) {
        try {
            error = null
            cached = device.client.await().cacheFile(route.path, route.name)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: failure.toString() }
    }
    PScaffold(topBar = {
        PTopAppBar(title = route.name, subtitle = state.info?.deviceName.orEmpty(), onNavigateBack = { navController.navigateUp() })
    }) { padding ->
        when {
            error != null -> ManagedError(error!!, Modifier.padding(padding)) { retry++ }
            cached == null -> NoDataColumn(loading = true, modifier = Modifier.fillMaxSize().padding(padding))
            else -> AsyncImage(
                model = fileToUriString(cached!!), contentDescription = route.name,
                modifier = Modifier.fillMaxSize().padding(padding), contentScale = ContentScale.Fit,
                onError = { error = it.result.throwable.message ?: it.result.throwable.toString() },
            )
        }
    }
}
