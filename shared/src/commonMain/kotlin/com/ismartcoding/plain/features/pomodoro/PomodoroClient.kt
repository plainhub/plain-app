package com.ismartcoding.plain.features.pomodoro

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.PomodoroToday
import com.ismartcoding.plain.rust.graphql.GraphqlApi

object PomodoroClient {
    suspend fun today(): PomodoroToday =
        GraphqlApi.execute(Query.pomodoroToday())

    suspend fun start(durationSec: Int): PomodoroToday =
        GraphqlApi.execute(Mutation.startPomodoro(durationSec))

    suspend fun pause(): PomodoroToday =
        GraphqlApi.execute(Mutation.pausePomodoro())

    suspend fun stop(): PomodoroToday =
        GraphqlApi.execute(Mutation.stopPomodoro())

    suspend fun setRemainingTime(remainingSec: Int): PomodoroToday =
        GraphqlApi.execute(Mutation.setPomodoroRemainingTime(remainingSec))
}
