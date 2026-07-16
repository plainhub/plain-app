package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder

/**
 * Shared GraphQL schema definition. Called by both the Android (Ktor) and iOS
 * (SwiftNIO, future) HTTP server entry points to assemble the full API schema.
 */
fun SchemaBuilder.applyMainSchema() {
    addChatMessageSchema()
    addChatChannelSchema()
    addSmsSchema()
    addImageSchema()
    addAudioSchema()
    addVideoSchema()
    addMediaSchema()
    addDocSchema()
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
    addAppFileSchema()
    addAppLogsSchema()
    addDataStoreSchema()
    addDbSchema()
    addDiscoverSchema()
    addBookmarkSchema()
    addPairingSchema()
    addPeerSchema()
    addSchemaTypes()
}
