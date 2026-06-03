package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import com.ismartcoding.plain.web.models.BatteryHealth
import com.ismartcoding.plain.web.models.BatteryPlugged
import com.ismartcoding.plain.web.models.BatteryStatus
import com.ismartcoding.plain.web.models.DocExtGroup
import com.ismartcoding.plain.web.models.ID
import kotlin.time.Instant

fun SchemaBuilder.addSchemaTypes() {
    enum<MediaPlayMode>()
    enum<DataType>()
    enum<DeviceType>()
    enum<Permission>()
    enum<FileSortBy>()
    enum<PomodoroState>()
    enum<ScreenMirrorMode>()
    enum<ScreenMirrorControlAction>()
    enum<DevicePlatform>()
    enum<BatteryHealth>()
    enum<BatteryStatus>()
    enum<BatteryPlugged>()
    stringScalar<Instant> {
        deserialize = { value: String -> Instant.parse(value) }
        serialize = Instant::toString
    }
    stringScalar<ID> {
        deserialize = { it: String -> ID(it) }
        serialize = { it: ID -> it.toString() }
    }
}
