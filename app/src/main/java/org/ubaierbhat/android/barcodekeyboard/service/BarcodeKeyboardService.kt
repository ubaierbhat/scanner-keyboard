package org.ubaierbhat.android.barcodekeyboard.service

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.ubaierbhat.android.barcodekeyboard.MainActivity
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.history.HistoryPanelView
import org.ubaierbhat.android.barcodekeyboard.history.ScanHistoryStore
import org.ubaierbhat.android.barcodekeyboard.keyboard.BackspaceDispatcher
import org.ubaierbhat.android.barcodekeyboard.keyboard.EnterActionResolver
import org.ubaierbhat.android.barcodekeyboard.keyboard.EnterBehavior
import org.ubaierbhat.android.barcodekeyboard.keyboard.EnterDispatcher
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyView
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardActionListener
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyboardView
import org.ubaierbhat.android.barcodekeyboard.scanner.DuplicateSuppressor
import org.ubaierbhat.android.barcodekeyboard.scanner.ScanEvent
import org.ubaierbhat.android.barcodekeyboard.scanner.ScanSettings
import org.ubaierbhat.android.barcodekeyboard.scanner.ScanEventPlanner
import org.ubaierbhat.android.barcodekeyboard.scanner.ScannerView

class BarcodeKeyboardService : InputMethodService(), KeyboardActionListener {

    private enum class Mode {
        KEYBOARD,
        SCANNER,
        HISTORY,
    }

    private var inputContainer: FrameLayout? = null
    private var keyboardView: KeyboardView? = null
    private var permissionView: View? = null
    private var openSetupKey: KeyView? = null
    private var scannerView: ScannerView? = null
    private var historyPanelView: HistoryPanelView? = null
    private var historyStore: ScanHistoryStore? = null
    private var mode = Mode.KEYBOARD

    private val handler = Handler(Looper.getMainLooper())
    private val dismissScannerRunnable = Runnable { closeScanner() }

    private var enterBehavior: EnterBehavior = EnterBehavior.SendKeyEvent
    private val duplicateSuppressor = DuplicateSuppressor()
    private var scanSettings: ScanSettings? = null

    override fun onCreate() {
        super.onCreate()
        historyStore = ScanHistoryStore(this)
        scanSettings = ScanSettings(this)
    }

    override fun onCreateInputView(): View {
        scannerView?.stop()
        scannerView = null
        historyPanelView = null
        mode = Mode.KEYBOARD
        val root = LayoutInflater.from(this).inflate(R.layout.input_view, null) as FrameLayout
        attachNavigationBarInsets(root)
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
        keyboardView?.applyInputType(info?.inputType ?: android.text.InputType.TYPE_NULL)
        if (!restarting) {
            captureClipboardEntry()
        }
        if (mode == Mode.SCANNER) {
            closeScanner()
        } else if (mode == Mode.HISTORY) {
            closeHistory()
        }
        if (permissionView?.visibility == View.VISIBLE && isCameraGranted()) {
            showKeyboard()
        }
    }

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return
        BackspaceDispatcher.dispatch(ic, ic.getSelectedText(0))
    }

    override fun onEnter() {
        val inputConnection = currentInputConnection ?: return
        dispatchEnter(inputConnection)
    }

    private fun dispatchEnter(inputConnection: InputConnection) {
        EnterDispatcher.dispatch(
            enterBehavior,
            { actionId -> inputConnection.performEditorAction(actionId) },
            { sendKeyChar('\n') },
            {
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
            },
        )
    }

    private fun sendTab(inputConnection: InputConnection) {
        val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB)
        val up = KeyEvent(
            down.downTime,
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_TAB,
            0,
        )
        val downHandled = inputConnection.sendKeyEvent(down)
        val upHandled = inputConnection.sendKeyEvent(up)
        if (!downHandled && !upHandled) {
            inputConnection.commitText("\t", 1)
        }
    }

    override fun onScanRequested() {
        if (mode == Mode.SCANNER) {
            return
        }
        if (!isCameraGranted()) {
            showPermissionPrompt()
            return
        }
        openScanner()
    }

    override fun onHistoryRequested() {
        if (mode == Mode.HISTORY) {
            return
        }
        openHistory()
    }

    override fun onSettingsRequested() {
        openSetup()
        requestHideSelf(0)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboardView?.resetToLetters()
        closeScanner()
        closeHistory()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        closeScanner()
        closeHistory()
    }

    override fun onDestroy() {
        closeScanner()
        closeHistory()
        scannerView = null
        historyPanelView = null
        keyboardView?.resetToLetters()
        keyboardView = null
        openSetupKey = null
        permissionView = null
        inputContainer = null
        historyStore = null
        super.onDestroy()
    }

    private fun openScanner() {
        val container = inputContainer ?: return
        val scanner = obtainScannerView() ?: return
        val height = (resources.displayMetrics.heightPixels * SCANNER_HEIGHT_FRACTION).toInt()
        if (scanner.parent != null) {
            (scanner.parent as ViewGroup).removeView(scanner)
        }
        container.addView(
            scanner,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
            ),
        )
        keyboardView?.visibility = View.GONE
        permissionView?.visibility = View.GONE
        historyPanelView?.visibility = View.GONE
        scanner.visibility = View.VISIBLE
        mode = Mode.SCANNER
        scanner.start(this)
        duplicateSuppressor.reset()
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
                onBarcodeResult = { text -> handleBarcodeResult(text) },
                onContinuousChanged = { duplicateSuppressor.reset() },
            )
        }.also { scannerView = it }
    }

    private fun handleBarcodeResult(text: String) {
        if (mode != Mode.SCANNER) {
            return
        }
        val continuous = scannerView?.isContinuousEnabled == true
        if (continuous && !duplicateSuppressor.shouldEmit(text)) {
            return
        }
        val events = ScanEventPlanner.plan(
            raw = text,
            continuous = continuous,
            translateActions = scanSettings?.translateScanActions == true
        )
        if (events.isNotEmpty()) {
            emitScanEvents(events, 0)
        }
        inputContainer?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        recordScan(text)
        if (!continuous) {
            closeScanner()
        }
    }

    /**
     * Sequenced emitter: IME sessions are view-bound and a focus switch completes
     * asynchronously, so each action must settle before the chain re-resolves the connection.
     */
    private fun emitScanEvents(events: List<ScanEvent>, index: Int) {
        if (index >= events.size) {
            return
        }
        when (val event = events[index]) {
            is ScanEvent.Type -> {
                currentInputConnection?.commitText(event.text, 1)
                emitScanEvents(events, index + 1)
            }
            ScanEvent.Enter -> {
                currentInputConnection?.let { dispatchEnter(it) }
                handler.postDelayed({ emitScanEvents(events, index + 1) }, SCAN_ACTION_SETTLE_MS)
            }
            ScanEvent.Tab -> {
                currentInputConnection?.let { sendTab(it) }
                handler.postDelayed({ emitScanEvents(events, index + 1) }, SCAN_ACTION_SETTLE_MS)
            }
        }
    }

    private fun recordScan(text: String) {
        historyStore?.add(text)
    }

    private fun scheduleScannerDismiss() {
        handler.removeCallbacks(dismissScannerRunnable)
        handler.postDelayed(dismissScannerRunnable, SCANNER_ERROR_DISMISS_MS)
    }

    private fun closeScanner() {
        handler.removeCallbacks(dismissScannerRunnable)
        duplicateSuppressor.reset()
        scannerView?.stop()
        showKeyboard()
    }

    private fun openHistory() {
        val container = inputContainer ?: return
        val panel = obtainHistoryPanelView() ?: return
        closeScanner()
        val height = (resources.displayMetrics.heightPixels * HISTORY_HEIGHT_FRACTION).toInt()
        if (panel.parent != null) {
            (panel.parent as ViewGroup).removeView(panel)
        }
        container.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
            ),
        )
        panel.refresh(historyStore?.entries().orEmpty())
        keyboardView?.visibility = View.GONE
        permissionView?.visibility = View.GONE
        scannerView?.visibility = View.GONE
        panel.visibility = View.VISIBLE
        mode = Mode.HISTORY
    }

    private fun obtainHistoryPanelView(): HistoryPanelView? {
        historyPanelView?.let { return it }
        if (inputContainer == null) {
            return null
        }
        return HistoryPanelView(this).apply {
            setCallbacks(
                onEntrySelected = { text -> handleHistoryEntrySelected(text) },
                onClose = { closeHistory() },
                onClear = { clearHistory() },
            )
        }.also { historyPanelView = it }
    }

    private fun handleHistoryEntrySelected(text: String) {
        currentInputConnection?.commitText(text, 1)
        closeHistory()
    }

    private fun clearHistory() {
        historyStore?.clear()
        historyPanelView?.refresh(historyStore?.entries().orEmpty())
    }

    private fun closeHistory() {
        historyPanelView?.visibility = View.GONE
        if (mode == Mode.HISTORY) {
            showKeyboard()
        }
    }

    private fun captureClipboardEntry() {
        val store = historyStore ?: return
        try {
            val clipboard = getSystemService(ClipboardManager::class.java) ?: return
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) {
                return
            }
            val text = clip.getItemAt(0).coerceToText(this)?.toString()?.trim() ?: return
            if (text.isEmpty() || text.length > MAX_CLIPBOARD_ENTRY_LENGTH) {
                return
            }
            if (store.entries().firstOrNull() == text) {
                return
            }
            store.add(text)
        } catch (error: SecurityException) {
            return
        } catch (error: IllegalStateException) {
            return
        }
    }

    private fun showKeyboard() {
        mode = Mode.KEYBOARD
        scannerView?.visibility = View.GONE
        permissionView?.visibility = View.GONE
        historyPanelView?.visibility = View.GONE
        keyboardView?.visibility = View.VISIBLE
    }

    private fun showPermissionPrompt() {
        keyboardView?.visibility = View.GONE
        scannerView?.visibility = View.GONE
        historyPanelView?.visibility = View.GONE
        permissionView?.visibility = View.VISIBLE
    }

    private fun isCameraGranted(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    private fun openSetup() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
        startActivity(intent)
    }

    internal companion object {
        const val SCANNER_HEIGHT_FRACTION = 0.45f
        const val HISTORY_HEIGHT_FRACTION = 0.45f
        const val SCANNER_ERROR_DISMISS_MS = 1500L
        const val SCAN_ACTION_SETTLE_MS = 150L
        const val MAX_CLIPBOARD_ENTRY_LENGTH = 500

        internal fun attachNavigationBarInsets(view: View) {
            val baseLeft = view.paddingLeft
            val baseTop = view.paddingTop
            val baseRight = view.paddingRight
            val baseBottom = view.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                target.setPadding(
                    baseLeft + bars.left,
                    baseTop,
                    baseRight + bars.right,
                    baseBottom + bars.bottom,
                )
                insets
            }
        }
    }
}
