package com.ismartcoding.plain.web

import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.GraphqlRequest
import com.ismartcoding.lib.kgraphql.KGraphQL
import com.ismartcoding.lib.kgraphql.context
import com.ismartcoding.lib.kgraphql.schema.Schema
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaConfigurationDSL
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.preferences.AuthDevTokenPreference
import com.ismartcoding.plain.web.schemas.addAppSchema
import com.ismartcoding.plain.web.schemas.addAudioSchema
import com.ismartcoding.plain.web.schemas.addBookmarkSchema
import com.ismartcoding.plain.web.schemas.addCallSchema
import com.ismartcoding.plain.web.schemas.addChatChannelSchema
import com.ismartcoding.plain.web.schemas.addChatMessageSchema
import com.ismartcoding.plain.web.schemas.addChatQuerySchema
import com.ismartcoding.plain.web.schemas.addContactSchema
import com.ismartcoding.plain.web.schemas.addDocQueries
import com.ismartcoding.plain.web.schemas.addFeedSchema
import com.ismartcoding.plain.web.schemas.addFileMutationSchema
import com.ismartcoding.plain.web.schemas.addFileQuerySchema
import com.ismartcoding.plain.web.schemas.addFileUploadSchema
import com.ismartcoding.plain.web.schemas.addImageSchema
import com.ismartcoding.plain.web.schemas.addNoteSchema
import com.ismartcoding.plain.web.schemas.addNotificationSchema
import com.ismartcoding.plain.web.schemas.addPackageSchema
import com.ismartcoding.plain.web.schemas.addPomodoroSchema
import com.ismartcoding.plain.web.schemas.addSchemaTypes
import com.ismartcoding.plain.web.schemas.addScreenMirrorSchema
import com.ismartcoding.plain.web.schemas.addSmsSchema
import com.ismartcoding.plain.web.schemas.addTagSchema
import com.ismartcoding.plain.web.schemas.addVideoMediaSchema
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class MainGraphQL(val schema: Schema) {
    class Configuration : SchemaConfigurationDSL() {
        fun init() {
            schemaBlock = {
                addChatQuerySchema()
                addChatMessageSchema()
                addChatChannelSchema()
                addSmsSchema()
                addImageSchema()
                addAudioSchema()
                addVideoMediaSchema()
                addDocQueries()
                addContactSchema()
                addCallSchema()
                addPackageSchema()
                addFileQuerySchema()
                addFileUploadSchema()
                addFileMutationSchema()
                addFeedSchema()
                addNoteSchema()
                addTagSchema()
                addScreenMirrorSchema()
                addPomodoroSchema()
                addNotificationSchema()
                addAppSchema()
                addBookmarkSchema()
                addSchemaTypes()
            }
        }

        internal var schemaBlock: (SchemaBuilder.() -> Unit)? = null
    }

    companion object Feature : BaseApplicationPlugin<Application, Configuration, MainGraphQL> {
        override val key = AttributeKey<MainGraphQL>("MainGraphQL")

        private suspend fun executeGraphqlQL(
            schema: Schema,
            query: String,
            call: ApplicationCall,
        ): String {
            val request = Json.decodeFromString(GraphqlRequest.serializer(), query)
            return schema.execute(
                request.query,
                request.variables?.toString(),
                context { +call },
            )
        }

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit,
        ): MainGraphQL {
            val config = Configuration().apply(configure)
            val schema =
                KGraphQL.schema {
                    configuration = config
                    config.schemaBlock?.invoke(this)
                }

            pipeline.routing {
                route("/graphql") {
                    post {
                        if (!TempData.webEnabled) {
                            call.respond(HttpStatusCode.Forbidden)
                            return@post
                        }
                        val clientId = call.request.header("c-id") ?: ""
                        if (clientId.isNotEmpty()) {
                            val token = HttpServerManager.tokenCache[clientId]
                            if (token == null) {
                                call.respond(HttpStatusCode.Unauthorized)
                                return@post
                            }

                            val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, call.receive())
                            val decryptedStr = decryptedBytes?.decodeToString() ?: ""
                            if (decryptedStr.isEmpty()) {
                                call.respond(HttpStatusCode.Unauthorized)
                                return@post
                            }

                            val parsed = ReplayGuard.parse(decryptedStr)
                            if (parsed == null) {
                                call.respond(HttpStatusCode.BadRequest)
                                return@post
                            }
                            val err = ReplayGuard.validate(clientId, parsed)
                            if (err != null) {
                                call.respond(HttpStatusCode.BadRequest)
                                return@post
                            }

                            HttpServerManager.clientRequestTs[clientId] = System.currentTimeMillis()
                            sendEvent(WebRequestReceivedEvent())
                            val r = executeGraphqlQL(schema, parsed.body, call)
                            call.respondBytes(CryptoHelper.chaCha20Encrypt(token, r))
                        } else {
                            val authStr = call.request.header("authorization")?.split(" ")
                            val token = AuthDevTokenPreference.getAsync(MainApp.instance)
                            if (token.isEmpty() || authStr?.get(1) != token) {
                                call.respondText(
                                    """{"errors":[{"message":"Unauthorized"}]}""",
                                    contentType = ContentType.Application.Json,
                                )
                                return@post
                            }

                            val requestStr = call.receiveText()
                            LogCat.d("[Request] $requestStr")
                            HttpServerManager.clientRequestTs[clientId] = System.currentTimeMillis() // record the api request time
                            sendEvent(WebRequestReceivedEvent())
                            val r = executeGraphqlQL(schema, requestStr, call)
                            call.respondText(r, contentType = ContentType.Application.Json)
                        }
                    }
                }
            }

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                try {
                    coroutineScope {
                        proceed()
                    }
                } catch (e: Throwable) {
                    if (e is GraphQLError) {
                        val clientId = call.request.header("c-id") ?: ""
                        val type = call.request.header("c-type") ?: "" // peer
                        val channelId = call.request.header("c-cid") ?: "" // chat channel id
                        if (clientId.isNotEmpty()) {
                            val token = if (channelId.isNotEmpty()) {
                                ChatCacheManager.channelKeyCache[channelId]
                            } else if (type == "peer") {
                                ChatCacheManager.peerKeyCache[channelId]
                            } else {
                                HttpServerManager.tokenCache[clientId]
                            }
                            if (token != null) {
                                call.respondBytes(CryptoHelper.chaCha20Encrypt(token, e.serialize()))
                            } else {
                                call.respond(HttpStatusCode.Unauthorized)
                            }
                        } else {
                            context.respond(HttpStatusCode.OK, e.serialize())
                        }
                    } else {
                        throw e
                    }
                }
            }
            return MainGraphQL(schema)
        }
    }
}
