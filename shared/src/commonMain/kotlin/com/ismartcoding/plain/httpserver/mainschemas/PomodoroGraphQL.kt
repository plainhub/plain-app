package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.preferences.*
import com.ismartcoding.plain.features.PomodoroHelper
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.httpserver.models.PomodoroSettings
import com.ismartcoding.plain.httpserver.models.PomodoroToday
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun pomodoroSettings(): PomodoroSettings = UserPrefs.pomodoroSettingsValue().toModel()
@GraphQLQuery
suspend fun pomodoroToday(): PomodoroToday = PomodoroHelper.today()
@GraphQLMutation
suspend fun startPomodoro(durationSec: Int): Boolean { PomodoroHelper.start(durationSec); return true }
@GraphQLMutation
suspend fun pausePomodoro(): Boolean { PomodoroHelper.pause(); return true }
@GraphQLMutation
suspend fun stopPomodoro(): Boolean { PomodoroHelper.stop(); return true }
fun SchemaBuilder.addPomodoroSchema() { }
