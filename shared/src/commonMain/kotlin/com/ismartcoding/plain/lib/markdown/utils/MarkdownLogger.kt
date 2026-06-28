package com.ismartcoding.plain.lib.markdown.utils

import androidx.compose.runtime.snapshots.Snapshot

object MarkdownLogger {
    /**
     * Whether to print debug log statements to the relevant system logger. Do not build release
     * artifacts with this enabled. It's purely for debugging purposes.
     */
    var enabled: Boolean = false

    fun d(tag: String, message: () -> String) {
        d(tag = tag, throwable = null, message = message)
    }

    fun d(tag: String, throwable: Throwable?, message: () -> String) {
        if (enabled) {
            Snapshot.withoutReadObservation {
                platformLog(
                    tag = tag,
                    message = buildString {
                        append(message())
                        if (throwable != null) {
                            append(". Throwable: ")
                            append(throwable)
                        }
                    },
                )
            }
        }
    }
}

internal expect fun platformLog(tag: String, message: String)