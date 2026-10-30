package com.ismartcoding.plain.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.apps.AppPage
import com.ismartcoding.plain.ui.apps.AppsPage

internal fun NavGraphBuilder.addAppsPages(
    navController: NavHostController,
    source: @Composable () -> ContentSource? = { ContentSource.Local },
) {
    contentDestination<Routing.Apps>(source) { AppsPage(navController) }
    contentDestination<Routing.AppDetails>(source) { entry ->
        AppPage(navController, entry.toRoute<Routing.AppDetails>().id, LocalContentSource.current)
    }
}
