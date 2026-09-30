package org.ubaierbhat.android.barcodekeyboard.service

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.keyboard.EnterActionResolver
import org.ubaierbhat.android.barcodekeyboard.keyboard.EnterBehavior
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardActionListener
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardView

class BarcodeKeyboardService : InputMethodService(), KeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var enterBehavior: EnterBehavior = EnterBehavior.SendKeyEvent

    override fun onCreateInputView(): View {
        val root = LayoutInflater.from(this).inflate(R.layout.input_view, null) as FrameLayout
        keyboardView = root.findViewById<KeyboardView>(R.id.keyboard_view).apply {
            setListener(this@BarcodeKeyboardService)
        }
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        enterBehavior = if (info != null) {
            EnterActionResolver.resolve(info)
        } else {
            EnterBehavior.SendKeyEvent
        }
    }

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onBackspace() {
        val inputConnection = currentInputConnection ?: return
        val selectedText = inputConnection.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
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
        } else {
            inputConnection.deleteSurroundingText(1, 1)
        }
    }

    override fun onEnter() {
        val inputConnection = currentInputConnection ?: return
        when (val behavior = enterBehavior) {
            is EnterBehavior.PerformAction -> inputConnection.performEditorAction(behavior.actionId)
            EnterBehavior.Newline -> sendKeyChar('\n')
            EnterBehavior.SendKeyEvent -> {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                inputConnection.sendKeyEvent(down)
                inputConnection.sendKeyEvent(
                    KeyEvent(
                        down.downTime,
                        SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_ENTER,
                        0,
                    ),
                )
            }
        }
    }

    override fun onScanRequested() = Unit

    override fun onHistoryRequested() = Unit

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboardView?.resetToLetters()
    }

    override fun onDestroy() {
        keyboardView?.resetToLetters()
        keyboardView = null
        super.onDestroy()
    }
}
