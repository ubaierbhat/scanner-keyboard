package org.ubaierbhat.android.barcodekeyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardStateTest {

    @Test
    fun initialModeIsOff() {
        assertEquals(ShiftMode.OFF, KeyboardState().shiftMode)
    }

    @Test
    fun toggleCyclesOffOnOff() {
        val state = KeyboardState()
        state.toggle()
        assertEquals(ShiftMode.ON, state.shiftMode)
        state.toggle()
        assertEquals(ShiftMode.OFF, state.shiftMode)
    }

    @Test
    fun toggleFromLockedTurnsOff() {
        val state = KeyboardState()
        state.toggleLock()
        state.toggle()
        assertEquals(ShiftMode.OFF, state.shiftMode)
    }

    @Test
    fun toggleLockFromOffOrOnSetsLocked() {
        val fromOff = KeyboardState()
        fromOff.toggleLock()
        assertEquals(ShiftMode.LOCKED, fromOff.shiftMode)

        val fromOn = KeyboardState()
        fromOn.toggle()
        fromOn.toggleLock()
        assertEquals(ShiftMode.LOCKED, fromOn.shiftMode)
    }

    @Test
    fun toggleLockCyclesLockedToOff() {
        val state = KeyboardState()
        state.toggleLock()
        state.toggleLock()
        assertEquals(ShiftMode.OFF, state.shiftMode)
    }

    @Test
    fun consumeLetterResetsOn() {
        val state = KeyboardState()
        state.toggle()
        state.consumeLetter()
        assertEquals(ShiftMode.OFF, state.shiftMode)
    }

    @Test
    fun consumeLetterKeepsLocked() {
        val state = KeyboardState()
        state.toggleLock()
        state.consumeLetter()
        state.consumeLetter()
        assertEquals(ShiftMode.LOCKED, state.shiftMode)
    }

    @Test
    fun consumeLetterKeepsOff() {
        val state = KeyboardState()
        state.consumeLetter()
        assertEquals(ShiftMode.OFF, state.shiftMode)
    }

    @Test
    fun applyToLowercasesWhenOff() {
        val state = KeyboardState()
        assertEquals("q", state.applyTo("Q"))
        assertEquals("q", state.applyTo("q"))
    }

    @Test
    fun applyToUppercasesWhenOn() {
        val state = KeyboardState()
        state.toggle()
        assertEquals("Q", state.applyTo("q"))
        assertEquals("Q", state.applyTo("Q"))
    }

    @Test
    fun applyToUppercasesWhenLocked() {
        val state = KeyboardState()
        state.toggleLock()
        assertEquals("Z", state.applyTo("z"))
    }

    @Test
    fun isUppercaseReflectsMode() {
        val state = KeyboardState()
        assertFalse(state.isUppercase())
        state.toggle()
        assertTrue(state.isUppercase())
        state.consumeLetter()
        assertFalse(state.isUppercase())
        state.toggleLock()
        assertTrue(state.isUppercase())
    }

    @Test
    fun resetClearsAnyMode() {
        val locked = KeyboardState()
        locked.toggleLock()
        locked.reset()
        assertEquals(ShiftMode.OFF, locked.shiftMode)

        val on = KeyboardState()
        on.toggle()
        on.reset()
        assertEquals(ShiftMode.OFF, on.shiftMode)
    }
}
