package org.ubaierbhat.android.barcodekeyboard.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateSuppressorTest {

    @Test
    fun firstValueIsEmitted() {
        val suppressor = DuplicateSuppressor()
        assertTrue(suppressor.shouldEmit("A"))
    }

    @Test
    fun consecutiveSameValueIsSuppressed() {
        val suppressor = DuplicateSuppressor()
        suppressor.shouldEmit("A")
        assertFalse(suppressor.shouldEmit("A"))
        assertFalse(suppressor.shouldEmit("A"))
    }

    @Test
    fun differentValueRearmsTheSameValue() {
        val suppressor = DuplicateSuppressor()
        assertTrue(suppressor.shouldEmit("A"))
        assertTrue(suppressor.shouldEmit("B"))
        assertTrue(suppressor.shouldEmit("A"))
    }

    @Test
    fun resetForgetsTheLastValue() {
        val suppressor = DuplicateSuppressor()
        suppressor.shouldEmit("A")
        suppressor.reset()
        assertTrue(suppressor.shouldEmit("A"))
    }
}
