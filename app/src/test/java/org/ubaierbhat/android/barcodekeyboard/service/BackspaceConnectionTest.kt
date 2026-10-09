package org.ubaierbhat.android.barcodekeyboard.service

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.ubaierbhat.android.barcodekeyboard.keyboard.BackspaceDispatcher

@RunWith(RobolectricTestRunner::class)
class BackspaceConnectionTest {

    private fun connectionWith(text: String, start: Int, end: Int): Pair<EditText, InputConnection> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val editText = EditText(context)
        editText.setText(text)
        editText.setSelection(start, end)
        // TextView.onCreateInputConnection is public API and returns the same real framework
        // connection (com.android.internal.inputmethod.EditableInputConnection) that an editor
        // app installs for an IME — no reflection needed.
        val inputConnection = editText.onCreateInputConnection(EditorInfo())
            ?: error("EditText must provide an InputConnection")
        return editText to inputConnection
    }

    @Test
    fun backspaceWithCursorInMiddleDeletesOnlyLeftCharacter() {
        val (editText, inputConnection) = connectionWith("abcdef", 3, 3)

        BackspaceDispatcher.dispatch(inputConnection, inputConnection.getSelectedText(0))

        assertEquals("abdef", editText.text.toString())
    }

    @Test
    fun backspaceWithSelectionSendsDelKeyEventsInsteadOfDeletingSurroundingText() {
        val (editText, inputConnection) = connectionWith("abcdef", 1, 4)
        val keyEvents = mutableListOf<Pair<Int, Int>>()
        val surroundingDeletes = mutableListOf<Pair<Int, Int>>()
        val recordingConnection = object : InputConnectionWrapper(inputConnection, false) {
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                keyEvents += event.action to event.keyCode
                return super.sendKeyEvent(event)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                surroundingDeletes += beforeLength to afterLength
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
        }

        BackspaceDispatcher.dispatch(recordingConnection, recordingConnection.getSelectedText(0))

        assertEquals(
            listOf(
                KeyEvent.ACTION_DOWN to KeyEvent.KEYCODE_DEL,
                KeyEvent.ACTION_UP to KeyEvent.KEYCODE_DEL,
            ),
            keyEvents,
        )
        assertEquals(emptyList<Pair<Int, Int>>(), surroundingDeletes)
        // Observed under Robolectric: the real connection does NOT honor sendKeyEvent(DEL)
        // (verified detached and attached to an activity), so the "bcd" selection is not
        // collapsed here. On a real device the DEL key events collapse the selection to "aef";
        // the observable behavior asserted in this harness is the exact key-event traffic above,
        // the absence of any deleteSurroundingText call, and the unchanged text.
        assertEquals("abcdef", editText.text.toString())
    }
}
