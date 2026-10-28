package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ismartcoding.plain.ui.tools.ManagedContentPage
import com.ismartcoding.plain.ui.tools.ManagedDeviceViewModel
import com.ismartcoding.plain.ui.tools.ManagedDocumentPage
import com.ismartcoding.plain.ui.tools.ManagedImagePage
import com.ismartcoding.plain.ui.tools.ToolsPage

internal fun NavGraphBuilder.addManagedDevicePages(navController: NavHostController) {
    composable<Routing.ManagedTools> { entry ->
        val route = entry.toRoute<Routing.ManagedTools>()
        ToolsPage(navController, managedDevice(navController, route.peerId, route.publicKey))
    }
    composable<Routing.ManagedContent> { entry ->
        val route = entry.toRoute<Routing.ManagedContent>()
        ManagedContentPage(navController, managedDevice(navController, route.peerId, route.publicKey), route)
    }
    composable<Routing.ManagedDocument> { entry ->
        val route = entry.toRoute<Routing.ManagedDocument>()
        ManagedDocumentPage(navController, managedDevice(navController, route.peerId, route.publicKey), route)
    }
    composable<Routing.ManagedImage> { entry ->
        val route = entry.toRoute<Routing.ManagedImage>()
        ManagedImagePage(navController, managedDevice(navController, route.peerId, route.publicKey), route)
    }
}

@Composable
private fun managedDevice(navController: NavHostController, peerId: String, publicKey: String): ManagedDeviceViewModel {
    val owner = remember(navController, peerId, publicKey) { navController.getBackStackEntry(Routing.ManagedTools(peerId, publicKey)) }
    return viewModel(viewModelStoreOwner = owner, key = "managed-device") { ManagedDeviceViewModel(peerId, publicKey) }
}
