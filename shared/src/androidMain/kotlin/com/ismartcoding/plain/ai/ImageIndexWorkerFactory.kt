package com.ismartcoding.plain.ai

interface ImageIndexWorkerFactory {
    fun isReady(): Boolean
    fun create(): ImageIndexWorker
}
