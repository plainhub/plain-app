package com.ismartcoding.plain.ble.server

interface BleGattServer {
    fun start()
    fun stop()
    fun refreshAdvertising()

    fun sendNotification(mac: String, charUuid: String, value: String): Boolean

    /**
     * Send a notification and suspend until the BLE stack confirms it has
     * been transmitted (via `onNotificationSent` on Android or
     * `peripheralManagerIsReadyToUpdateSubscribers` on iOS). Required for
     * chunked response delivery — without flow control, back-to-back
     * notifications can be silently dropped by the BLE controller.
     *
     * Returns true if the notification was sent, false on failure or timeout.
     */
    suspend fun sendNotificationBlocking(mac: String, charUuid: String, value: String): Boolean
}
