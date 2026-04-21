package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import com.ismartcoding.plain.web.models.ChatChannel
import com.ismartcoding.plain.web.models.ChatChannelMember
import com.ismartcoding.plain.web.models.ChatItem
import com.ismartcoding.plain.web.models.Peer
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addChatQuerySchema() {
    query("chatItems") {
        resolver { id: String ->
            val dao = AppDatabase.instance.chatDao()
            val items = if (id.startsWith("channel:")) {
                dao.getByChannelId(id.removePrefix("channel:"))
            } else {
                dao.getByChatId(id.replace("peer:", ""))
            }
            items.map { it.toModel() }
        }
    }
    query("chatChannels") {
        resolver { ->
            AppDatabase.instance.chatChannelDao().getAll()
                .sortedBy { it.name.lowercase() }
                .map { it.toModel() }
        }
    }
    query("peers") {
        resolver { ->
            AppDatabase.instance.peerDao().getAll().map { it.toModel() }
        }
    }
    query("appFiles") {
        resolver { offset: Int, limit: Int ->
            val dao = AppDatabase.instance.appFileDao()
            val chatDao = AppDatabase.instance.chatDao()
            val files = dao.getPage(limit, offset)
            val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
            files.map { it.toModel(AppFileDisplayNameHelper.resolveDisplayName(it, nameMap)) }
        }
    }
    query("appFileCount") {
        resolver { ->
            AppDatabase.instance.appFileDao().count()
        }
    }
    type<Peer> {}
    type<ChatChannel> {}
    type<ChatChannelMember> {}
    type<ChatItem> {
        property("data") {
            resolver { c: ChatItem ->
                c.getContentData()
            }
        }
    }
}
