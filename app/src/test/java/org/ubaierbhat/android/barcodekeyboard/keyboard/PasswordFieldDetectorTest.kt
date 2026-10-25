package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordFieldDetectorTest {

    private fun info(inputType: Int): EditorInfo =
        EditorInfo().apply { this.inputType = inputType }

    @Test
    fun textPasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun visiblePasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun webPasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun numberPasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun plainTextIsNotPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        assertFalse(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun plainNumberIsNotPasswordField() {
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        assertFalse(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun nullTypeIsNotPasswordField() {
        assertFalse(PasswordFieldDetector.isPasswordField(InputType.TYPE_NULL))
    }

    @Test
    fun nullEditorInfoIsNotPasswordField() {
        assertFalse(PasswordFieldDetector.isPasswordField(null as EditorInfo?))
    }

    @Test
    fun editorInfoDelegatesToInputType() {
        val password = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(info(password)))
        assertFalse(PasswordFieldDetector.isPasswordField(info(InputType.TYPE_CLASS_TEXT)))
    }
}
