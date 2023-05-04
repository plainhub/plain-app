package com.ismartcoding.plain.web

import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.GraphqlRequest
import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.context
import com.apurebase.kgraphql.helpers.getFields
import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.dsl.SchemaConfigurationDSL
import com.apurebase.kgraphql.schema.execution.Execution
import com.apurebase.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.allowSensitivePermissions
import com.ismartcoding.lib.extensions.newPath
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.PhoneHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.BoxProxyApi
import com.ismartcoding.plain.api.HttpApiTimeout
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.aichat.AIChatHelper
import com.ismartcoding.plain.features.application.ApplicationHelper
import com.ismartcoding.plain.features.audio.AudioHelper
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.features.audio.DPlaylistAudio
import com.ismartcoding.plain.features.audio.MediaPlayMode
import com.ismartcoding.plain.features.call.CallHelper
import com.ismartcoding.plain.features.call.SimHelper
import com.ismartcoding.plain.features.chat.ChatHelper
import com.ismartcoding.plain.features.contact.ContactHelper
import com.ismartcoding.plain.features.contact.GroupHelper
import com.ismartcoding.plain.features.contact.SourceHelper
import com.ismartcoding.plain.features.exchange.DExchangeRates
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.fetchContentAsync
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.image.ImageHelper
import com.ismartcoding.plain.features.note.NoteHelper
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.features.tag.TagRelationStub
import com.ismartcoding.plain.features.theme.AppTheme
import com.ismartcoding.plain.features.video.VideoHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.*
import com.ismartcoding.plain.workers.FeedFetchWorker
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.*
import java.io.StringReader
import java.io.StringWriter
import kotlin.io.path.Path
import kotlin.io.path.moveTo

class SXGraphQL(val schema: Schema) {
    class Configuration : SchemaConfigurationDSL() {
        fun init() {
            schemaBlock = {
                query("aiChats") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        val items = AIChatHelper.search(QueryHelper.prepareQuery(query), limit, offset)
                        items.map { it.toModel() }
                    }
                    type<AIChat> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.AI_CHAT)
                            }
                        }
                    }
                }
                query("aiChatCount") {
                    resolver { query: String ->
                        AIChatHelper.count(QueryHelper.prepareQuery(query))
                    }
                }
                query("aiChatConfig") {
                    resolver { ->
                        AIChatConfig(LocalStorage.chatGPTApiKey)
                    }
                }
                query("aiChat") {
                    resolver { id: ID ->
                        AIChatHelper.getAsync(id.value)?.toModel()
                    }
                }
                query("chatItems") {
                    resolver { ->
                        val items = AppDatabase.instance.chatDao().getAll()
                        items.map { it.toModel() }
                    }
                }
                type<ChatItem> {
                    property(ChatItem::_content) {
                        ignore = true
                    }
                    property("data") {
                        resolver { c: ChatItem ->
                            c.getData()
                        }
                    }
                }
                query("messages") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        Permission.READ_SMS.check()
                        SmsHelper.search(MainApp.instance, QueryHelper.prepareQuery(query), limit, offset).map { it.toModel() }
                    }
                    type<Message> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.SMS)
                            }
                        }
                    }
                }
                query("messageCount") {
                    resolver { query: String ->
                        if (Permission.READ_SMS.can()) {
                            SmsHelper.count(MainApp.instance, QueryHelper.prepareQuery(query))
                        } else {
                            -1
                        }
                    }
                }
                query("images") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        ImageHelper.search(MainApp.instance, QueryHelper.prepareQuery(query), limit, offset, LocalStorage.imageSortBy).map { it.toModel() }
                    }
                    type<Image> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.IMAGE)
                            }
                        }
                    }
                }
                query("imageCount") {
                    resolver { query: String ->
                        if (Permission.WRITE_EXTERNAL_STORAGE.can()) {
                            ImageHelper.count(MainApp.instance, QueryHelper.prepareQuery(query))
                        } else {
                            -1
                        }
                    }
                }
                query("videos") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        VideoHelper.search(MainApp.instance, QueryHelper.prepareQuery(query), limit, offset, LocalStorage.videoSortBy).map { it.toModel() }
                    }
                    type<Video> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.VIDEO)
                            }
                        }
                    }
                }
                query("videoCount") {
                    resolver { query: String ->
                        if (Permission.WRITE_EXTERNAL_STORAGE.can()) {
                            VideoHelper.count(MainApp.instance, QueryHelper.prepareQuery(query))
                        } else {
                            -1
                        }
                    }
                }
                query("audios") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        AudioHelper.search(MainApp.instance, QueryHelper.prepareQuery(query), limit, offset, LocalStorage.audioSortBy).map { it.toModel() }
                    }
                    type<Audio> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.AUDIO)
                            }
                        }
                    }
                }
                query("audioCount") {
                    resolver { query: String ->
                        if (Permission.WRITE_EXTERNAL_STORAGE.can()) {
                            AudioHelper.count(MainApp.instance, QueryHelper.prepareQuery(query))
                        } else {
                            -1
                        }
                    }
                }
                query("contacts") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        Permission.READ_CONTACTS.check()
                        ContactHelper.search(MainApp.instance, QueryHelper.prepareQuery(query), limit, offset).map { it.toModel() }
                    }
                    type<Contact> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.CONTACT)
                            }
                        }
                    }
                }
                query("contactCount") {
                    resolver { query: String ->
                        if (Permission.READ_CONTACTS.can()) {
                            ContactHelper.count(MainApp.instance, QueryHelper.prepareQuery(query))
                        } else {
                            -1
                        }
                    }
                }
                query("contactSources") {
                    resolver { ->
                        Permission.READ_CONTACTS.check()
                        SourceHelper.getAll().map { it.toModel() }
                    }
                }
                query("contactGroups") {
                    resolver { node: Execution.Node ->
                        Permission.READ_CONTACTS.check()
                        val groups = GroupHelper.getAll().map { it.toModel() }
                        val fields = node.getFields()
                        if (fields.contains(ContactGroup::contactCount.name)) {
                            // TODO support contactsCount
                        }
                        groups
                    }
                }
                query("calls") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        Permission.READ_CALL_LOG.check()
                        CallHelper.search(MainApp.instance, QueryHelper.prepareQuery(query), limit, offset).map { it.toModel() }
                    }
                    type<Call> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.CALL)
                            }
                        }
                    }
                }
                query("callCount") {
                    resolver { query: String ->
                        if (Permission.READ_CALL_LOG.can()) {
                            CallHelper.count(MainApp.instance, QueryHelper.prepareQuery(query))
                        } else {
                            -1
                        }
                    }
                }
                query("sims") {
                    resolver { ->
                        SimHelper.getAll().map { it.toModel() }
                    }
                }
                query("apps") {
                    resolver { offset: Int, limit: Int, query: String ->
                        ApplicationHelper.search(query, limit, offset).map { it.toModel() }
                    }
                }
                query("appCount") {
                    resolver { query: String ->
                        ApplicationHelper.count(query)
                    }
                }
                query("storageStats") {
                    resolver { ->
                        FileSystemHelper.getMainStorageStats(MainApp.instance).toModel()
                    }
                }
                query("screenMirrorImage") {
                    resolver { ->
                        ScreenMirrorService.instance?.getLatestImageBase64() ?: ""
                    }
                }
                query("files") {
                    resolver { dir: String, showHidden: Boolean ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        val p = dir.ifEmpty { FileSystemHelper.getInternalStoragePath(MainApp.instance) }
                        val files = FileSystemHelper.getFilesList(p, showHidden, LocalStorage.fileSortBy).map { it.toModel() }
                        Files(p, files)
                    }
                }
                query("boxes") {
                    resolver { ->
                        val items = AppDatabase.instance.boxDao().getAll()
                        items.map { it.toModel() }
                    }
                }
                query("tags") {
                    resolver { type: TagType ->
                        val items = TagHelper.getAll(type)
                        items.map { it.toModel() }
                    }
                }
                query("feeds") {
                    resolver { ->
                        val items = FeedHelper.getAll()
                        items.map { it.toModel() }
                    }
                }
                query("feedEntries") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        val items = FeedEntryHelper.search(QueryHelper.prepareQuery(query), limit, offset)
                        items.map { it.toModel() }
                    }
                    type<FeedEntry> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.FEED_ENTRY)
                            }
                        }
                    }
                }
                query("feedEntryCount") {
                    resolver { query: String ->
                        FeedEntryHelper.count(QueryHelper.prepareQuery(query))
                    }
                }
                query("feedEntry") {
                    resolver { id: ID ->
                        val data = FeedEntryHelper.feedEntryDao.getById(id.value)
                        data?.toModel()
                    }
                }
                query("notes") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        val items = NoteHelper.search(QueryHelper.prepareQuery(query), limit, offset)
                        items.map { it.toModel() }
                    }
                    type<Note> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, TagType.NOTE)
                            }
                        }
                    }
                }
                query("noteCount") {
                    resolver { query: String ->
                        NoteHelper.count(QueryHelper.prepareQuery(query))
                    }
                }
                query("note") {
                    resolver { id: ID ->
                        val data = NoteHelper.getById(id.value)
                        data?.toModel()
                    }
                }
                query("latestExchangeRates") {
                    resolver { live: Boolean ->
                        if (live || UIDataCache.current().latestExchangeRates == null) {
                            val client = HttpClientManager.httpClient()
                            val r = withIO { client.get("https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml") }
                            if (r.status == HttpStatusCode.OK) {
                                val xml = r.body<String>()
                                UIDataCache.current().run {
                                    val ex = DExchangeRates()
                                    ex.fromXml(xml)
                                    latestExchangeRates = ex
                                }
                            }
                        }
                        UIDataCache.current().latestExchangeRates?.toModel()
                    }
                }
                query("app") {
                    resolver { ->
                        App(
                            usbConnected = PlugInControlReceiver.isUSBConnected(),
                            fileIdToken = LocalStorage.fileIdToken,
                            MainApp.instance.getExternalFilesDir(null)?.path ?: "",
                            if (LocalStorage.demoMode) "Demo phone" else PhoneHelper.getDeviceName(MainApp.instance),
                            PhoneHelper.getBatteryPercentage(MainApp.instance),
                            LocalStorage.appLocale, LocalStorage.appTheme,
                            MainApp.getAppVersion(),
                            Permission.values().filter { it.isEnabled() && it.can() },
                            LocalStorage.audioPlaylist.map { it.toModel() },
                            LocalStorage.audioPlayMode,
                            LocalStorage.audioPlaying?.path ?: "",
                            MainApp.instance.allowSensitivePermissions()
                        )
                    }
                }
                query("fileIds") {
                    resolver { paths: List<String> ->
                        paths.map { FileHelper.getFileId(it) }
                    }
                }
                mutation("uninstallApps") {
                    resolver { ids: List<ID> ->
                        ids.forEach {
                            ApplicationHelper.uninstall(MainActivity.instance.get()!!, it.value)
                        }
                        true
                    }
                }
                mutation("updateAIChatConfig") {
                    resolver { chatGPTApiKey: String ->
                        LocalStorage.chatGPTApiKey = chatGPTApiKey
                        AIChatConfig(LocalStorage.chatGPTApiKey)
                    }
                }
                mutation("createChatItem") {
                    resolver { message: String ->
                        val items = ChatHelper.createChatItemsAsync(
                            DMessageContent(
                                DMessageType.TEXT.value,
                                DMessageText(message)
                            )
                        )
                        sendEvent(HttpServerEvents.MessageCreatedEvent(items))
                        items.map { it.toModel() }
                    }
                }
                mutation("deleteChatItem") {
                    resolver { id: ID ->
                        val item = ChatHelper.getAsync(id.value)
                        if (item != null) {
                            ChatHelper.deleteAsync(item)
                        }
                        true
                    }
                }
                mutation("createAIChat") {
                    resolver { id: ID, message: String, isMe: Boolean ->
                        if (LocalStorage.chatGPTApiKey.isEmpty()) {
                            throw Exception("no_api_key")
                        }
                        val items = AIChatHelper.createChatItemsAsync(id.value, isMe, message)
                        if (isMe) {
                            sendEvent(AIChatCreatedEvent(items[0]))
                        }
                        items.map { it.toModel() }
                    }
                }
                mutation("deleteAIChatsByParentIds") {
                    resolver { ids: List<ID> ->
                        val newIds = ids.map { it.value }.toSet()
                        AIChatHelper.deleteByParentIdsAsync(newIds)
                        true
                    }
                }
                mutation("deleteAIChats") {
                    resolver { ids: List<ID> ->
                        val newIds = ids.map { it.value }.toSet()
                        AIChatHelper.deleteAsync(newIds)
                        true
                    }
                }
                mutation("deleteContacts") {
                    resolver { ids: List<ID> ->
                        Permission.WRITE_CONTACTS.check()
                        val newIds = ids.map { it.value }.toSet()
                        TagHelper.deleteTagRelationByKeys(newIds, TagType.CONTACT)
                        ContactHelper.deleteByIds(MainApp.instance, newIds)
                        true
                    }
                }
                mutation("fetchFeedContent") {
                    resolver { id: ID ->
                        val feed = FeedEntryHelper.feedEntryDao.getById(id.value)
                        feed?.fetchContentAsync()
                        feed?.toModel()
                    }
                }
                mutation("updateContact") {
                    resolver { id: ID, input: ContactInput ->
                        Permission.WRITE_CONTACTS.check()
                        ContactHelper.update(id.value, input)
                        ContactHelper.get(MainApp.instance, id.value)?.toModel()
                    }
                }
                mutation("createContact") {
                    resolver { input: ContactInput ->
                        Permission.WRITE_CONTACTS.check()
                        val id = ContactHelper.create(input)
                        if (id.isEmpty()) null else ContactHelper.get(MainApp.instance, id)?.toModel()
                    }
                }
                mutation("createTag") {
                    resolver { type: TagType, name: String ->
                        val id = TagHelper.addOrUpdate("") {
                            this.name = name
                            this.type = type.value
                        }
                        TagHelper.get(id)?.toModel()
                    }
                }
                mutation("updateTag") {
                    resolver { id: ID, name: String ->
                        TagHelper.addOrUpdate(id.value) {
                            this.name = name
                        }
                        TagHelper.get(id.value)?.toModel()
                    }
                }
                mutation("deleteTag") {
                    resolver { id: ID ->
                        TagHelper.deleteTagRelationsByTagId(id.value)
                        TagHelper.delete(id.value)
                        true
                    }
                }
                mutation("syncFeeds") {
                    resolver { id: ID? ->
                        FeedFetchWorker.oneTimeRequest(id?.value ?: "")
                        true
                    }
                }
                mutation("updateFeed") {
                    resolver { id: ID, name: String ->
                        FeedHelper.updateAsync(id.value) {
                            this.name = name
                        }
                        FeedHelper.getById(id.value)?.toModel()
                    }
                }
                mutation("startScreenMirror") {
                    resolver { ->
                        sendEvent(StartScreenMirrorEvent())
                        true
                    }
                }
                mutation("stopScreenMirror") {
                    resolver { ->
                        ScreenMirrorService.instance?.stop()
                        ScreenMirrorService.instance = null
                        true
                    }
                }
                mutation("createContactGroup") {
                    resolver { name: String, accountName: String, accountType: String ->
                        Permission.WRITE_CONTACTS.check()
                        GroupHelper.create(name, accountName, accountType).toModel()
                    }
                }

                mutation("call") {
                    resolver { number: String ->
                        Permission.CALL_PHONE.check()
                        CallHelper.call(MainActivity.instance.get()!!, number)
                        true
                    }
                }
                mutation("updateContactGroup") {
                    resolver { id: ID, name: String ->
                        Permission.WRITE_CONTACTS.check()
                        GroupHelper.update(id.value, name)
                        ContactGroup(id, name)
                    }
                }
                mutation("deleteContactGroup") {
                    resolver { id: ID ->
                        Permission.WRITE_CONTACTS.check()
                        GroupHelper.delete(id.value)
                        true
                    }
                }

                mutation("deleteCalls") {
                    resolver { ids: List<ID> ->
                        Permission.WRITE_CALL_LOG.check()
                        val newIds = ids.map { it.value }.toSet()
                        TagHelper.deleteTagRelationByKeys(newIds, TagType.CALL)
                        CallHelper.deleteByIds(MainApp.instance, newIds)
                        true
                    }
                }
                mutation("deleteFiles") {
                    resolver { paths: List<String> ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        paths.forEach {
                            java.io.File(it).deleteRecursively()
                        }
                        MainApp.instance.scanFileByConnection(paths.toTypedArray())
                        true
                    }
                }
                mutation("createDir") {
                    resolver { path: String ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        FileSystemHelper.createDirectory(path).toModel()
                    }
                }
                mutation("renameFile") {
                    resolver { path: String, name: String ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        val dst = FileHelper.rename(path, name)
                        if (dst != null) {
                            MainApp.instance.scanFileByConnection(path)
                            MainApp.instance.scanFileByConnection(dst)
                        }
                        dst != null
                    }
                }
                mutation("copyFile") {
                    resolver { src: String, dst: String, overwrite: Boolean ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        val dstFile = java.io.File(dst)
                        if (overwrite || !dstFile.exists()) {
                            java.io.File(src).copyRecursively(dstFile, overwrite)
                        } else {
                            java.io.File(src)
                                .copyRecursively(java.io.File(dstFile.newPath()), false)
                        }
                        MainApp.instance.scanFileByConnection(dstFile)
                        true
                    }
                }
                mutation("playAudio") {
                    resolver { path: String ->
                        val audio = DPlaylistAudio.fromPath(MainApp.instance, path)
                        LocalStorage.audioPlaying = audio
                        if (!LocalStorage.audioPlaylist.any { it.path == audio.path }) {
                            LocalStorage.addPlaylistAudio(audio)
                        }
                        audio.toModel()
                    }
                }
                mutation("updateAudioPlayMode") {
                    resolver { mode: MediaPlayMode ->
                        LocalStorage.audioPlayMode = mode
                        true
                    }
                }
                mutation("clearAudioPlaylist") {
                    resolver { ->
                        AudioPlayer.instance.pause()
                        LocalStorage.audioPlaying = null
                        LocalStorage.audioPlaylist = arrayListOf()
                        sendEvent(ClearAudioPlaylistEvent())
                        true
                    }
                }
                mutation("deletePlaylistAudio") {
                    resolver { path: String ->
                        LocalStorage.deletePlaylistAudio(path)
                        true
                    }
                }
                mutation("saveNote") {
                    resolver { id: ID, input: NoteInput ->
                        val newId = NoteHelper.addOrUpdateAsync(id.value) {
                            title = input.title
                            content = input.content
                        }
                        NoteHelper.getById(newId)?.toModel()
                    }
                }
                mutation("trashNotes") {
                    resolver { ids: List<ID> ->
                        NoteHelper.trashAsync(ids.map { it.value }.toSet())
                        true
                    }
                }
                mutation("untrashNotes") {
                    resolver { ids: List<ID> ->
                        NoteHelper.untrashAsync(ids.map { it.value }.toSet())
                        true
                    }
                }
                mutation("deleteNotes") {
                    resolver { ids: List<ID> ->
                        val newIds = ids.map { it.value }.toSet()
                        TagHelper.deleteTagRelationByKeys(newIds, TagType.NOTE)
                        NoteHelper.deleteAsync(newIds)
                        true
                    }
                }
                mutation("deleteFeedEntries") {
                    resolver { ids: List<ID> ->
                        val newIds = ids.map { it.value }.toSet()
                        TagHelper.deleteTagRelationByKeys(newIds, TagType.FEED_ENTRY)
                        FeedEntryHelper.feedEntryDao.delete(newIds)
                        true
                    }
                }
                mutation("addPlaylistAudios") {
                    resolver { paths: List<String> ->
                        val context = MainApp.instance
                        LocalStorage.addPlaylistAudios(paths.map { DPlaylistAudio.fromPath(context, it) })
                        true
                    }
                }
                mutation("createFeed") {
                    resolver { url: String ->
                        val syndFeed = withIO { FeedHelper.fetchAsync(url) }
                        val id = FeedHelper.addAsync {
                            this.url = url
                            this.name = syndFeed.title
                        }
                        FeedFetchWorker.oneTimeRequest(id)
                        sendEvent(ActionEvent(ActionSourceType.FEED, ActionType.CREATED, setOf(id)))
                        FeedHelper.getById(id)
                    }
                }
                mutation("importFeeds") {
                    resolver { content: String ->
                        FeedHelper.import(StringReader(content))
                        true
                    }
                }
                mutation("exportFeeds") {
                    resolver { ->
                        val writer = StringWriter()
                        FeedHelper.export(writer)
                        writer.toString()
                    }
                }
                mutation("addToTags") {
                    resolver { tagType: TagType, tagIds: List<ID>, items: List<TagRelationStub> ->
                        tagIds.forEach { tagId ->
                            val existingKeys = withIO { TagHelper.getKeysByTagId(tagId.value) }
                            val newItems = items.filter { !existingKeys.contains(it.key) }
                            if (newItems.isNotEmpty()) {
                                TagHelper.addTagRelations(newItems.map {
                                    it.toTagRelation(tagId.value, tagType)
                                })
                            }
                        }
                        true
                    }
                }
                mutation("updateTagRelations") {
                    resolver { tagType: TagType, item: TagRelationStub, addTagIds: List<ID>, removeTagIds: List<ID> ->
                        addTagIds.forEach { tagId ->
                            TagHelper.addTagRelations(arrayOf(item).map {
                                it.toTagRelation(tagId.value, tagType)
                            })
                        }
                        if (removeTagIds.isNotEmpty()) {
                            TagHelper.deleteTagRelationByKeysTagIds(setOf(item.key), removeTagIds.map { it.value }.toSet())
                        }
                        true
                    }
                }
                mutation("removeFromTags") {
                    resolver { tagIds: List<ID>, keys: List<ID> ->
                        TagHelper.deleteTagRelationByKeysTagIds(keys.map { it.value }.toSet(), tagIds.map { it.value }.toSet())
                        true
                    }
                }
                mutation("deleteMediaItems") {
                    resolver { tagType: TagType, ids: List<ID> ->
                        val newIds = ids.map { it.value }.toSet()
                        TagHelper.deleteTagRelationByKeys(newIds, tagType)
                        val context = MainApp.instance
                        when (tagType) {
                            TagType.AUDIO -> {
                                val paths = AudioHelper.deleteRecordsAndFilesByIds(context, newIds)
                                LocalStorage.deletePlaylistAudios(paths)
                            }
                            TagType.VIDEO -> {
                                val paths = VideoHelper.deleteRecordsAndFilesByIds(context, newIds)
                                LocalStorage.deleteVideos(paths)
                            }
                            TagType.IMAGE -> {
                                ImageHelper.deleteRecordsAndFilesByIds(context, newIds)
                            }
                            else -> {
                            }
                        }
                        true
                    }
                }
                mutation("moveFile") {
                    resolver { src: String, dst: String, overwrite: Boolean ->
                        Permission.WRITE_EXTERNAL_STORAGE.check()
                        val dstFile = java.io.File(dst)
                        if (overwrite || !dstFile.exists()) {
                            Path(src).moveTo(Path(dst), overwrite)
                        } else {
                            Path(src).moveTo(Path(dstFile.newPath()), false)
                        }
                        MainApp.instance.scanFileByConnection(src)
                        MainApp.instance.scanFileByConnection(dstFile)
                        true
                    }
                }
                mutation("deleteFeed") {
                    resolver { id: ID ->
                        val newIds = setOf(id.value)
                        val entryIds = FeedEntryHelper.feedEntryDao.getIds(newIds)
                        if (entryIds.isNotEmpty()) {
                            TagHelper.deleteTagRelationByKeys(entryIds.toSet(), TagType.FEED_ENTRY)
                            FeedEntryHelper.feedEntryDao.deleteByFeedIds(newIds)
                        }
                        FeedHelper.deleteAsync(newIds)
                        true
                    }
                }
                mutation("syncFeedContent") {
                    resolver { id: ID ->
                        val feedEntry = FeedEntryHelper.feedEntryDao.getById(id.value)
                        feedEntry?.fetchContentAsync()
                        feedEntry?.toModel()
                    }
                }
                enum<AppTheme>()
                enum<MediaPlayMode>()
                enum<TagType>()
                enum<Permission>()
                stringScalar<Instant> {
                    deserialize = { value: String -> value.toInstant() }
                    serialize = Instant::toString
                }

                stringScalar<ID> {
                    deserialize = { it: String -> ID(it) }
                    serialize = { it: ID -> it.toString() }
                }
            }
        }

        internal var schemaBlock: (SchemaBuilder.() -> Unit)? = null
    }

    companion object Feature : BaseApplicationPlugin<Application, Configuration, SXGraphQL> {
        override val key = AttributeKey<SXGraphQL>("KGraphQL")

        private suspend fun executeGraphqlQL(
            schema: Schema,
            query: String,
            useBoxApi: Boolean
        ): String {
            if (useBoxApi) {
                return BoxProxyApi.executeAsync(query, HttpApiTimeout.MEDIUM_SECONDS)
            }
            val request = Json.decodeFromString(GraphqlRequest.serializer(), query)
            return schema.execute(request.query, request.variables.toString(), context {})
        }

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit
        ): SXGraphQL {
            val config = Configuration().apply(configure)
            val schema = KGraphQL.schema {
                configuration = config
                config.schemaBlock?.invoke(this)
            }

            val routing: Routing.() -> Unit = {
                route("/graphql") {
                    post {
                        if (!LocalStorage.webConsoleEnabled) {
                            call.response.status(HttpStatusCode.BadRequest)
                            return@post
                        }
                        val clientId = call.request.header("c-id") ?: ""
                        val useBoxApi = call.request.header("x-box-api") == "true"
                        if (clientId.isNotEmpty()) {
                            val token = HttpServerManager.tokenCache[clientId]
                            if (token == null) {
                                call.response.status(HttpStatusCode.Unauthorized)
                                return@post
                            }

                            var requestStr = ""
                            val decryptedBytes = CryptoHelper.aesDecrypt(token, call.receive())
                            if (decryptedBytes != null) {
                                requestStr = decryptedBytes.decodeToString()
                            }
                            if (requestStr.isEmpty()) {
                                call.response.status(HttpStatusCode.Unauthorized)
                                return@post
                            }

                            LogCat.d("[Request] $requestStr")
                            HttpServerManager.clientRequestTs[clientId] = System.currentTimeMillis() // record the api request time
                            val r = executeGraphqlQL(schema, requestStr, useBoxApi)
                            call.respondBytes(CryptoHelper.aesEncrypt(token, r))
                        } else {
                            val authStr = call.request.header("authorization")?.split(" ")
                            if (!LocalStorage.authDevTokenEnabled || authStr == null || authStr.size != 2 || authStr[1] != LocalStorage.authDevToken) {
                                call.respondText(
                                    """{"errors":[{"message":"Unauthorized"}]}""",
                                    contentType = ContentType.Application.Json
                                )
                                return@post
                            }

                            val requestStr = call.receiveText()
                            LogCat.d("[Request] $requestStr")
                            HttpServerManager.clientRequestTs[clientId] = System.currentTimeMillis() // record the api request time
                            val r = executeGraphqlQL(schema, requestStr, useBoxApi)
                            call.respondText(r, contentType = ContentType.Application.Json)
                        }
                    }
                }
            }

            pipeline.pluginOrNull(Routing)?.apply(routing) ?: pipeline.install(Routing, routing)

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                try {
                    coroutineScope {
                        proceed()
                    }
                } catch (e: Throwable) {
                    if (e is GraphQLError) {
                        val clientId = call.request.header("c-id") ?: ""
                        if (clientId.isNotEmpty()) {
                            val token = HttpServerManager.tokenCache[clientId]
                            if (token != null) {
                                call.respondBytes(CryptoHelper.aesEncrypt(token, e.serialize()))
                            } else {
                                call.response.status(HttpStatusCode.Unauthorized)
                            }
                        } else {
                            context.respond(HttpStatusCode.OK, e.serialize())
                        }
                    } else throw e
                }
            }
            return SXGraphQL(schema)
        }

        private fun GraphQLError.serialize(): String = buildJsonObject {
            put("errors", buildJsonArray {
                addJsonObject {
                    put("message", message)
                    put("locations", buildJsonArray {
                        locations?.forEach {
                            addJsonObject {
                                put("line", it.line)
                                put("column", it.column)
                            }
                        }
                    })
                    put("path", buildJsonArray {
                        // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                    })
                }
            })
        }.toString()
    }

}
