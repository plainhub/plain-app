package com.ismartcoding.plain.web

import android.os.Build
import android.os.Environment
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
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.extensions.toAppUrl
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.helpers.PhoneHelper
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.CancelNotificationsEvent
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.ClearAudioPlaylistEvent
import com.ismartcoding.plain.features.DeleteChatItemViewEvent
import com.ismartcoding.plain.features.LinkPreviewHelper
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.StartScreenMirrorEvent
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.call.SimHelper
import com.ismartcoding.plain.features.contact.GroupHelper
import com.ismartcoding.plain.features.contact.SourceHelper
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.feed.fetchContentAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.CallMediaStoreHelper
import com.ismartcoding.plain.features.media.ContactMediaStoreHelper
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.SmsMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.DeviceInfoHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.helpers.TempHelper
import com.ismartcoding.plain.preference.ApiPermissionsPreference
import com.ismartcoding.plain.preference.AudioPlayModePreference
import com.ismartcoding.plain.preference.AudioPlayingPreference
import com.ismartcoding.plain.preference.AudioPlaylistPreference
import com.ismartcoding.plain.preference.AudioSortByPreference
import com.ismartcoding.plain.preference.AuthDevTokenPreference
import com.ismartcoding.plain.preference.DeveloperModePreference
import com.ismartcoding.plain.preference.DeviceNamePreference
import com.ismartcoding.plain.preference.ScreenMirrorQualityPreference
import com.ismartcoding.plain.preference.VideoPlaylistPreference
import com.ismartcoding.plain.receivers.BatteryReceiver
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.ui.MainActivity
import com.ismartcoding.plain.web.loaders.FeedsLoader
import com.ismartcoding.plain.web.loaders.FileInfoLoader
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.ActionResult
import com.ismartcoding.plain.web.models.App
import com.ismartcoding.plain.web.models.Audio
import com.ismartcoding.plain.web.models.Call
import com.ismartcoding.plain.web.models.ChatItem
import com.ismartcoding.plain.web.models.Contact
import com.ismartcoding.plain.web.models.ContactGroup
import com.ismartcoding.plain.web.models.ContactInput
import com.ismartcoding.plain.web.models.FeedEntry
import com.ismartcoding.plain.web.models.FileInfo
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.Image
import com.ismartcoding.plain.web.models.MediaFileInfo
import com.ismartcoding.plain.web.models.Message
import com.ismartcoding.plain.web.models.Note
import com.ismartcoding.plain.web.models.NoteInput
import com.ismartcoding.plain.web.models.PackageStatus
import com.ismartcoding.plain.web.models.StorageStats
import com.ismartcoding.plain.web.models.Tag
import com.ismartcoding.plain.web.models.TempValue
import com.ismartcoding.plain.web.models.Video
import com.ismartcoding.plain.web.models.toExportModel
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.websocket.EventType
import com.ismartcoding.plain.web.websocket.WebSocketEvent
import com.ismartcoding.plain.workers.FeedFetchWorker
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import java.io.StringWriter
import kotlin.collections.set
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import java.nio.channels.FileChannel

class SXGraphQL(val schema: Schema) {
    class Configuration : SchemaConfigurationDSL() {
        fun init() {
            val uploadTmpDir = File(MainApp.instance.filesDir, "upload_tmp")
            schemaBlock = {
                query("chatItems") {
                    resolver { ->
                        val dao = AppDatabase.instance.chatDao()
                        var items = dao.getAll()
                        if (!TempData.chatItemsMigrated) {
                            val context = MainApp.instance
                            TempData.chatItemsMigrated = true
                            val types = setOf("app", "storage", "work", "social", "exchange")
                            val ids = items.filter { types.contains(it.content.type) }.map { it.id }
                            if (ids.isNotEmpty()) {
                                dao.deleteByIds(ids)
                                items = items.filter { !types.contains(it.content.type) }
                            }
                            items.filter { setOf(DMessageType.IMAGES.value, DMessageType.FILES.value).contains(it.content.type) }.forEach {
                                if (it.content.value is DMessageImages) {
                                    val c = it.content.value as DMessageImages
                                    if (c.items.any { i -> !i.uri.startsWith("app://") }) {
                                        it.content.value =
                                            DMessageImages(
                                                c.items.map { i ->
                                                    DMessageFile(i.id, i.uri.toAppUrl(context), i.size, i.duration, i.width, i.height)
                                                },
                                            )
                                        dao.update(it)
                                    }
                                } else if (it.content.value is DMessageFiles) {
                                    val c = it.content.value as DMessageFiles
                                    if (c.items.any { i -> !i.uri.startsWith("app://") }) {
                                        it.content.value =
                                            DMessageFiles(
                                                c.items.map { i ->
                                                    DMessageFile(i.id, i.uri.toAppUrl(context), i.size, i.duration, i.width, i.height)
                                                },
                                            )
                                        dao.update(it)
                                    }
                                }
                            }
                        }
                        items.map { it.toModel() }
                    }
                }
                type<ChatItem> {
//                    property(ChatItem::_content) {
//                        ignore = true
//                    }
                    property("data") {
                        resolver { c: ChatItem ->
                            c.getContentData()
                        }
                    }
                }
                query("messages") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        Permission.READ_SMS.checkAsync(MainApp.instance)
                        SmsMediaStoreHelper.searchAsync(MainApp.instance, query, limit, offset).map { it.toModel() }
                    }
                    type<Message> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.SMS)
                            }
                        }
                    }
                }
                query("messageCount") {
                    resolver { query: String ->
                        if (Permission.READ_SMS.can(MainApp.instance)) {
                            SmsMediaStoreHelper.countAsync(MainApp.instance, query)
                        } else {
                            0
                        }
                    }
                }
                query("images") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
                        val context = MainApp.instance
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
                        ImageMediaStoreHelper.searchAsync(context, query, limit, offset, sortBy).map {
                            it.toModel()
                        }
                    }
                    type<Image> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.IMAGE)
                            }
                        }
                    }
                }
                query("imageCount") {
                    resolver { query: String ->
                        val context = MainApp.instance
                        if (Permission.WRITE_EXTERNAL_STORAGE.can(context)) {
                            ImageMediaStoreHelper.countAsync(context, query)
                        } else {
                            0
                        }
                    }
                }
                query("mediaBuckets") {
                    resolver { type: DataType ->
                        val context = MainApp.instance
                        if (Permission.WRITE_EXTERNAL_STORAGE.can(context)) {
                            if (type == DataType.IMAGE) {
                                ImageMediaStoreHelper.getBucketsAsync(context).map { it.toModel() }
                            } else if (type == DataType.AUDIO) {
                                if (isQPlus()) {
                                    AudioMediaStoreHelper.getBucketsAsync(context).map { it.toModel() }
                                } else {
                                    emptyList()
                                }
                            } else if (type == DataType.VIDEO) {
                                VideoMediaStoreHelper.getBucketsAsync(context).map { it.toModel() }
                            } else {
                                emptyList()
                            }
                        } else {
                            emptyList()
                        }
                    }
                }
                query("videos") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
                        val context = MainApp.instance
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
                        VideoMediaStoreHelper.searchAsync(context, query, limit, offset, sortBy).map {
                            it.toModel()
                        }
                    }
                    type<Video> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.VIDEO)
                            }
                        }
                    }
                }
                query("videoCount") {
                    resolver { query: String ->
                        if (Permission.WRITE_EXTERNAL_STORAGE.can(MainApp.instance)) {
                            VideoMediaStoreHelper.countAsync(MainApp.instance, query)
                        } else {
                            0
                        }
                    }
                }
                query("audios") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
                        val context = MainApp.instance
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
                        AudioMediaStoreHelper.searchAsync(context, query, limit, offset, sortBy).map {
                            it.toModel()
                        }
                    }
                    type<Audio> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.AUDIO)
                            }
                        }
                    }
                }
                query("audioCount") {
                    resolver { query: String ->
                        if (Permission.WRITE_EXTERNAL_STORAGE.can(MainApp.instance)) {
                            AudioMediaStoreHelper.countAsync(MainApp.instance, query)
                        } else {
                            0
                        }
                    }
                }
                query("contacts") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        val context = MainApp.instance
                        Permissions.checkAsync(context, setOf(Permission.READ_CONTACTS))
                        try {
                            ContactMediaStoreHelper.searchAsync(context, query, limit, offset).map { it.toModel() }
                        } catch (ex: Exception) {
                            LogCat.e(ex)
                            emptyList()
                        }
                    }
                    type<Contact> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.CONTACT)
                            }
                        }
                    }
                }
                query("contactCount") {
                    resolver { query: String ->
                        val context = MainApp.instance
                        if (Permission.READ_CONTACTS.can(context)) {
                            ContactMediaStoreHelper.countAsync(context, query)
                        } else {
                            0
                        }
                    }
                }
                query("contactSources") {
                    resolver { ->
                        Permissions.checkAsync(MainApp.instance, setOf(Permission.READ_CONTACTS))
                        SourceHelper.getAll().map { it.toModel() }
                    }
                }
                query("contactGroups") {
                    resolver { node: Execution.Node ->
                        Permissions.checkAsync(MainApp.instance, setOf(Permission.READ_CONTACTS))
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
                        Permissions.checkAsync(MainApp.instance, setOf(Permission.READ_CALL_LOG))
                        CallMediaStoreHelper.searchAsync(MainApp.instance, query, limit, offset).map { it.toModel() }
                    }
                    type<Call> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.CALL)
                            }
                        }
                    }
                }
                query("callCount") {
                    resolver { query: String ->
                        val context = MainApp.instance
                        if (Permission.READ_CALL_LOG.can(context)) {
                            CallMediaStoreHelper.countAsync(context, query)
                        } else {
                            0
                        }
                    }
                }
                query("sims") {
                    resolver { ->
                        SimHelper.getAll().map { it.toModel() }
                    }
                }
                query("packages") {
                    resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
                        PackageHelper.searchAsync(query, limit, offset, sortBy).map { it.toModel() }
                    }
                }
                query("packageStatuses") {
                    resolver { ids: List<ID> ->
                        PackageHelper.getPackageStatuses(ids.map { it.value }).map { PackageStatus(ID(it.key), it.value) }
                    }
                }
                query("packageCount") {
                    resolver { query: String ->
                        PackageHelper.count(query)
                    }
                }
                query("storageStats") {
                    resolver { ->
                        val context = MainApp.instance
                        StorageStats(
                            FileSystemHelper.getInternalStorageStats().toModel(),
                            FileSystemHelper.getSDCardStorageStats(context).toModel(),
                            FileSystemHelper.getUSBStorageStats().map { it.toModel() },
                        )
                    }
                }
                query("screenMirrorState") {
                    resolver { ->
                        val image = ScreenMirrorService.instance?.getLatestImage()
                        if (image != null) {
                            sendEvent(WebSocketEvent(EventType.SCREEN_MIRRORING, image))
                            true
                        } else {
                            false
                        }
                    }
                }
                query("screenMirrorQuality") {
                    resolver { ->
                        ScreenMirrorQualityPreference.getValueAsync(MainApp.instance).toModel()
                    }
                }
                query("recentFiles") {
                    resolver { ->
                        val context = MainApp.instance
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
                        if (isQPlus()) {
                            FileMediaStoreHelper.getRecentFilesAsync(context).map { it.toModel() }
                        } else {
                            FileSystemHelper.getRecentFiles().map { it.toModel() }
                        }
                    }
                }
                query("files") {
                    resolver { root: String, offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
                        val context = MainApp.instance
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
//                        val appFolder = context.getExternalFilesDir(null)?.path ?: ""
//                        val internalPath = FileSystemHelper.getInternalStoragePath()
                        //   if (!isQPlus() || root.startsWith(appFolder) || !root.startsWith(internalPath)) {
                        val filterFields = QueryHelper.parseAsync(query)
                        val showHidden = filterFields.find { it.name == "show_hidden" }?.value?.toBoolean() ?: false
                        val text = filterFields.find { it.name == "text" }?.value ?: ""
                        val parent = filterFields.find { it.name == "parent" }?.value ?: ""
                        if (text.isNotEmpty()) {
                            FileSystemHelper.search(text, parent.ifEmpty { root }, showHidden).sorted(sortBy).drop(offset).take(limit).map { it.toModel() }
                        } else {
                            FileSystemHelper.getFilesList(parent.ifEmpty { root }, showHidden, sortBy).drop(offset).take(limit).map { it.toModel() }
                        }
//                        } else {
//                            FileMediaStoreHelper.searchAsync(MainApp.instance, query, limit, offset, sortBy).map { it.toModel() }
//                        }
                    }
                }
                query("fileInfo") {
                    resolver { id: ID, path: String ->
                        val context = MainApp.instance
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
                        val finalPath = path.getFinalPath(context)
                        val file = File(finalPath)
                        val updatedAt = Instant.fromEpochMilliseconds(file.lastModified())
                        var tags = emptyList<Tag>()
                        var data: MediaFileInfo? = null
                        if (finalPath.isImageFast()) {
                            if (id.value.isNotEmpty()) {
                                tags = TagsLoader.load(id.value, DataType.IMAGE)
                            }
                            data = FileInfoLoader.loadImage(finalPath)
                        } else if (finalPath.isVideoFast()) {
                            if (id.value.isNotEmpty()) {
                                tags = TagsLoader.load(id.value, DataType.VIDEO)
                            }
                            data = FileInfoLoader.loadVideo(context, finalPath)
                        } else if (finalPath.isAudioFast()) {
                            if (id.value.isNotEmpty()) {
                                tags = TagsLoader.load(id.value, DataType.AUDIO)
                            }
                            data = FileInfoLoader.loadAudio(context, finalPath)
                        }
                        FileInfo(path, updatedAt, size = file.length(), tags, data)
                    }
                }
                query("tags") {
                    resolver { type: DataType ->
                        val tagCountMap = TagHelper.count(type).associate { it.id to it.count }
                        TagHelper.getAll(type).map {
                            it.count = tagCountMap[it.id] ?: 0
                            it.toModel()
                        }
                    }
                }
                query("tagRelations") {
                    resolver { type: DataType, keys: List<String> ->
                        TagHelper.getTagRelationsByKeys(keys.toSet(), type).map { it.toModel() }
                    }
                }
                query("notifications") {
                    resolver { ->
                        val context = MainApp.instance
                        Permission.NOTIFICATION_LISTENER.checkAsync(context)
                        TempData.notifications.sortedByDescending { it.time }.map { it.toModel() }
                    }
                }
                query("feeds") {
                    resolver { ->
                        val items = FeedHelper.getAll()
                        items.map { it.toModel() }
                    }
                }
                query("feedsCount") {
                    resolver { ->
                        FeedHelper.getFeedCounts().map { it.toModel() }
                    }
                }
                query("feedEntries") {
                    configure {
                        executor = Executor.DataLoaderPrepared
                    }
                    resolver { offset: Int, limit: Int, query: String ->
                        val items = FeedEntryHelper.search(query, limit, offset)
                        items.map { it.toModel() }
                    }
                    type<FeedEntry> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.FEED_ENTRY)
                            }
                        }
                        dataProperty("feed") {
                            prepare { item -> item.feedId }
                            loader { ids ->
                                FeedsLoader.load(ids)
                            }
                        }
                    }
                }
                query("feedEntryCount") {
                    resolver { query: String ->
                        FeedEntryHelper.count(query)
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
                        val items = NoteHelper.search(query, limit, offset)
                        items.map { it.toModel() }
                    }
                    type<Note> {
                        dataProperty("tags") {
                            prepare { item -> item.id.value }
                            loader { ids ->
                                TagsLoader.load(ids, DataType.NOTE)
                            }
                        }
                    }
                }
                query("noteCount") {
                    resolver { query: String ->
                        NoteHelper.count(query)
                    }
                }
                query("note") {
                    resolver { id: ID ->
                        val data = NoteHelper.getById(id.value)
                        data?.toModel()
                    }
                }
                query("deviceInfo") {
                    resolver { ->
                        val context = MainApp.instance
                        val apiPermissions = ApiPermissionsPreference.getAsync(context)
                        val readPhoneNumber = apiPermissions.contains(Permission.READ_PHONE_NUMBERS.toString())
                        DeviceInfoHelper.getDeviceInfo(context, readPhoneNumber).toModel()
                    }
                }
                query("battery") {
                    resolver { ->
                        BatteryReceiver.get(MainApp.instance).toModel()
                    }
                }
                query("app") {
                    resolver { ->
                        val context = MainApp.instance
                        val apiPermissions = ApiPermissionsPreference.getAsync(context)
                        App(
                            usbConnected = PlugInControlReceiver.isUSBConnected(context),
                            urlToken = TempData.urlToken,
                            httpPort = TempData.httpPort,
                            httpsPort = TempData.httpsPort,
                            externalFilesDir = context.getExternalFilesDir(null)?.path ?: "",
                            deviceName = DeviceNamePreference.getAsync(context).ifEmpty { PhoneHelper.getDeviceName(context) },
                            PhoneHelper.getBatteryPercentage(context),
                            BuildConfig.VERSION_CODE,
                            Build.VERSION.SDK_INT,
                            BuildConfig.CHANNEL,
                            Permission.entries.filter { apiPermissions.contains(it.name) && it.can(MainApp.instance) },
                            AudioPlaylistPreference.getValueAsync(context).map { it.toModel() },
                            TempData.audioPlayMode,
                            AudioPlayingPreference.getValueAsync(context),
                            sdcardPath = FileSystemHelper.getSDCardPath(context),
                            usbDiskPaths = FileSystemHelper.getUsbDiskPaths(),
                            internalStoragePath = FileSystemHelper.getInternalStoragePath(),
                            downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                            developerMode = DeveloperModePreference.getAsync(context),
                        )
                    }
                }
                query("fileIds") {
                    resolver { paths: List<String> ->
                        paths.map { FileHelper.getFileId(it) }
                    }
                }
                mutation("setTempValue") {
                    resolver { key: String, value: String ->
                        TempHelper.setValue(key, value)
                        TempValue(key, value)
                    }
                }
                mutation("uninstallPackages") {
                    resolver { ids: List<ID> ->
                        ids.forEach {
                            PackageHelper.uninstall(MainActivity.instance.get()!!, it.value)
                        }
                        true
                    }
                }
                mutation("cancelNotifications") {
                    resolver { ids: List<ID> ->
                        sendEvent(CancelNotificationsEvent(ids.map { it.value }.toSet()))
                        true
                    }
                }
                mutation("createChatItem") {
                    resolver { content: String ->
                        var item =
                            ChatHelper.sendAsync(
                                DChat.parseContent(content),
                            )
                        val urls = LinkPreviewHelper.extractUrls(content)
                        if (urls.isNotEmpty()) {
                            val context = MainApp.instance
                            val links = ChatHelper.fetchAndUpdateLinkPreviewsAsync(context, item, urls)
                            if (links.isNotEmpty()) {
                                item = AppDatabase.instance.chatDao().getById(item.id) ?: item
                            }
                        }
                        sendEvent(HttpServerEvents.MessageCreatedEvent(arrayListOf(item)))
                        arrayListOf(item).map { it.toModel() }
                    }
                }
                mutation("deleteChatItem") {
                    resolver { id: ID ->
                        val item = ChatHelper.getAsync(id.value)
                        if (item != null) {
                            ChatHelper.deleteAsync(MainApp.instance, item.id, item.content.value)
                            sendEvent(DeleteChatItemViewEvent(item.id))
                        }
                        true
                    }
                }
                mutation("relaunchApp") {
                    resolver { ->
                        coIO {
                            AppHelper.relaunch(MainApp.instance)
                        }
                        true
                    }
                }
                mutation("deleteContacts") {
                    resolver { query: String ->
                        val context = MainApp.instance
                        Permission.WRITE_CONTACTS.checkAsync(context)
                        val newIds = ContactMediaStoreHelper.getIdsAsync(context, query)
                        TagHelper.deleteTagRelationByKeys(newIds, DataType.CONTACT)
                        ContactMediaStoreHelper.deleteByIdsAsync(context, newIds)
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
                        Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
                        ContactMediaStoreHelper.updateAsync(id.value, input)
                        ContactMediaStoreHelper.getByIdAsync(MainApp.instance, id.value)?.toModel()
                    }
                }
                mutation("createContact") {
                    resolver { input: ContactInput ->
                        Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
                        val id = ContactMediaStoreHelper.createAsync(input)
                        if (id.isEmpty()) null else ContactMediaStoreHelper.getByIdAsync(MainApp.instance, id)?.toModel()
                    }
                }
                mutation("createTag") {
                    resolver { type: DataType, name: String ->
                        val id =
                            TagHelper.addOrUpdate("") {
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
                    resolver { id: ID, name: String, fetchContent: Boolean ->
                        FeedHelper.updateAsync(id.value) {
                            this.name = name
                            this.fetchContent = fetchContent
                        }
                        FeedHelper.getById(id.value)?.toModel()
                    }
                }
                mutation("startScreenMirror") {
                    resolver { ->
                        ScreenMirrorService.qualityData = ScreenMirrorQualityPreference.getValueAsync(MainApp.instance)
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
                mutation("updateScreenMirrorQuality") {
                    resolver { quality: Int, resolution: Int ->
                        val qualityData = DScreenMirrorQuality(quality, resolution)
                        ScreenMirrorQualityPreference.putAsync(MainApp.instance, qualityData)
                        ScreenMirrorService.qualityData = qualityData
                        true
                    }
                }
                mutation("createContactGroup") {
                    resolver { name: String, accountName: String, accountType: String ->
                        Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
                        GroupHelper.create(name, accountName, accountType).toModel()
                    }
                }

                mutation("call") {
                    resolver { number: String ->
                        Permission.CALL_PHONE.checkAsync(MainApp.instance)
                        CallMediaStoreHelper.call(MainActivity.instance.get()!!, number)
                        true
                    }
                }
                mutation("updateContactGroup") {
                    resolver { id: ID, name: String ->
                        Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
                        GroupHelper.update(id.value, name)
                        ContactGroup(id, name)
                    }
                }
                mutation("deleteContactGroup") {
                    resolver { id: ID ->
                        Permission.WRITE_CONTACTS.checkAsync(MainApp.instance)
                        GroupHelper.delete(id.value)
                        true
                    }
                }

                mutation("deleteCalls") {
                    resolver { query: String ->
                        val context = MainApp.instance
                        Permission.WRITE_CALL_LOG.checkAsync(context)
                        val newIds = CallMediaStoreHelper.getIdsAsync(context, query)
                        TagHelper.deleteTagRelationByKeys(newIds, DataType.CALL)
                        CallMediaStoreHelper.deleteByIdsAsync(context, newIds)
                        true
                    }
                }
                mutation("deleteFiles") {
                    resolver { paths: List<String> ->
                        val context = MainApp.instance
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
                        paths.forEach {
                            java.io.File(it).deleteRecursively()
                        }
                        context.scanFileByConnection(paths.toTypedArray())
                        true
                    }
                }
                mutation("createDir") {
                    resolver { path: String ->
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
                        FileSystemHelper.createDirectory(path).toModel()
                    }
                }
                mutation("renameFile") {
                    resolver { path: String, name: String ->
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
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
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
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
                        val context = MainApp.instance
                        val audio = DPlaylistAudio.fromPath(context, path)
                        AudioPlayingPreference.putAsync(context, audio.path)
                        if (!AudioPlaylistPreference.getValueAsync(context).any { it.path == audio.path }) {
                            AudioPlaylistPreference.addAsync(context, listOf(audio))
                        }
                        audio.toModel()
                    }
                }
                mutation("updateAudioPlayMode") {
                    resolver { mode: MediaPlayMode ->
                        AudioPlayModePreference.putAsync(MainApp.instance, mode)
                        true
                    }
                }
                mutation("clearAudioPlaylist") {
                    resolver { ->
                        val context = MainApp.instance
                        AudioPlayingPreference.putAsync(context, "")
                        AudioPlaylistPreference.putAsync(context, arrayListOf())
                        coMain {
                            AudioPlayer.clear()
                        }
                        sendEvent(ClearAudioPlaylistEvent())
                        true
                    }
                }
                mutation("deletePlaylistAudio") {
                    resolver { path: String ->
                        AudioPlaylistPreference.deleteAsync(MainApp.instance, setOf(path))
                        true
                    }
                }
                mutation("saveNote") {
                    resolver { id: ID, input: NoteInput ->
                        val item =
                            NoteHelper.addOrUpdateAsync(id.value) {
                                title = input.title
                                content = input.content
                            }
                        NoteHelper.getById(item.id)?.toModel()
                    }
                }
                mutation("saveFeedEntriesToNotes") {
                    resolver { query: String ->
                        val entries = FeedEntryHelper.search(query, Int.MAX_VALUE, 0)
                        val ids = mutableListOf<String>()
                        entries.forEach { m ->
                            val c = "# ${m.title}\n\n" + m.content.ifEmpty { m.description }
                            NoteHelper.saveToNotesAsync(m.id) {
                                title = c.cut(250).replace("\n", "")
                                content = c
                            }
                            ids.add(m.id)
                        }
                        NoteHelper.search("ids:${ids.joinToString(",")}", Int.MAX_VALUE, 0).map { it.toModel() }
                    }
                }
                mutation("trashNotes") {
                    resolver { query: String ->
                        val ids = NoteHelper.getIdsAsync(query)
                        TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
                        NoteHelper.trashAsync(ids)
                        query
                    }
                }
                mutation("restoreNotes") {
                    resolver { query: String ->
                        val ids = NoteHelper.getTrashedIdsAsync(query)
                        NoteHelper.restoreAsync(ids)
                        query
                    }
                }
                mutation("deleteNotes") {
                    resolver { query: String ->
                        val ids = NoteHelper.getTrashedIdsAsync(query)
                        TagHelper.deleteTagRelationByKeys(ids, DataType.NOTE)
                        NoteHelper.deleteAsync(ids)
                        query
                    }
                }
                mutation("deleteFeedEntries") {
                    resolver { query: String ->
                        val ids = FeedEntryHelper.getIdsAsync(query)
                        TagHelper.deleteTagRelationByKeys(ids, DataType.FEED_ENTRY)
                        FeedEntryHelper.deleteAsync(ids)
                        query
                    }
                }
                mutation("addPlaylistAudios") {
                    resolver { query: String ->
                        val context = MainApp.instance
                        // 1000 items at most
                        val items = AudioMediaStoreHelper.searchAsync(context, query, 1000, 0, AudioSortByPreference.getValueAsync(context))
                        AudioPlaylistPreference.addAsync(context, items.map { it.toPlaylistAudio() })
                        true
                    }
                }
                mutation("reorderPlaylistAudios") {
                    resolver { paths: List<String> ->
                        val context = MainApp.instance

                        // Get current playlist
                        val currentPlaylist = AudioPlaylistPreference.getValueAsync(context)
                        if (currentPlaylist.isEmpty() || paths.isEmpty()) {
                            return@resolver true
                        }

                        // Create a map of paths to audio items
                        val audioMap = currentPlaylist.associateBy { it.path }

                        // Reorder the playlist based on the provided paths
                        val reorderedPlaylist = mutableListOf<DPlaylistAudio>()

                        // First add audio items in the new order
                        paths.forEach { path ->
                            audioMap[path]?.let { audio ->
                                reorderedPlaylist.add(audio)
                            }
                        }

                        // Add other audio items that are not in the reorder list (keep their original positions)
                        currentPlaylist.forEach { audio ->
                            if (!paths.contains(audio.path)) {
                                reorderedPlaylist.add(audio)
                            }
                        }

                        // Save the reordered playlist
                        AudioPlaylistPreference.putAsync(context, reorderedPlaylist)

                        true
                    }
                }
                mutation("createFeed") {
                    resolver { url: String, fetchContent: Boolean ->
                        val syndFeed = withIO { FeedHelper.fetchAsync(url) }
                        val id =
                            FeedHelper.addAsync {
                                this.url = url
                                this.name = syndFeed.title ?: ""
                                this.fetchContent = fetchContent
                            }
                        FeedFetchWorker.oneTimeRequest(id)
                        FeedHelper.getById(id)
                    }
                }
                mutation("importFeeds") {
                    resolver { content: String ->
                        FeedHelper.importAsync(StringReader(content))
                        true
                    }
                }
                mutation("exportFeeds") {
                    resolver { ->
                        val writer = StringWriter()
                        FeedHelper.exportAsync(writer)
                        writer.toString()
                    }
                }
                mutation("exportNotes") {
                    resolver { query: String ->
                        val items = NoteHelper.search(query, Int.MAX_VALUE, 0)
                        val keys = items.map { it.id }
                        val allTags = TagHelper.getAll(DataType.NOTE)
                        val map = TagHelper.getTagRelationsByKeys(keys.toSet(), DataType.NOTE).groupBy { it.key }
                        jsonEncode(items.map {
                            val tagIds = map[it.id]?.map { t -> t.tagId } ?: emptyList()
                            it.toExportModel(if (tagIds.isNotEmpty()) allTags.filter { tagIds.contains(it.id) }.map { t -> t.toModel() } else emptyList())
                        })
                    }
                }
                mutation("addToTags") {
                    resolver { type: DataType, tagIds: List<ID>, query: String ->
                        var items = listOf<TagRelationStub>()
                        val context = MainApp.instance
                        when (type) {
                            DataType.AUDIO -> {
                                items = AudioMediaStoreHelper.getTagRelationStubsAsync(context, query)
                            }

                            DataType.VIDEO -> {
                                items = VideoMediaStoreHelper.getTagRelationStubsAsync(context, query)
                            }

                            DataType.IMAGE -> {
                                items = ImageMediaStoreHelper.getTagRelationStubsAsync(context, query)
                            }

                            DataType.SMS -> {
                                items = SmsMediaStoreHelper.getIdsAsync(context, query).map { TagRelationStub(it) }
                            }

                            DataType.CONTACT -> {
                                items = ContactMediaStoreHelper.getIdsAsync(context, query).map { TagRelationStub(it) }
                            }

                            DataType.NOTE -> {
                                items = NoteHelper.getIdsAsync(query).map { TagRelationStub(it) }
                            }

                            DataType.FEED_ENTRY -> {
                                items = FeedEntryHelper.getIdsAsync(query).map { TagRelationStub(it) }
                            }

                            DataType.CALL -> {
                                items = CallMediaStoreHelper.getIdsAsync(context, query).map { TagRelationStub(it) }
                            }

                            else -> {}
                        }

                        tagIds.forEach { tagId ->
                            val existingKeys = withIO { TagHelper.getKeysByTagId(tagId.value) }
                            val newItems = items.filter { !existingKeys.contains(it.key) }
                            if (newItems.isNotEmpty()) {
                                TagHelper.addTagRelations(
                                    newItems.map {
                                        it.toTagRelation(tagId.value, type)
                                    },
                                )
                            }
                        }
                        true
                    }
                }
                mutation("updateTagRelations") {
                    resolver { type: DataType, item: TagRelationStub, addTagIds: List<ID>, removeTagIds: List<ID> ->
                        addTagIds.forEach { tagId ->
                            TagHelper.addTagRelations(
                                arrayOf(item).map {
                                    it.toTagRelation(tagId.value, type)
                                },
                            )
                        }
                        if (removeTagIds.isNotEmpty()) {
                            TagHelper.deleteTagRelationByKeysTagIds(setOf(item.key), removeTagIds.map { it.value }.toSet())
                        }
                        true
                    }
                }
                mutation("removeFromTags") {
                    resolver { type: DataType, tagIds: List<ID>, query: String ->
                        val context = MainApp.instance
                        var ids = setOf<String>()
                        when (type) {
                            DataType.AUDIO -> {
                                ids = AudioMediaStoreHelper.getIdsAsync(context, query)
                            }

                            DataType.VIDEO -> {
                                ids = VideoMediaStoreHelper.getIdsAsync(context, query)
                            }

                            DataType.IMAGE -> {
                                ids = ImageMediaStoreHelper.getIdsAsync(context, query)
                            }

                            DataType.SMS -> {
                                ids = SmsMediaStoreHelper.getIdsAsync(context, query)
                            }

                            DataType.CONTACT -> {
                                ids = ContactMediaStoreHelper.getIdsAsync(context, query)
                            }

                            DataType.NOTE -> {
                                ids = NoteHelper.getIdsAsync(query)
                            }

                            DataType.FEED_ENTRY -> {
                                ids = FeedEntryHelper.getIdsAsync(query)
                            }

                            DataType.CALL -> {
                                ids = CallMediaStoreHelper.getIdsAsync(context, query)
                            }

                            else -> {}
                        }

                        TagHelper.deleteTagRelationByKeysTagIds(ids, tagIds.map { it.value }.toSet())
                        true
                    }
                }
                mutation("deleteMediaItems") {
                    resolver { type: DataType, query: String ->
                        val ids: Set<String>
                        val context = MainApp.instance
                        val hasTrashFeature = AppFeatureType.MEDIA_TRASH.has()
                        when (type) {
                            DataType.AUDIO -> {
                                ids = if (hasTrashFeature) AudioMediaStoreHelper.getTrashedIdsAsync(context, query) else AudioMediaStoreHelper.getIdsAsync(context, query)
                                AudioMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, true)
                            }

                            DataType.VIDEO -> {
                                ids = if (hasTrashFeature) VideoMediaStoreHelper.getTrashedIdsAsync(context, query) else VideoMediaStoreHelper.getIdsAsync(context, query)
                                VideoMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, true)
                            }

                            DataType.IMAGE -> {
                                ids = if (hasTrashFeature) ImageMediaStoreHelper.getTrashedIdsAsync(context, query) else ImageMediaStoreHelper.getIdsAsync(context, query)
                                ImageMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, true)
                            }

                            else -> {
                            }
                        }
                        ActionResult(type, query)
                    }
                }
                mutation("trashMediaItems") {
                    resolver { type: DataType, query: String ->
                        if (!isRPlus()) {
                            return@resolver ActionResult(type, query)
                        }

                        var ids = setOf<String>()
                        val context = MainApp.instance
                        when (type) {
                            DataType.AUDIO -> {
                                ids = AudioMediaStoreHelper.getIdsAsync(context, query)
                                val paths = AudioMediaStoreHelper.getPathsByIdsAsync(context, ids)
                                AudioMediaStoreHelper.trashByIdsAsync(context, ids)
                                AudioPlaylistPreference.deleteAsync(context, paths)
                            }

                            DataType.VIDEO -> {
                                ids = VideoMediaStoreHelper.getIdsAsync(context, query)
                                val paths = VideoMediaStoreHelper.getPathsByIdsAsync(context, ids)
                                VideoMediaStoreHelper.trashByIdsAsync(context, ids)
                                VideoPlaylistPreference.deleteAsync(context, paths)
                            }

                            DataType.IMAGE -> {
                                ids = ImageMediaStoreHelper.getIdsAsync(context, query)
                                ImageMediaStoreHelper.trashByIdsAsync(context, ids)
                            }

                            else -> {
                            }
                        }
                        TagHelper.deleteTagRelationByKeys(ids, type)
                        ActionResult(type, query)
                    }
                }
                mutation("restoreMediaItems") {
                    resolver { type: DataType, query: String ->
                        if (!isRPlus()) {
                            return@resolver ActionResult(type, query)
                        }

                        val ids: Set<String>
                        val context = MainApp.instance
                        when (type) {
                            DataType.AUDIO -> {
                                ids = AudioMediaStoreHelper.getTrashedIdsAsync(context, query)
                                AudioMediaStoreHelper.restoreByIdsAsync(context, ids)
                            }

                            DataType.VIDEO -> {
                                ids = VideoMediaStoreHelper.getTrashedIdsAsync(context, query)
                                VideoMediaStoreHelper.restoreByIdsAsync(context, ids)
                            }

                            DataType.IMAGE -> {
                                ids = ImageMediaStoreHelper.getTrashedIdsAsync(context, query)
                                ImageMediaStoreHelper.restoreByIdsAsync(context, ids)
                            }

                            else -> {
                            }
                        }
                        ActionResult(type, query)
                    }
                }
                mutation("moveFile") {
                    resolver { src: String, dst: String, overwrite: Boolean ->
                        Permission.WRITE_EXTERNAL_STORAGE.checkAsync(MainApp.instance)
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
                            TagHelper.deleteTagRelationByKeys(entryIds.toSet(), DataType.FEED_ENTRY)
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
                query("uploadedChunks") {
                    resolver { fileId: String ->
                        val chunkDir = File(uploadTmpDir, fileId)
                        if (!chunkDir.exists()) return@resolver emptyList<Int>()

                        chunkDir.listFiles()
                            ?.mapNotNull { it.name.removePrefix("chunk_").toIntOrNull() }
                            ?.sorted()
                            ?: emptyList()
                    }
                }
                mutation("mergeChunks") {
                    resolver { fileId: String, totalChunks: Int, path: String, replace: Boolean ->
                        val chunkDir = File(uploadTmpDir, fileId)
                        if (!chunkDir.exists()) {
                            throw GraphQLError("No chunks found for $fileId")
                        }

                        val outputFile = if (replace) {
                            File(path)
                        } else {
                            val originalFile = File(path)
                            if (originalFile.exists()) {
                                File(originalFile.newPath())
                            } else {
                                originalFile
                            }
                        }
                        outputFile.parentFile?.mkdirs()

                        outputFile.outputStream().use { outputStream ->
                            val outputChannel = outputStream.channel
                            for (i in 0 until totalChunks) {
                                val chunkFile = File(chunkDir, "chunk_$i")
                                if (!chunkFile.exists()) {
                                    throw GraphQLError("Missing chunk $i")
                                }

                                chunkFile.inputStream().channel.use { inputChannel ->
                                    var position = 0L
                                    val size = inputChannel.size()
                                    while (position < size) {
                                        val transferred = inputChannel.transferTo(position, size - position, outputChannel)
                                        if (transferred <= 0) break
                                        position += transferred
                                    }
                                }
                            }
                        }
                        chunkDir.deleteRecursively()
                        MainApp.instance.scanFileByConnection(outputFile, null)
                        outputFile.absolutePath
                    }
                }
                enum<MediaPlayMode>()
                enum<DataType>()
                enum<Permission>()
                enum<FileSortBy>()
                stringScalar<Instant> {
                    deserialize = { value: String -> Instant.parse(value) }
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
        ): String {
            val request = Json.decodeFromString(GraphqlRequest.serializer(), query)
            return schema.execute(request.query, request.variables.toString(), context {})
        }

        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit,
        ): SXGraphQL {
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

                            var requestStr = ""
                            val decryptedBytes = CryptoHelper.aesDecrypt(token, call.receive())
                            if (decryptedBytes != null) {
                                requestStr = decryptedBytes.decodeToString()
                            }
                            if (requestStr.isEmpty()) {
                                call.respond(HttpStatusCode.Unauthorized)
                                return@post
                            }

                            LogCat.d("[Request] $requestStr")
                            HttpServerManager.clientRequestTs[clientId] = System.currentTimeMillis() // record the api request time
                            val r = executeGraphqlQL(schema, requestStr)
                            call.respondBytes(CryptoHelper.aesEncrypt(token, r))
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
                            val r = executeGraphqlQL(schema, requestStr)
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
                        if (clientId.isNotEmpty()) {
                            val token = HttpServerManager.tokenCache[clientId]
                            if (token != null) {
                                call.respondBytes(CryptoHelper.aesEncrypt(token, e.serialize()))
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
            return SXGraphQL(schema)
        }

        private fun GraphQLError.serialize(): String =
            buildJsonObject {
                put(
                    "errors",
                    buildJsonArray {
                        addJsonObject {
                            put("message", message)
                            put(
                                "locations",
                                buildJsonArray {
                                    locations?.forEach {
                                        addJsonObject {
                                            put("line", it.line)
                                            put("column", it.column)
                                        }
                                    }
                                },
                            )
                            put(
                                "path",
                                buildJsonArray {
                                    // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                                },
                            )
                        }
                    },
                )
            }.toString()
    }
}
