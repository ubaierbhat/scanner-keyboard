package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

object PasswordFieldDetector {

    fun isPasswordField(info: EditorInfo?): Boolean =
        info != null && isPasswordField(info.inputType)

    fun isPasswordField(inputType: Int): Boolean =
        when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> inputType and InputType.TYPE_MASK_VARIATION in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            )
            InputType.TYPE_CLASS_NUMBER ->
                inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
}
