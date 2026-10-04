package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class InputTypeLayoutsTest {

    @Test
    fun phoneClassSelectsPhoneLayout() {
        assertEquals(KeyboardLayer.PHONE, InputTypeLayouts.layerFor(InputType.TYPE_CLASS_PHONE))
    }

    @Test
    fun numberClassSelectsNumberLayout() {
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        assertEquals(KeyboardLayer.NUMBER, InputTypeLayouts.layerFor(inputType))
    }

    @Test
    fun dateTimeClassSelectsNumberLayout() {
        assertEquals(KeyboardLayer.NUMBER, InputTypeLayouts.layerFor(InputType.TYPE_CLASS_DATETIME))
    }

    @Test
    fun textClassSelectsLetters() {
        assertEquals(KeyboardLayer.LETTERS, InputTypeLayouts.layerFor(InputType.TYPE_CLASS_TEXT))
    }

    @Test
    fun multiLineTextSelectsLetters() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        assertEquals(KeyboardLayer.LETTERS, InputTypeLayouts.layerFor(inputType))
    }

    @Test
    fun emailVariationSelectsLetters() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        assertEquals(KeyboardLayer.LETTERS, InputTypeLayouts.layerFor(inputType))
    }

    @Test
    fun nullClassSelectsLetters() {
        assertEquals(KeyboardLayer.LETTERS, InputTypeLayouts.layerFor(InputType.TYPE_NULL))
    }
}
