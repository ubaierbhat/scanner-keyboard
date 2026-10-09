package org.ubaierbhat.android.barcodekeyboard.service

import android.content.Context
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.ubaierbhat.android.barcodekeyboard.keyboard.BackspaceDispatcher

@RunWith(RobolectricTestRunner::class)
class BackspaceConnectionTest {

    private fun dispatch(inputConnection: InputConnection) {
        val selectedText = inputConnection.getSelectedText(0)
        when (BackspaceDispatcher.plan(selectedText)) {
            BackspaceDispatcher.Action.DeleteSelection -> {
                val down = android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_DEL,
                )
                inputConnection.sendKeyEvent(down)
                inputConnection.sendKeyEvent(
                    android.view.KeyEvent(
                        down.downTime,
                        android.os.SystemClock.uptimeMillis(),
                        android.view.KeyEvent.ACTION_UP,
                        android.view.KeyEvent.KEYCODE_DEL,
                        0,
                    ),
                )
            }
            BackspaceDispatcher.Action.DeleteOneBeforeCursor ->
                inputConnection.deleteSurroundingText(1, 0)
        }
    }

    @Test
    fun backspaceWithCursorInMiddleDeletesOnlyLeftCharacter() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val editText = EditText(context)
        editText.setText("abcdef")
        editText.setSelection(3)
        // android.view.inputmethod.EditableInputConnection is @hide and was moved to
        // com.android.internal.inputmethod in Android 15, so it is absent from both the SDK
        // stubs and the Robolectric android-all jar under its original name; construct the
        // real framework class reflectively.
        val inputConnection = Class.forName("com.android.internal.inputmethod.EditableInputConnection")
            .getConstructor(android.widget.TextView::class.java)
            .newInstance(editText) as InputConnection

        dispatch(inputConnection)

        assertEquals("abdef", editText.text.toString())
    }
}
