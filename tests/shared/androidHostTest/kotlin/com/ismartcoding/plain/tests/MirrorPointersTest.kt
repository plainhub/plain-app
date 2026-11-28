package com.ismartcoding.plain.tests

import android.view.MotionEvent
import com.ismartcoding.plain.services.screenmirror.MirrorContact
import com.ismartcoding.plain.services.screenmirror.MirrorPointers
import org.junit.Assert.assertEquals
import org.junit.Test

class MirrorPointersTest {
    @Test fun dragAndPinchKeepOneContactSequence() {
        val actions = mutableListOf<Int>()
        val times = mutableListOf<Long>()
        val samples = mutableListOf<List<MirrorContact>>()
        val pointers = MirrorPointers { time, action, contacts ->
            times.add(time)
            actions.add(action)
            samples.add(contacts.toList())
        }
        pointers.down(243, 1f, 2f, 1f, 10)
        pointers.down(91, 3f, 4f, 1f, 20)
        pointers.move(243, 5f, 6f, 0.5f)
        pointers.up(243, 7f, 8f)
        pointers.up(91, null, null)
        assertEquals(listOf(0, 261, 2, 6, 1), actions)
        assertEquals(List(5) { 10L }, times)
        assertEquals(MirrorContact(0, 5f, 6f, 0.5f), samples[2][0])
        assertEquals(MirrorContact(0, 7f, 8f, 0f), samples[3][0])
        assertEquals(listOf(MirrorContact(1, 3f, 4f, 0f)), samples.last())
    }

    @Test fun disconnectCancelsAllPointersAndIgnoresTrailingMove() {
        val actions = mutableListOf<Int>()
        val pointers = MirrorPointers { _, action, _ -> actions.add(action) }
        pointers.down(65, 1f, 2f, 1f, 10)
        pointers.reset()
        pointers.move(65, 9f, 9f, 1f)
        pointers.down(65, 3f, 4f, 1f, 30)
        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_DOWN), actions)
    }
}
