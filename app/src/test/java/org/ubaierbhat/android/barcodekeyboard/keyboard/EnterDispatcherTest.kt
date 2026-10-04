package org.ubaierbhat.android.barcodekeyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class EnterDispatcherTest {

    private class Spy {
        val calls = mutableListOf<String>()
        var actionHandled = true

        fun performAction(actionId: Int): Boolean {
            calls.add("action:$actionId")
            return actionHandled
        }

        fun newline() {
            calls.add("newline")
        }

        fun enterKey() {
            calls.add("enterKey")
        }
    }

    private fun dispatch(behavior: EnterBehavior, spy: Spy) {
        EnterDispatcher.dispatch(
            behavior,
            { spy.performAction(it) },
            { spy.newline() },
            { spy.enterKey() },
        )
    }

    @Test
    fun performActionHandledFiresNoFallback() {
        val spy = Spy()
        dispatch(EnterBehavior.PerformAction(3), spy)
        assertEquals(listOf("action:3"), spy.calls)
    }

    @Test
    fun performActionUnhandledFallsBackToEnterKeyEvent() {
        val spy = Spy()
        spy.actionHandled = false
        dispatch(EnterBehavior.PerformAction(3), spy)
        assertEquals(listOf("action:3", "enterKey"), spy.calls)
    }

    @Test
    fun newlineBehaviorSendsNewline() {
        val spy = Spy()
        dispatch(EnterBehavior.Newline, spy)
        assertEquals(listOf("newline"), spy.calls)
    }

    @Test
    fun sendKeyEventBehaviorSendsEnterKey() {
        val spy = Spy()
        dispatch(EnterBehavior.SendKeyEvent, spy)
        assertEquals(listOf("enterKey"), spy.calls)
    }
}
