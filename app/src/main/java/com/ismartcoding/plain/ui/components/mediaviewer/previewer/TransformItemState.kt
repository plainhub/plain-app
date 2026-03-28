package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal val imageTransformMutex = Mutex()
internal val transformItemStateMap = mutableStateMapOf<Any, TransformItemState>()

class TransformItemState(
    var key: Any = Unit,
    var blockCompose: (@Composable (Any) -> Unit) = {},
    var scope: CoroutineScope,
    var blockPosition: Offset = Offset.Zero,
    var blockSize: IntSize = IntSize.Zero,
    var intrinsicSize: Size? = null,
    var checkInBound: (TransformItemState.() -> Boolean)? = null,
) {
    private fun checkItemInMap() {
        if (checkInBound == null) return
        if (checkInBound!!.invoke(this)) addItem() else removeItem()
    }

    internal fun onPositionChange(position: Offset, size: IntSize) {
        blockPosition = position; blockSize = size
        scope.launch { checkItemInMap() }
    }

    fun checkIfInBound(checkInBound: () -> Boolean) {
        if (checkInBound()) addItem() else removeItem()
    }

    fun addItem(key: Any? = null) {
        val currentKey = key ?: this.key
        if (checkInBound != null) return
        synchronized(imageTransformMutex) { transformItemStateMap[currentKey] = this }
    }

    fun removeItem(key: Any? = null) {
        synchronized(imageTransformMutex) {
            val currentKey = key ?: this.key
            if (checkInBound != null) return
            transformItemStateMap.remove(currentKey)
        }
    }
}

@Composable
fun rememberTransformItemState(
    scope: CoroutineScope = rememberCoroutineScope(),
    checkInBound: (TransformItemState.() -> Boolean)? = null,
): TransformItemState {
    return remember { TransformItemState(scope = scope, checkInBound = checkInBound) }
}
