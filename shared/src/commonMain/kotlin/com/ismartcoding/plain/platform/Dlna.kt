package com.ismartcoding.plain.platform

import kotlinx.coroutines.flow.Flow

/**
 * Start the DLNA renderer service (HTTP + SSDP advertiser).
 * No-op on iOS (no DLNA receiver support).
 */
expect fun startDlnaRenderer()

/**
 * Stop the DLNA renderer service and release the server socket.
 * No-op on iOS.
 */
expect fun stopDlnaRenderer()

/**
 * Current playback position in milliseconds for the given platform player instance.
 * @param player platform-specific player object (Android: ExoPlayer; iOS: AVPlayer or null)
 */
expect fun getPlayerPositionMs(player: Any?): Long

/**
 * Total duration in milliseconds for the given platform player instance.
 */
expect fun getPlayerDurationMs(player: Any?): Long

/**
 * 扫描 SSDP 多播响应，返回每个响应对应的来源 IP 和原始 SSDP 报文。
 * 仅做平台最底层的 UDP 多播 + MulticastLock（Android）工作；
 * 业务层（解析 header、构造 DlnaDevice）由 commonMain 的 DlnaDeviceScanner 负责。
 * iOS 返回空 Flow（无 DLNA 投屏支持）。
 */
expect fun searchDlnaDevicesRaw(): Flow<DlnaSsdpResponse>

/**
 * SSDDP 多播搜索得到的原始数据（来源 IP + 报文文本）。
 */
data class DlnaSsdpResponse(
    val hostAddress: String,
    val header: String,
)

