package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import com.ismartcoding.plain.web.models.DocExtGroup
import com.ismartcoding.plain.web.models.ID
import kotlin.time.Instant

fun SchemaBuilder.addSchemaTypes() {
    enum<MediaPlayMode>()
    enum<DataType>()
    enum<Permission>()
    enum<FileSortBy>()
    enum<PomodoroState>()
    enum<ScreenMirrorMode>()
    enum<ScreenMirrorControlAction>()
    type<DocExtGroup>()
    stringScalar<Instant> {
        deserialize = { value: String -> Instant.parse(value) }
        serialize = Instant::toString
    }
    stringScalar<ID> {
        deserialize = { it: String -> ID(it) }
        serialize = { it: ID -> it.toString() }
    }
}
