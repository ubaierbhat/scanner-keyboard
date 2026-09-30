package org.ubaierbhat.android.barcodekeyboard.service

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardActionListener
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardView

class BarcodeKeyboardService : InputMethodService(), KeyboardActionListener {

    private var keyboardView: KeyboardView? = null

    override fun onCreateInputView(): View {
        val root = LayoutInflater.from(this).inflate(R.layout.input_view, null) as FrameLayout
        keyboardView = root.findViewById<KeyboardView>(R.id.keyboard_view).apply {
            setListener(this@BarcodeKeyboardService)
        }
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
    }

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onBackspace() {
        val inputConnection = currentInputConnection ?: return
        val selectedText = inputConnection.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        } else {
            inputConnection.deleteSurroundingText(1, 1)
        }
    }

    override fun onEnter() {
        sendKeyChar('\n')
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
