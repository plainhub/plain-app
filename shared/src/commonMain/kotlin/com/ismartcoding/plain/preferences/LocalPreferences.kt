package com.ismartcoding.plain.preferences

import androidx.compose.runtime.compositionLocalOf
import com.ismartcoding.plain.data.DUpdateInfo
import com.ismartcoding.plain.enums.DarkTheme

val LocalDarkTheme = compositionLocalOf { DarkTheme.UseDeviceTheme.value }
val LocalAmoledDarkTheme = compositionLocalOf { false }
val LocalWeb = compositionLocalOf { WebPreference.default }
val LocalUpdateInfo = compositionLocalOf { DUpdateInfo() }

val LocalNewVersion = compositionLocalOf { "" }
val LocalSkipVersion = compositionLocalOf { "" }
val LocalNewVersionPublishDate = compositionLocalOf { "" }
val LocalNewVersionLog = compositionLocalOf { "" }
val LocalNewVersionSize = compositionLocalOf { 0L }
val LocalAutoCheckUpdate = compositionLocalOf { true }
