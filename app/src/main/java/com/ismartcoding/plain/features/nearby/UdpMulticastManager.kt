package com.ismartcoding.plain.features.nearby

import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.nearby.NearbyDiscoverManager.MULTICAST_PORT
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import kotlin.coroutines.coroutineContext

/**
 * UDP Multicast network manager for handling packet sending and receiving
 * Now uses subscription pattern instead of polling for better performance
 */
class UdpMulticastManager {
    companion object {
        private const val MULTICAST_ADDRESS = "224.0.0.100"
        private const val RECEIVE_TIMEOUT = 1000             // Reduced timeout for better responsiveness
        private const val BUFFER_SIZE = 2048
    }

    data class MulticastMessage(
        val message: String,
        val senderIP: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private var receiverJob: Job? = null
    private var onMessageReceived: ((message: String, senderIP: String) -> Unit)? = null

    fun sendMulticast(message: String) {
        coIO {
            var socket: MulticastSocket? = null
            try {
                socket = MulticastSocket()
                socket.timeToLive = 1  // Limit to local subnet

                val multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS)
                val messageBytes = message.toByteArray()
                val packet = DatagramPacket(
                    messageBytes,
                    messageBytes.size,
                    multicastAddress,
                    MULTICAST_PORT
                )

                socket.send(packet)
                LogCat.d("Multicast sent: $message")
            } catch (e: Exception) {
                LogCat.e("Error sending multicast: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    /**
     * Create a Flow to subscribe to multicast messages
     * This replaces the polling approach with a reactive subscription pattern
     */
    fun messageFlow(): Flow<MulticastMessage> = callbackFlow {
        var socket: MulticastSocket? = null
        LogCat.d("Starting multicast message flow subscription")

        try {
            socket = MulticastSocket(MULTICAST_PORT).apply {
                soTimeout = RECEIVE_TIMEOUT
                reuseAddress = true
            }
            
            val multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS)
            socket.joinGroup(multicastAddress)
            
            val buffer = ByteArray(BUFFER_SIZE)
            
            // Continuous listening, controlled by coroutine's isActive
            while (coroutineContext.isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)  // Block and wait for messages
                    
                    val message = String(packet.data, 0, packet.length)
                    val senderIP = packet.address.hostAddress ?: ""
                    
                    // Use trySendBlocking to avoid backpressure issues
                    val result = trySendBlocking(MulticastMessage(message, senderIP))
                    if (result.isFailure) {
                        LogCat.w("Failed to send message to flow: ${result.exceptionOrNull()?.message}")
                    }
                        
                } catch (e: SocketTimeoutException) {
                    // Timeout is expected, continue listening
                    continue
                } catch (e: Exception) {
                    if (coroutineContext.isActive) {
                        LogCat.e("Error receiving multicast: ${e.message}")
                        // Don't retry immediately, let Flow's retry mechanism handle it
                        break
                    }
                }
            }
        } catch (e: Exception) {
            LogCat.e("Error setting up multicast receiver: ${e.message}")
            close(e)  // Close Flow and propagate exception
        }

        awaitClose {
            LogCat.d("Closing multicast subscription")
            try {
                socket?.leaveGroup(InetAddress.getByName(MULTICAST_ADDRESS))
                socket?.close()
            } catch (e: Exception) {
                LogCat.e("Error closing multicast socket: ${e.message}")
            }
        }
    }

    /**
     * Start receiver using callback (maintains backward compatibility)
     * Now internally uses the subscription-based approach
     */
    fun startReceiver(onMessage: (message: String, senderIP: String) -> Unit) {
        if (receiverJob?.isActive == true) return

        onMessageReceived = onMessage
        receiverJob = coIO {
            try {
                messageFlow().collect { multicastMessage ->
                    onMessageReceived?.invoke(multicastMessage.message, multicastMessage.senderIP)
                }
            } catch (e: Exception) {
                LogCat.e("Error in receiver flow: ${e.message}")
            }
        }
    }

    fun stopReceiver() {
        receiverJob?.cancel()
        receiverJob = null
        onMessageReceived = null
        LogCat.d("Multicast receiver stopped")
    }


} 