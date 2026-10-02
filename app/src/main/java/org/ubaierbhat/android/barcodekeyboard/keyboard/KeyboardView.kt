package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import org.ubaierbhat.android.barcodekeyboard.R

enum class KeyboardLayer {
    LETTERS,
    SYMBOLS,
    SYMBOLS_ALT,
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

    init {
        LayoutInflater.from(context).inflate(R.layout.keyboard_view, this)
        letterKeys = LETTER_IDS.map { id -> findViewById<KeyView>(id) }
        letterKeys.forEach { key ->
            key.onPress = { onLetterPressed(key) }
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
        layers = LAYER_IDS.map { id -> findViewById<View>(id) }
        showLayer(KeyboardLayer.LETTERS)
    }

    fun setListener(listener: KeyboardActionListener?) {
        this.listener = listener
    }

    fun resetToLetters() {
        keyboardState.reset()
        showLayer(KeyboardLayer.LETTERS)
        refreshLetterLabels()
    }

    private fun onLetterPressed(key: KeyView) {
        listener?.onText(key.text.toString())
        keyboardState.consumeLetter()
        refreshLetterLabels()
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
        layers.forEachIndexed { index, view ->
            view.visibility = if (index == layer.ordinal) VISIBLE else GONE
        }
        val shiftEnabled = layer == KeyboardLayer.LETTERS
        shiftKeys.forEach { it.isEnabled = shiftEnabled }
    }

    companion object {
        private const val SPACE_TEXT = " "
        private val LETTER_IDS = intArrayOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m,
        )
        private val SHIFT_IDS = intArrayOf(
            R.id.key_shift, R.id.key_shift_symbols, R.id.key_shift_alt,
        )
        private val BACKSPACE_IDS = intArrayOf(
            R.id.key_backspace, R.id.key_backspace_symbols, R.id.key_backspace_alt,
        )
        private val SCAN_IDS = intArrayOf(
            R.id.key_scan, R.id.key_scan_symbols, R.id.key_scan_alt,
        )
        private val HIST_IDS = intArrayOf(
            R.id.key_hist, R.id.key_hist_symbols, R.id.key_hist_alt,
        )
        private val SPACE_IDS = intArrayOf(
            R.id.key_space, R.id.key_space_symbols, R.id.key_space_alt,
        )
        private val ENTER_IDS = intArrayOf(
            R.id.key_enter, R.id.key_enter_symbols, R.id.key_enter_alt,
        )
        private val LAYER_IDS = intArrayOf(
            R.id.layer_letters, R.id.layer_symbols, R.id.layer_symbols_alt,
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
