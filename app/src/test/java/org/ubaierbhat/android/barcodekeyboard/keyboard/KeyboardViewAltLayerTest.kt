package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.ubaierbhat.android.barcodekeyboard.R

@RunWith(RobolectricTestRunner::class)
class KeyboardViewAltLayerTest {

    private class RecordingListener : KeyboardActionListener {
        val text = mutableListOf<String>()
        override fun onText(text: String) {
            this.text += text
        }

        override fun onBackspace() = Unit
        override fun onEnter() = Unit
        override fun onScanRequested() = Unit
        override fun onHistoryRequested() = Unit
        override fun onSettingsRequested() = Unit
    }

    private fun altKeys(): List<KeyView> {
        val keyboard = KeyboardView(ApplicationProvider.getApplicationContext())
        val layer = keyboard.findViewById<LinearLayout>(R.id.layer_symbols_alt)
        val row = layer.getChildAt(1) as LinearLayout
        return (0 until row.childCount).mapNotNull { row.getChildAt(it) as? KeyView }
    }

    private fun altKeysWith(listener: KeyboardActionListener): List<KeyView> {
        val keyboard = KeyboardView(ApplicationProvider.getApplicationContext())
        keyboard.setListener(listener)
        val layer = keyboard.findViewById<LinearLayout>(R.id.layer_symbols_alt)
        val row = layer.getChildAt(1) as LinearLayout
        return (0 until row.childCount).mapNotNull { row.getChildAt(it) as? KeyView }
    }

    @Test
    fun altRowOneExposesCodeAndCurrencySymbols() {
        val keys = altKeys()
        assertEquals(10, keys.size)
        val expected = listOf("(", ")", "{", "}", "[", "]", "`", "^", "_", "€")
        assertEquals(expected, keys.map { it.text.toString() })
    }

    @Test
    fun backtickAndCaretCommitText() {
        val listener = RecordingListener()
        val keys = altKeysWith(listener)
        keys.first { it.text.toString() == "`" }.performClick()
        keys.first { it.text.toString() == "^" }.performClick()
        keys.first { it.text.toString() == "€" }.performClick()
        keys.first { it.text.toString() == "_" }.performClick()
        assertEquals(listOf("`", "^", "€", "_"), listener.text)
    }
}
