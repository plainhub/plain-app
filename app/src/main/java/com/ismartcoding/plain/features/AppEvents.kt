package com.ismartcoding.plain.features

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.content.ContextCompat
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
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.SslHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.data.enums.*
import com.ismartcoding.plain.db.DAIChat
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.features.aichat.AIChatHelper
import com.ismartcoding.plain.features.audio.AudioAction
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.features.exchange.DExchangeRates
import com.ismartcoding.plain.features.feed.FeedWorkerStatus
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import com.ismartcoding.plain.web.websocket.WebSocketHelper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

class BoxConnectivityStateChangedEvent

class UpdateLocaleEvent
class HttpServerEnabledEvent(val enabled: Boolean)

class StartHttpServerEvent
class StartScreenMirrorEvent


class SendMessageEvent(val content: DMessageContent)
class UpdateMessageEvent(var chatItem: DChat)
class DeleteChatItemViewEvent(val id: String)

class DeviceNameUpdatedEvent(val id: String, val name: String?)

class CurrentBoxChangedEvent

class ChatInputEditEvent(val content: String)
class ChatItemRefreshEvent(val data: DMessageContent)

class VocabularyCreatedEvent
class VocabularyUpdatedEvent
class VocabularyDeletedEvent(val id: String)

class VocabularyWordsDeletedEvent(val id: String)
class VocabularyWordsUpdatedEvent(val id: String)

class EnableWebConsoleEvent(val isEnabled: Boolean)

class ConfirmToAcceptLoginEvent(
    val session: DefaultWebSocketServerSession, val clientId: String, val browserName: String,
    val browserVersion: String, val osName: String, val osVersion: String, val isMobile: Boolean
)

class PermissionResultEvent(val permission: String)
class RequestPermissionEvent(val permission: String)
class PickFileEvent(val tag: PickFileTag, val type: PickFileType, val multiple: Boolean)
class PickFileResultEvent(val tag: PickFileTag, val type: PickFileType, val uris: Set<Uri>)
class ExportFileEvent(val type: ExportFileType, val fileName: String)
class ExportFileResultEvent(val type: ExportFileType, val uri: Uri)

class ActionEvent(val source: ActionSourceType, val action: ActionType, val ids: Set<String>, val extra: Any? = null)

class AudioActionEvent(val action: AudioAction)
class ClearAudioPlaylistEvent

class FeedStatusEvent(val feedId: String, val status: FeedWorkerStatus)

data class PlayAudioEvent(val uri: Uri)
data class PlayAudioResultEvent(val uri: Uri)

data class LatestExchangeRatesResultEvent(val result: ApiResult)
class FetchLatestExchangeRatesEvent

class AIChatCreatedEvent(val item: DAIChat)

object AppEvents {
    private val mediaPlayer = MediaPlayer()
    private var mediaPlayingUri: Uri? = null

    @OptIn(BetaOpenAI::class)
    fun register() {
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
            WebSocketHelper.sendEventAsync(event)
        }

        receiveEventHandler<FetchLatestExchangeRatesEvent> {
            val client = HttpClientManager.httpClient()
            try {
                val r = withIO { client.get("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml") }
                if (r.status == HttpStatusCode.OK) {
                    val xml = r.body<String>()
                    UIDataCache.current().run {
                        val ex = DExchangeRates()
                        ex.fromXml(xml)
                        latestExchangeRates = ex
                    }
                }
                sendEvent(LatestExchangeRatesResultEvent(ApiResult(r)))
            } catch (ex: Exception) {
                sendEvent(LatestExchangeRatesResultEvent(ApiResult(null, ex)))
            }
        }

        receiveEventHandler<PermissionResultEvent> { event ->
            if (event.permission == Permission.POST_NOTIFICATIONS.toSysPermission()) {
                AudioPlayer.instance.showNotification()
            }
        }

        receiveEventHandler<StartHttpServerEvent> {
            ContextCompat.startForegroundService(MainApp.instance, Intent(MainApp.instance, HttpServerService::class.java))
        }

        receiveEventHandler<AIChatCreatedEvent> { event ->
            coIO {
                val openAI = OpenAI(
                    OpenAIConfig(
                        token = LocalStorage.chatGPTApiKey,
                        timeout = Timeout(socket = 60.seconds),
                    )
                )

                val messages = mutableListOf<ChatMessage>()
                val parentId = event.item.parentId.ifEmpty { event.item.id }
                messages.addAll(AIChatHelper.getChats(parentId).map {
                    ChatMessage(
                        role = if (it.isMe) ChatRole.User else ChatRole.Assistant,
                        content = it.content,
                    )
                })

                try {
                    val chatCompletionRequest = ChatCompletionRequest(
                        model = ModelId("gpt-3.5-turbo"),
                        messages = messages
                    )
                    openAI.chatCompletions(chatCompletionRequest).collect { completion ->
                        val data = JSONObject()
                        data.put("parentId", parentId)
                        val c = completion.choices.getOrNull(0)
                        data.put("content", c?.delta?.content ?: "")
                        data.put("finishReason", c?.finishReason ?: "")
                        LogCat.d(Json.encodeToString(completion))
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