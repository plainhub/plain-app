package com.ismartcoding.plain.ui.navigation

import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.app.DeviceContentEntry
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.lib.extensions.isPdfFile

internal suspend fun openDeviceContentEntry(navController: NavHostController, entry: DeviceContentEntry, source: ContentSource) {
    when (entry.feature) {
        AppFeatureType.NOTES -> navController.navigate(Routing.NoteDetail(entry.id))
        AppFeatureType.FEEDS -> navController.navigate(Routing.FeedEntry(entry.id))
        AppFeatureType.APPS -> navController.navigate(Routing.AppDetails(entry.id))
        AppFeatureType.FILES -> navController.navigate(Routing.Files(entry.path))
        AppFeatureType.CHAT -> navController.navigate(Routing.Chat(entry.id))
        AppFeatureType.DOCS -> when {
            entry.path.isTextFile() -> navController.navigateTextFile(entry.path, entry.title, entry.id)
            entry.path.isPdfFile() -> navController.navigatePdf(com.ismartcoding.plain.platform.files.fileToUriString(source.fileForOpen(entry.path, entry.title)), entry.title)
            else -> navController.navigateOtherFile(entry.path, entry.title)
        }
        else -> error("Unsupported search destination ${entry.feature}")
    }
}
