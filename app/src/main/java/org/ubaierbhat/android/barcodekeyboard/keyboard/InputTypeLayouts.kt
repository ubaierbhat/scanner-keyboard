package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType

object InputTypeLayouts {

    fun layerFor(inputType: Int): KeyboardLayer =
        when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_PHONE -> KeyboardLayer.PHONE
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME -> KeyboardLayer.NUMBER
            else -> KeyboardLayer.LETTERS
        }
}
