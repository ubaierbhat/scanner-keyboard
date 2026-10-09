package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.InputConnection

object BackspaceDispatcher {

    sealed interface Action {
        data object DeleteSelection : Action
        data object DeleteOneBeforeCursor : Action
    }

    fun plan(selectedText: CharSequence?): Action =
        if (selectedText.isNullOrEmpty()) {
            Action.DeleteOneBeforeCursor
        } else {
            Action.DeleteSelection
        }

    fun dispatch(inputConnection: InputConnection, selectedText: CharSequence?) {
        when (plan(selectedText)) {
            Action.DeleteSelection -> {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                inputConnection.sendKeyEvent(down)
                inputConnection.sendKeyEvent(
                    KeyEvent(
                        down.downTime,
                        SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_DEL,
                        0,
                    ),
                )
            }
            Action.DeleteOneBeforeCursor ->
                inputConnection.deleteSurroundingText(1, 0)
        }
    }
}
