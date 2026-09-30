package org.ubaierbhat.android.barcodekeyboard.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.ubaierbhat.android.barcodekeyboard.MainActivity
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.keyboard.EnterActionResolver
import org.ubaierbhat.android.barcodekeyboard.keyboard.EnterBehavior
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyView
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardActionListener
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardView
import org.ubaierbhat.android.barcodekeyboard.scanner.ScannerView

class BarcodeKeyboardService : InputMethodService(), KeyboardActionListener {

    private enum class Mode {
        KEYBOARD,
        SCANNER,
    }

    private var inputContainer: FrameLayout? = null
    private var keyboardView: KeyboardView? = null
    private var permissionView: View? = null
    private var openSetupKey: KeyView? = null
    private var scannerView: ScannerView? = null
    private var mode = Mode.KEYBOARD

    private val handler = Handler(Looper.getMainLooper())
    private val dismissScannerRunnable = Runnable { closeScanner() }

    private var enterBehavior: EnterBehavior = EnterBehavior.SendKeyEvent

    override fun onCreateInputView(): View {
        val root = LayoutInflater.from(this).inflate(R.layout.input_view, null) as FrameLayout
        inputContainer = root
        keyboardView = root.findViewById<KeyboardView>(R.id.keyboard_view).apply {
            setListener(this@BarcodeKeyboardService)
        }
        permissionView = root.findViewById(R.id.scanner_permission)
        openSetupKey = root.findViewById<KeyView>(R.id.scanner_open_setup_key).apply {
            onPress = { openSetup() }
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
        if (mode != Mode.KEYBOARD) {
            closeScanner()
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

    override fun onScanRequested() {
        if (mode == Mode.SCANNER) {
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            showPermissionPrompt()
            return
        }
        openScanner()
    }

    override fun onHistoryRequested() = Unit

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboardView?.resetToLetters()
        closeScanner()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        closeScanner()
    }

    override fun onDestroy() {
        closeScanner()
        scannerView = null
        keyboardView?.resetToLetters()
        keyboardView = null
        openSetupKey = null
        permissionView = null
        inputContainer = null
        super.onDestroy()
    }

    private fun openScanner() {
        val container = inputContainer ?: return
        val scanner = obtainScannerView() ?: return
        val height = (resources.displayMetrics.heightPixels * SCANNER_HEIGHT_FRACTION).toInt()
        if (scanner.parent == null) {
            container.addView(
                scanner,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    height,
                ),
            )
        } else {
            val params = scanner.layoutParams as FrameLayout.LayoutParams
            params.height = height
            scanner.layoutParams = params
        }
        keyboardView?.visibility = View.GONE
        permissionView?.visibility = View.GONE
        scanner.visibility = View.VISIBLE
        mode = Mode.SCANNER
        scanner.start(this)
    }

    private fun obtainScannerView(): ScannerView? {
        scannerView?.let { return it }
        if (inputContainer == null) {
            return null
        }
        return ScannerView(this).apply {
            setCallbacks(
                onClose = { closeScanner() },
                onError = { scheduleScannerDismiss() },
            )
        }.also { scannerView = it }
    }

    private fun scheduleScannerDismiss() {
        handler.removeCallbacks(dismissScannerRunnable)
        handler.postDelayed(dismissScannerRunnable, SCANNER_ERROR_DISMISS_MS)
    }

    private fun closeScanner() {
        handler.removeCallbacks(dismissScannerRunnable)
        scannerView?.stop()
        showKeyboard()
    }

    private fun showKeyboard() {
        mode = Mode.KEYBOARD
        scannerView?.visibility = View.GONE
        permissionView?.visibility = View.GONE
        keyboardView?.visibility = View.VISIBLE
    }

    private fun showPermissionPrompt() {
        keyboardView?.visibility = View.GONE
        scannerView?.visibility = View.GONE
        permissionView?.visibility = View.VISIBLE
    }

    private fun openSetup() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private companion object {
        const val SCANNER_HEIGHT_FRACTION = 0.45f
        const val SCANNER_ERROR_DISMISS_MS = 1500L
    }
}
