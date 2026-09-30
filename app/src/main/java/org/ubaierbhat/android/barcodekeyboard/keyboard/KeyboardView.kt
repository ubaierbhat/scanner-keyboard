package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.ubaierbhat.android.barcodekeyboard.R

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var listener: KeyboardActionListener? = null
    private val letterKeys: List<KeyView>
    private val shiftKey: KeyView
    private var shifted = false

    init {
        LayoutInflater.from(context).inflate(R.layout.keyboard_view, this)
        letterKeys = LETTER_IDS.map { id -> findViewById<KeyView>(id) }
        letterKeys.forEach { key ->
            key.onPress = { listener?.onText(key.text.toString()) }
        }
        shiftKey = findViewById<KeyView>(R.id.key_shift).apply {
            onPress = { toggleShift() }
        }
        findViewById<KeyView>(R.id.key_backspace).onPress = { listener?.onBackspace() }
        findViewById<KeyView>(R.id.key_scan).onPress = { listener?.onScanRequested() }
        findViewById<KeyView>(R.id.key_space).onPress = { listener?.onText(SPACE_TEXT) }
        findViewById<KeyView>(R.id.key_enter).onPress = { listener?.onEnter() }
    }

    fun setListener(listener: KeyboardActionListener?) {
        this.listener = listener
    }

    fun resetToLetters() {
        applyShift(shifted = false)
    }

    private fun toggleShift() {
        applyShift(shifted = !shifted)
    }

    private fun applyShift(shifted: Boolean) {
        this.shifted = shifted
        shiftKey.isKeyValueChecked = shifted
        letterKeys.forEach { key ->
            val label = key.text.toString()
            key.text = if (shifted) label.uppercase() else label.lowercase()
        }
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
    }
}
