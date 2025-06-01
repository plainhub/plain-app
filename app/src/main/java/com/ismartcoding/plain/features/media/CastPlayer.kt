package com.ismartcoding.plain.features.media

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.upnp.UPnPDevice
import com.ismartcoding.plain.data.IMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CastPlayer {
    var currentDevice: UPnPDevice? = null

    private val _items = MutableStateFlow<List<IMedia>>(emptyList())
    val items: StateFlow<List<IMedia>> = _items.asStateFlow()

    private val _currentUri = MutableStateFlow("")
    val currentUri: StateFlow<String> = _currentUri.asStateFlow()
    val isPlaying = MutableStateFlow(false)

    // 播放进度相关状态
    val progress = MutableStateFlow(0f) // 当前播放位置（秒）
    val duration = MutableStateFlow(0f) // 总时长（秒）
    val supportsCallback = MutableStateFlow(false) // 是否支持回调

    var sid: String = ""

    fun setItems(newItems: List<IMedia>) {
        _items.value = newItems
    }

    fun addItem(item: IMedia) {
        _items.value = _items.value + item
    }

    fun removeItem(item: IMedia) {
        _items.value = _items.value.filter { it.path != item.path }
    }

    fun removeItemAt(index: Int) {
        val currentList = _items.value.toMutableList()
        if (index in 0 until currentList.size) {
            currentList.removeAt(index)
            _items.value = currentList
        }
    }

    fun clearItems() {
        _items.value = emptyList()
        _currentUri.value = ""
        isPlaying.value = false
        progress.value = 0f
        duration.value = 0f
        supportsCallback.value = false
    }

    fun setCurrentUri(uri: String) {
        _currentUri.value = uri
    }

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        val currentList = _items.value.toMutableList()
        if (fromIndex in 0 until currentList.size && toIndex in 0 until currentList.size) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _items.value = currentList
        }
    }

    /**
     * 解析 UPnP 时间格式 (HH:MM:SS 或 HH:MM:SS.mmm) 到秒数
     */
    fun parseTimeToSeconds(timeString: String): Float {
        if (timeString.isEmpty() || timeString == "NOT_IMPLEMENTED") return 0f
        
        return try {
            val parts = timeString.split(":")
            if (parts.size >= 3) {
                val hours = parts[0].toFloat()
                val minutes = parts[1].toFloat()
                val seconds = parts[2].split(".")[0].toFloat() // 忽略毫秒部分
                hours * 3600 + minutes * 60 + seconds
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * 更新播放位置信息
     */
    fun updatePositionInfo(relTime: String, trackDuration: String) {
        progress.value = parseTimeToSeconds(relTime)
        duration.value = parseTimeToSeconds(trackDuration)
    }
}
