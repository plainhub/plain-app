package com.ismartcoding.plain.services.screenmirror

import android.view.MotionEvent

internal data class MirrorContact(val slot: Int, val x: Float, val y: Float, val pressure: Float)

internal class MirrorPointers(private val emit: (Long, Int, Collection<MirrorContact>) -> Unit) {
    private val contacts = linkedMapOf<Int, MirrorContact>()
    private var downTime = 0L

    fun down(id: Int, x: Float, y: Float, pressure: Float, now: Long) {
        if (id in contacts) reset()
        val slot = (0..31).firstOrNull { candidate -> contacts.values.none { it.slot == candidate } } ?: return
        if (contacts.isEmpty()) downTime = now
        contacts[id] = MirrorContact(slot, x, y, pressure)
        val action = if (contacts.size == 1) MotionEvent.ACTION_DOWN else
            MotionEvent.ACTION_POINTER_DOWN or (contacts.keys.indexOf(id) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        emit(downTime, action, contacts.values)
    }

    fun move(id: Int, x: Float, y: Float, pressure: Float) {
        val previous = contacts[id] ?: return
        contacts[id] = previous.copy(x = x, y = y, pressure = pressure)
        emit(downTime, MotionEvent.ACTION_MOVE, contacts.values)
    }

    fun up(id: Int, x: Float?, y: Float?) {
        val previous = contacts[id] ?: return
        contacts[id] = previous.copy(x = x ?: previous.x, y = y ?: previous.y, pressure = 0f)
        val action = if (contacts.size == 1) MotionEvent.ACTION_UP else
            MotionEvent.ACTION_POINTER_UP or (contacts.keys.indexOf(id) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        try { emit(downTime, action, contacts.values) } finally { contacts.remove(id) }
    }

    fun reset() {
        if (contacts.isEmpty()) return
        try { emit(downTime, MotionEvent.ACTION_CANCEL, contacts.values) } finally { contacts.clear() }
    }
}
