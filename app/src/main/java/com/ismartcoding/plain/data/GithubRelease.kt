package com.ismartcoding.plain.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LatestRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String = "",
    val body: String = "",
    @SerialName("html_url")
    val htmlUrl: String = "",
    val draft: Boolean = false,
    @SerialName("prerelease")
    val preRelease: Boolean = false,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("published_at")
    val publishedAt: String = "",
    val assets: List<AssetsItem> = arrayListOf(),
) {
    fun getDownloadUrl(): String {
        return assets.firstOrNull()?.browserDownloadUrl ?: ""
    }

    fun getDownloadSize(): Long {
        return assets.firstOrNull()?.size ?: 0
    }

    @Serializable
    data class AssetsItem(
        val name: String = "",
        @SerialName("content_type")
        val contentType: String = "",
        val size: Long = 0,
        @SerialName("download_count")
        val downloadCount: Int = 0,
        @SerialName("browser_download_url")
        val browserDownloadUrl: String = "",
    )
}