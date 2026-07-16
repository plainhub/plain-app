package com.ismartcoding.plain.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual fun startDlnaRenderer() {}

actual fun stopDlnaRenderer() {}

actual fun getPlayerPositionMs(player: Any?): Long = 0L

actual fun getPlayerDurationMs(player: Any?): Long = 0L

actual fun searchDlnaDevicesRaw(): Flow<DlnaSsdpResponse> = emptyFlow()

