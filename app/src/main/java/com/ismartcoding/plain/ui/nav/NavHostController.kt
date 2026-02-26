package com.ismartcoding.plain.ui.nav

import android.net.Uri
import androidx.navigation.NavHostController
import com.ismartcoding.plain.enums.TextFileType


fun NavHostController.navigateText(title: String, content: String, language: String) {
    navigate(Routing.Text(title, content, language)) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateChatEditText(id: String, content: String) {
    navigate(Routing.ChatEditText(id, content)) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateChatText(content: String) {
    navigate(Routing.ChatText(content)) {
        launchSingleTop = true
    }
}

fun NavHostController.navigatePdf(uri: Uri) {
    navigate(Routing.PdfViewer(uri.toString())) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateOtherFile(path: String, title: String = "") {
    navigate(Routing.OtherFile(path, title)) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateTextFile(path: String, title: String = "", mediaId: String = "", type: TextFileType = TextFileType.DEFAULT) {
    navigate(Routing.TextFile(path, title, mediaId, type.name)) {
        launchSingleTop = true
    }
}

fun NavHostController.navigateFiles(folderPath: String = "") {
    navigate(Routing.Files(folderPath)) {
        launchSingleTop = true
    }
}
