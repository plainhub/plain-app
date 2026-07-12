package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.runtime.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class Ticket {

    private var ticketKey by mutableStateOf("")

    private val ticketMap = mutableMapOf<String, Continuation<Unit>>()

    suspend fun awaitNextTicket() = suspendCoroutine { c ->
        ticketKey = Uuid.random().toString()
        ticketMap[ticketKey] = c
    }

    private fun clearTicket() {
        ticketMap.forEach {
            it.value.resume(Unit)
        }
        ticketMap.clear()
    }

    @Composable
    fun Next() {
        LaunchedEffect(ticketKey) {
            clearTicket()
        }
    }

}