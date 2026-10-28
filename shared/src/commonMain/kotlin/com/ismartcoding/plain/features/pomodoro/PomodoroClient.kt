package com.ismartcoding.plain.features.pomodoro

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.PomodoroToday
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object PomodoroClient {
    suspend fun today(): PomodoroToday =
        GraphqlApi.execute(Query.pomodoroToday())

    suspend fun start(durationSec: Int): PomodoroToday {
        GraphqlApi.execute(Mutation.startPomodoro(durationSec))
        return today()
    }

    suspend fun pause(): PomodoroToday {
        GraphqlApi.execute(Mutation.pausePomodoro())
        return today()
    }

    suspend fun stop(): PomodoroToday {
        GraphqlApi.execute(Mutation.stopPomodoro())
        return today()
    }

    suspend fun adjust(durationSec: Int): PomodoroToday =
        GraphqlApi.execute(Mutation.adjustPomodoro(durationSec))
}
