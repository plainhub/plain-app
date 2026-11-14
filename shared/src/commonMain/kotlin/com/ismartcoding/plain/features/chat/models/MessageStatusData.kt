package com.ismartcoding.plain.features.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class MessageStatusData(
    val results: List<MessageDeliveryResult> = emptyList(),
) {
    val total: Int get() = results.size
    val deliveredCount: Int get() = results.count { it.error == null }
    val failedCount: Int get() = results.count { it.error != null }
    val failedResults: List<MessageDeliveryResult> get() = results.filter { it.error != null }
    val deliveredResults: List<MessageDeliveryResult> get() = results.filter { it.error == null }
    val allFailed: Boolean get() = total > 0 && deliveredCount == 0
    fun deliveryLabel(): String = "$deliveredCount/$total"

}
