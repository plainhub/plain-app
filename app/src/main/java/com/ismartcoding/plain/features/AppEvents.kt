package com.ismartcoding.plain.features

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.helpers.SslHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.preference.ChatGPTApiKeyPreference
import com.ismartcoding.plain.db.DAIChat
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.web.AuthRequest
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import com.ismartcoding.plain.web.websocket.WebSocketHelper
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

class BoxConnectivityStateChangedEvent

class StartHttpServerEvent

class HttpServerStateChangedEvent(val state: HttpServerState)

class StartScreenMirrorEvent

class RestartAppEvent

class ConfirmDialogEvent(
    val title: String,
    val message: String,
    val confirmButton: Pair<String, () -> Unit>,
    val dismissButton: Pair<String, () -> Unit>?
)

class LoadingDialogEvent(
    val show: Boolean,
    val message: String = ""
)

class WindowFocusChangedEvent(val hasFocus: Boolean)

class DeleteChatItemViewEvent(val id: String)

class DeviceNameUpdatedEvent(val id: String, val name: String?)

class CurrentBoxChangedEvent

class VocabularyCreatedEvent

class VocabularyUpdatedEvent

class VocabularyDeletedEvent(val id: String)

class VocabularyWordsDeletedEvent(val id: String)

class VocabularyWordsUpdatedEvent(val id: String)

class ConfirmToAcceptLoginEvent(
    val session: DefaultWebSocketServerSession,
    val clientId: String,
    val request: AuthRequest,
)

class RequestPermissionsEvent(vararg val permissions: Permission)
class PermissionsResultEvent(val map: Map<String, Boolean>) {
    fun has(permission: Permission): Boolean {
        return map.containsKey(permission.toSysPermission())
    }
}

class PickFileEvent(val tag: PickFileTag, val type: PickFileType, val multiple: Boolean)

class PickFileResultEvent(val tag: PickFileTag, val type: PickFileType, val uris: Set<Uri>)

class ExportFileEvent(val type: ExportFileType, val fileName: String)

class ExportFileResultEvent(val type: ExportFileType, val uri: Uri)

class ActionEvent(val source: ActionSourceType, val action: ActionType, val ids: Set<String>, val extra: Any? = null)

class AudioActionEvent(val action: AudioAction)

class IgnoreBatteryOptimizationEvent
class AcquireWakeLockEvent
class ReleaseWakeLockEvent

class IgnoreBatteryOptimizationResultEvent

class CancelNotificationsEvent(val ids: Set<String>)

class ClearAudioPlaylistEvent

class FeedStatusEvent(val feedId: String, val status: FeedWorkerStatus)

data class PlayAudioEvent(val uri: Uri)

data class PlayAudioResultEvent(val uri: Uri)

class AIChatCreatedEvent(val item: DAIChat)

object AppEvents {
    private lateinit var mediaPlayer: MediaPlayer
    private var mediaPlayingUri: Uri? = null
    val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${BuildConfig.APPLICATION_ID}:http_server")

    @OptIn(BetaOpenAI::class)
    fun register() {
        mediaPlayer = MediaPlayer()
        receiveEventHandler<PlayAudioEvent> { event ->
            launch {
                try {
                    SslHelper.disableSSLCertificateChecking()
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        mediaPlayingUri?.let {
                            if (it.toString() != event.uri.toString()) {
                                sendEvent(PlayAudioResultEvent(it))
                            }
                        }
                    }
                    mediaPlayingUri = event.uri
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(MainApp.instance, event.uri)
                    mediaPlayer.setOnCompletionListener {
                        mediaPlayer.stop()
                        sendEvent(PlayAudioResultEvent(event.uri))
                    }
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    sendEvent(PlayAudioResultEvent(event.uri))
                }
            }
        }

        receiveEventHandler<WebSocketEvent> { event ->
            coIO {
                WebSocketHelper.sendEventAsync(event)
            }
        }

        receiveEventHandler<AcquireWakeLockEvent> {
            coIO {
                LogCat.d("AcquireWakeLockEvent")
                if (!wakeLock.isHeld) {
                    wakeLock.acquire()
                }
            }
        }

        receiveEventHandler<ReleaseWakeLockEvent> {
            coIO {
                LogCat.d("ReleaseWakeLockEvent")
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }

        receiveEventHandler<PermissionsResultEvent> { event ->
            if (event.map.containsKey(Permission.POST_NOTIFICATIONS.toSysPermission())) {
                if (AudioPlayer.isPlaying()) {
                    AudioPlayer.pause()
                    AudioPlayer.play()
                }
            }
        }

        receiveEventHandler<StartHttpServerEvent> {
            coIO {
                try {
                    val context = MainApp.instance
                    context.startService(Intent(context, HttpServerService::class.java))
                } catch (ex: Exception) {
                    LogCat.e(ex.toString())
                }
            }
        }

        receiveEventHandler<AIChatCreatedEvent> { event ->
            coIO {
                val parentId = event.item.parentId.ifEmpty { event.item.id }
                try {
                    val openAI =
                        OpenAI(
                            OpenAIConfig(
                                token = ChatGPTApiKeyPreference.getAsync(MainApp.instance),
                                timeout = Timeout(socket = 60.seconds),
                            ),
                        )

                    val messages = mutableListOf<ChatMessage>()
                    messages.addAll(
                        AIChatHelper.getChats(parentId).map {
                            ChatMessage(
                                role = if (it.isMe) ChatRole.User else ChatRole.Assistant,
                                content = it.content,
                            )
                        },
                    )

                    val chatCompletionRequest =
                        ChatCompletionRequest(
                            model = ModelId("gpt-3.5-turbo"),
                            messages = messages,
                        )
                    openAI.chatCompletions(chatCompletionRequest).collect { completion ->
                        val data = JSONObject()
                        data.put("parentId", parentId)
                        val c = completion.choices.getOrNull(0)
                        data.put("content", c?.delta?.content ?: "")
                        data.put("finishReason", c?.finishReason ?: "")
                        LogCat.d(jsonEncode(completion))
                        sendEvent(WebSocketEvent(EventType.AI_CHAT_REPLIED, data.toString()))
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    LogCat.e(ex.toString())
                    val data = JSONObject()
                    data.put("parentId", parentId)
                    data.put("content", ex.toString())
                    data.put("finishReason", "stop")
                    sendEvent(WebSocketEvent(EventType.AI_CHAT_REPLIED, data.toString()))
                }
            }
        }
    }
}
