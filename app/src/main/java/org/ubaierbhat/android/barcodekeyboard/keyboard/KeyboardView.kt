package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import kotlin.math.abs
import org.ubaierbhat.android.barcodekeyboard.R

enum class KeyboardLayer {
    LETTERS,
    SYMBOLS,
    SYMBOLS_ALT,
    PHONE,
    NUMBER,
}

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var listener: KeyboardActionListener? = null
    private val keyboardState = KeyboardState()
    private val letterKeys: List<KeyView>
    private val letterShiftKey: KeyView
    private val shiftKeys: List<KeyView>
    private val layers: List<View>
    private var currentLayer = KeyboardLayer.LETTERS
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
    private var accentPopup: PopupWindow? = null
    private var accentOriginKey: KeyView? = null
    private val accentCandidates = mutableListOf<KeyView>()
    private var accentHighlight = -1

    init {
        LayoutInflater.from(context).inflate(R.layout.keyboard_view, this)
        letterKeys = LETTER_IDS.map { id -> findViewById<KeyView>(id) }
        letterKeys.forEach { key ->
            key.onPress = { onLetterPressed(key) }
            if (AccentMap.variants(key.text[0]).isNotEmpty()) {
                key.longPressTimeoutMs = ACCENT_HOLD_TIMEOUT_MS
                key.dragHoldEnabled = true
                key.onLongPress = { showAccentPopup(key) }
                key.onHoldTouch = { event -> dispatchToAccentPopup(event) }
            }
        }
        letterShiftKey = findViewById<KeyView>(R.id.key_shift)
        shiftKeys = SHIFT_IDS.map { id -> findViewById<KeyView>(id) }
        shiftKeys.forEach { key ->
            key.onPress = {
                keyboardState.toggle()
                refreshLetterLabels()
            }
            key.onLongPress = {
                keyboardState.toggleLock()
                refreshLetterLabels()
            }
        }
        BACKSPACE_IDS.map { id -> findViewById<KeyView>(id) }.forEach { key ->
            key.onPress = { listener?.onBackspace() }
            key.onRepeat = { listener?.onBackspace() }
        }
        SYMBOL_TEXT_IDS.map { id -> findViewById<KeyView>(id) }.forEach { key ->
            key.onPress = { listener?.onText(key.text.toString()) }
        }
        SCAN_IDS.map { id -> findViewById<KeyView>(id) }.forEach { key ->
            key.onPress = { listener?.onScanRequested() }
        }
        HIST_IDS.map { id -> findViewById<KeyView>(id) }.forEach { key ->
            key.onPress = { listener?.onHistoryRequested() }
        }
        SPACE_IDS.map { id -> findViewById<KeyView>(id) }.forEach { key ->
            key.onPress = { listener?.onText(SPACE_TEXT) }
        }
        ENTER_IDS.map { id -> findViewById<KeyView>(id) }.forEach { key ->
            key.onPress = { listener?.onEnter() }
        }
        findViewById<KeyView>(R.id.key_symbols).onPress = { showLayer(KeyboardLayer.SYMBOLS) }
        findViewById<KeyView>(R.id.key_abc_symbols).onPress = { showLayer(KeyboardLayer.LETTERS) }
        findViewById<KeyView>(R.id.key_alt).onPress = { showLayer(KeyboardLayer.SYMBOLS_ALT) }
        findViewById<KeyView>(R.id.key_abc_alt).onPress = { showLayer(KeyboardLayer.LETTERS) }
        findViewById<KeyView>(R.id.key_numbers_back).onPress = { showLayer(KeyboardLayer.SYMBOLS) }
        findViewById<KeyView>(R.id.key_abc_phone).onPress = { showLayer(KeyboardLayer.LETTERS) }
        findViewById<KeyView>(R.id.key_alt_phone).onPress = { showLayer(KeyboardLayer.SYMBOLS_ALT) }
        findViewById<KeyView>(R.id.key_abc_number).onPress = { showLayer(KeyboardLayer.LETTERS) }
        findViewById<KeyView>(R.id.key_alt_number).onPress = { showLayer(KeyboardLayer.SYMBOLS_ALT) }
        layers = LAYER_IDS.map { id -> findViewById<View>(id) }
        showLayer(KeyboardLayer.LETTERS)
    }

    fun setListener(listener: KeyboardActionListener?) {
        this.listener = listener
    }

    fun resetToLetters() {
        dismissAccentPopup()
        keyboardState.reset()
        showLayer(KeyboardLayer.LETTERS)
        refreshLetterLabels()
    }

    fun applyInputType(inputType: Int) {
        val target = InputTypeLayouts.layerFor(inputType)
        if (target == KeyboardLayer.LETTERS &&
            (currentLayer == KeyboardLayer.LETTERS ||
                currentLayer == KeyboardLayer.SYMBOLS ||
                currentLayer == KeyboardLayer.SYMBOLS_ALT)
        ) {
            return
        }
        showLayer(target)
    }

    override fun onDetachedFromWindow() {
        dismissAccentPopup()
        super.onDetachedFromWindow()
    }

    private fun onLetterPressed(key: KeyView) {
        commitLetterText(key.text.toString())
    }

    private fun commitLetterText(text: String) {
        listener?.onText(text)
        keyboardState.consumeLetter()
        refreshLetterLabels()
    }

    private fun showAccentPopup(key: KeyView) {
        dismissAccentPopup()
        val baseLabel = key.text.toString()
        val labels = listOf(baseLabel) + AccentMap.variants(baseLabel[0])
            .map { keyboardState.applyTo(it.toString()) }
        val screenMargin = resources.getDimensionPixelSize(R.dimen.accent_popup_screen_margin)
        val containerPadding = resources.getDimensionPixelSize(R.dimen.accent_popup_padding)
        val keyMargin = resources.getDimensionPixelSize(R.dimen.key_padding)
        val desiredWidth = resources.getDimensionPixelSize(R.dimen.accent_popup_candidate_width)
        val minCandidateWidth = desiredWidth / MIN_CANDIDATE_WIDTH_DIVISOR
        val overhead = labels.size * keyMargin * 2 + containerPadding * 2
        val available = resources.displayMetrics.widthPixels - screenMargin * 2 - overhead
        val candidateWidth = (available / labels.size).coerceIn(minCandidateWidth, desiredWidth)
        val candidateHeight = resources.getDimensionPixelSize(R.dimen.key_height)
        val textColor = ContextCompat.getColor(context, R.color.key_text)
        val textSizePx = resources.getDimension(R.dimen.key_text_size)
        val container = LinearLayout(context)
        container.orientation = LinearLayout.HORIZONTAL
        container.setBackgroundResource(R.drawable.accent_popup_background)
        container.setPadding(containerPadding, containerPadding, containerPadding, containerPadding)
        labels.forEach { label ->
            val candidate = KeyView(context)
            candidate.text = label
            candidate.contentDescription = label
            candidate.setTextColor(textColor)
            candidate.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            candidate.onPress = {
                commitLetterText(label)
                dismissAccentPopup()
            }
            val params = LinearLayout.LayoutParams(candidateWidth, candidateHeight)
            params.setMargins(keyMargin, 0, keyMargin, 0)
            container.addView(candidate, params)
            accentCandidates.add(candidate)
        }
        accentOriginKey = key
        container.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val popupWidth = container.measuredWidth
        val popup = PopupWindow(
            container,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false,
        )
        popup.elevation = resources.getDimension(R.dimen.accent_popup_elevation)
        accentPopup = popup
        val windowLocation = IntArray(2)
        key.rootView.getLocationOnScreen(windowLocation)
        val keyLocation = IntArray(2)
        key.getLocationOnScreen(keyLocation)
        val gap = resources.getDimensionPixelSize(R.dimen.accent_popup_gap)
        val aboveY = keyLocation[1] - gap - container.measuredHeight
        val popupTopY = if (aboveY >= windowLocation[1]) {
            aboveY
        } else {
            keyLocation[1] + key.height + gap
        }
        val x = keyLocation[0] + key.width / 2 - popupWidth / 2 - windowLocation[0]
        val y = popupTopY - windowLocation[1]
        popup.showAtLocation(key, Gravity.NO_GRAVITY, x, y)
    }

    private fun dispatchToAccentPopup(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> trackAccentPopup(event.rawX, event.rawY)
            MotionEvent.ACTION_UP -> releaseAccentPopup(event.rawX, event.rawY)
            MotionEvent.ACTION_CANCEL -> dismissAccentPopup()
        }
    }

    private fun trackAccentPopup(rawX: Float, rawY: Float) {
        val index = accentIndexAt(rawX, rawY)
        if (index == accentHighlight) {
            return
        }
        accentCandidates.forEachIndexed { candidateIndex, candidate ->
            candidate.isPressed = candidateIndex == index
        }
        accentHighlight = index
    }

    private fun releaseAccentPopup(rawX: Float, rawY: Float) {
        val index = accentIndexAt(rawX, rawY)
        val text = when {
            isOverOriginKey(rawX, rawY) -> accentOriginKey?.text?.toString()
            index >= 0 -> accentCandidates[index].text.toString()
            else -> null
        }
        dismissAccentPopup()
        if (text != null) {
            commitLetterText(text)
        }
    }

    private fun accentIndexAt(rawX: Float, rawY: Float): Int {
        val container = accentPopup?.contentView ?: return -1
        val containerLocation = IntArray(2)
        container.getLocationOnScreen(containerLocation)
        val top = containerLocation[1]
        val bottom = top + container.height
        if (rawY < top - touchSlopPx || rawY > bottom + touchSlopPx) {
            return -1
        }
        val left = containerLocation[0]
        if (rawX < left || rawX > left + container.width) {
            return -1
        }
        var bestIndex = -1
        var bestDistance = Float.MAX_VALUE
        accentCandidates.forEachIndexed { index, candidate ->
            val location = IntArray(2)
            candidate.getLocationOnScreen(location)
            val center = location[0] + candidate.width / 2f
            val distance = abs(rawX - center)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun isOverOriginKey(rawX: Float, rawY: Float): Boolean {
        val key = accentOriginKey ?: return false
        val location = IntArray(2)
        key.getLocationOnScreen(location)
        return rawX >= location[0] - touchSlopPx &&
            rawX <= location[0] + key.width + touchSlopPx &&
            rawY >= location[1] - touchSlopPx &&
            rawY <= location[1] + key.height + touchSlopPx
    }

    private fun dismissAccentPopup() {
        accentPopup?.dismiss()
        accentPopup = null
        accentOriginKey = null
        accentCandidates.clear()
        accentHighlight = -1
    }

    private fun refreshLetterLabels() {
        letterKeys.forEach { key ->
            val cased = keyboardState.applyTo(key.text.toString())
            key.text = cased
            key.contentDescription = cased
        }
        letterShiftKey.isKeyValueChecked = keyboardState.isUppercase()
    }

    private fun showLayer(layer: KeyboardLayer) {
        currentLayer = layer
        layers.forEachIndexed { index, view ->
            view.visibility = if (index == layer.ordinal) VISIBLE else GONE
        }
        val shiftEnabled = layer == KeyboardLayer.LETTERS
        shiftKeys.forEach { it.isEnabled = shiftEnabled }
    }

    companion object {
        private const val SPACE_TEXT = " "
        private const val ACCENT_HOLD_TIMEOUT_MS = 350L
        private const val MIN_CANDIDATE_WIDTH_DIVISOR = 2
        private val LETTER_IDS = intArrayOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m,
        )
        private val SHIFT_IDS = intArrayOf(
            R.id.key_shift,
        )
        private val BACKSPACE_IDS = intArrayOf(
            R.id.key_backspace, R.id.key_backspace_symbols, R.id.key_backspace_alt,
            R.id.key_backspace_phone, R.id.key_backspace_number,
        )
        private val SCAN_IDS = intArrayOf(
            R.id.key_scan,
        )
        private val HIST_IDS = intArrayOf(
            R.id.key_hist,
        )
        private val SPACE_IDS = intArrayOf(
            R.id.key_space, R.id.key_space_symbols, R.id.key_space_alt,
            R.id.key_space_phone, R.id.key_space_number,
        )
        private val ENTER_IDS = intArrayOf(
            R.id.key_enter, R.id.key_enter_symbols, R.id.key_enter_alt,
            R.id.key_enter_phone, R.id.key_enter_number,
        )
        private val LAYER_IDS = intArrayOf(
            R.id.layer_letters, R.id.layer_symbols, R.id.layer_symbols_alt,
            R.id.layer_phone, R.id.layer_number,
        )
        private val SYMBOL_TEXT_IDS = intArrayOf(
            R.id.key_num_1, R.id.key_num_2, R.id.key_num_3, R.id.key_num_4, R.id.key_num_5,
            R.id.key_num_6, R.id.key_num_7, R.id.key_num_8, R.id.key_num_9, R.id.key_num_0,
            R.id.key_sym_at, R.id.key_sym_hash, R.id.key_sym_dollar, R.id.key_sym_percent,
            R.id.key_sym_ampersand, R.id.key_sym_hyphen, R.id.key_sym_plus,
            R.id.key_sym_open_paren, R.id.key_sym_close_paren,
            R.id.key_sym_asterisk, R.id.key_sym_double_quote, R.id.key_sym_apostrophe,
            R.id.key_sym_colon, R.id.key_sym_semicolon, R.id.key_sym_comma, R.id.key_sym_period,
            R.id.key_sym_question, R.id.key_sym_exclamation, R.id.key_sym_slash,
            R.id.key_period_letters, R.id.key_period_symbols, R.id.key_period_alt,
            R.id.key_period_phone, R.id.key_period_number,
            R.id.key_ph_num_1, R.id.key_ph_num_2, R.id.key_ph_num_3, R.id.key_ph_num_4,
            R.id.key_ph_num_5, R.id.key_ph_num_6, R.id.key_ph_num_7, R.id.key_ph_num_8,
            R.id.key_ph_num_9, R.id.key_ph_num_0,
            R.id.key_ph_open_paren, R.id.key_ph_close_paren, R.id.key_ph_plus,
            R.id.key_ph_hyphen, R.id.key_ph_period, R.id.key_ph_comma, R.id.key_ph_colon,
            R.id.key_ph_semicolon, R.id.key_ph_exclamation,
            R.id.key_ph_asterisk, R.id.key_ph_hash, R.id.key_ph_apostrophe,
            R.id.key_ph_double_quote, R.id.key_ph_at, R.id.key_ph_ampersand,
            R.id.key_ph_percent, R.id.key_ph_slash, R.id.key_ph_underscore, R.id.key_ph_tilde,
            R.id.key_nm_num_1, R.id.key_nm_num_2, R.id.key_nm_num_3, R.id.key_nm_num_4,
            R.id.key_nm_num_5, R.id.key_nm_num_6, R.id.key_nm_num_7, R.id.key_nm_num_8,
            R.id.key_nm_num_9, R.id.key_nm_num_0,
            R.id.key_nm_hyphen, R.id.key_nm_slash, R.id.key_nm_colon, R.id.key_nm_semicolon,
            R.id.key_nm_open_paren, R.id.key_nm_close_paren, R.id.key_nm_dollar,
            R.id.key_nm_ampersand, R.id.key_nm_at,
            R.id.key_nm_period, R.id.key_nm_comma, R.id.key_nm_question,
            R.id.key_nm_exclamation, R.id.key_nm_apostrophe, R.id.key_nm_equals,
            R.id.key_nm_plus, R.id.key_nm_underscore, R.id.key_nm_hash, R.id.key_nm_percent,
            R.id.key_alt_num_1, R.id.key_alt_num_2, R.id.key_alt_num_3, R.id.key_alt_num_4,
            R.id.key_alt_num_5, R.id.key_alt_num_6, R.id.key_alt_num_7, R.id.key_alt_num_8,
            R.id.key_alt_num_9, R.id.key_alt_num_0,
            R.id.key_alt_paren_open, R.id.key_alt_brace_open, R.id.key_alt_brace_close,
            R.id.key_alt_bracket_open, R.id.key_alt_bracket_close,
            R.id.key_alt_backslash, R.id.key_alt_pipe, R.id.key_alt_tilde, R.id.key_alt_less,
            R.id.key_alt_greater, R.id.key_alt_equals, R.id.key_alt_degree,
        )
    }
}
