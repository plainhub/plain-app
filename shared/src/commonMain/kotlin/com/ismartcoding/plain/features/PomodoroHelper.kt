package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DPomodoroItem
import com.ismartcoding.plain.httpserver.models.PomodoroToday
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object PomodoroHelper {
    private const val FIELDS = "date completedCount currentRound timeLeftSec totalTimeSec isRunning isPaused state"
    private val dayLock = Mutex()
    private var configuredDay = ""

    suspend fun configureDay(force: Boolean = false) = dayLock.withLock {
        val zone = TimeZone.currentSystemDefault()
        val date = TimeHelper.now().toLocalDateTime(zone).date
        val start = date.atStartOfDayIn(zone)
        val key = "$date/$start"
        if (force || configuredDay != key) {
            RustContentApi.mutate("configurePomodoroDay(date: ${gql(date.toString())}, dayStart: ${gql(start.toString())}) { $FIELDS }")
            configuredDay = key
        }
    }
    suspend fun today(): PomodoroToday {
        configureDay()
        return RustContentApi.query("pomodoroToday { $FIELDS }").getValue("pomodoroToday").today()
    }
    suspend fun start(durationSec: Int): PomodoroToday {
        configureDay()
        RustContentApi.mutate("startPomodoro(durationSec: $durationSec)")
        return today()
    }
    suspend fun pause(): PomodoroToday {
        RustContentApi.mutate("pausePomodoro")
        return today()
    }
    suspend fun stop(): PomodoroToday {
        RustContentApi.mutate("stopPomodoro")
        return today()
    }
    suspend fun adjust(durationSec: Int): PomodoroToday = RustContentApi.mutate("adjustPomodoro(durationSec: $durationSec) { $FIELDS }")
        .getValue("adjustPomodoro").today()
    suspend fun tick(skip: Boolean = false): Pair<PomodoroToday, PomodoroState?> {
        configureDay()
        val name = if (skip) "skipPomodoro" else "tickPomodoro"
        val result = RustContentApi.mutate("$name { today { $FIELDS } completedState }").getValue(name).jsonObject
        return result.getValue("today").today() to result["completedState"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.let(PomodoroState::valueOf)
    }
    suspend fun record(date: String): DPomodoroItem? = RustContentApi.query("pomodoroRecord(date: ${gql(date)}) { id date completedCount totalWorkSec totalBreakSec createdAt updatedAt }")
        .getValue("pomodoroRecord").takeUnless { it is JsonNull }?.jsonObject?.let {
            DPomodoroItem(id = it.string("id"), date = it.string("date"), completedCount = it.getValue("completedCount").jsonPrimitive.int,
                totalWorkSeconds = it.getValue("totalWorkSec").jsonPrimitive.int, totalBreakSeconds = it.getValue("totalBreakSec").jsonPrimitive.int,
                createdAt = it.instant("createdAt"), updatedAt = it.instant("updatedAt"))
        }
    private fun JsonElement.today(): PomodoroToday = jsonObject.let {
        PomodoroToday(date = it.instant("date"), completedCount = it.getValue("completedCount").jsonPrimitive.int,
            currentRound = it.getValue("currentRound").jsonPrimitive.int, timeLeftSec = it.getValue("timeLeftSec").jsonPrimitive.int,
            totalTimeSec = it.getValue("totalTimeSec").jsonPrimitive.int, isRunning = it.getValue("isRunning").jsonPrimitive.boolean,
            isPaused = it.getValue("isPaused").jsonPrimitive.boolean, state = PomodoroState.valueOf(it.string("state")))
    }
}
