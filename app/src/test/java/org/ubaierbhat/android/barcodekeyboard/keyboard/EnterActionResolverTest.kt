package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class EnterActionResolverTest {

    private fun editorInfo(imeOptions: Int = EditorInfo.IME_NULL, inputType: Int = InputType.TYPE_CLASS_TEXT): EditorInfo =
        EditorInfo().apply {
            this.imeOptions = imeOptions
            this.inputType = inputType
        }

    @Test
    fun searchActionResolvesToPerformAction() {
        val behavior = EnterActionResolver.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_SEARCH))
        assertEquals(EnterBehavior.PerformAction(EditorInfo.IME_ACTION_SEARCH), behavior)
    }

    @Test
    fun doneActionResolvesToPerformAction() {
        val behavior = EnterActionResolver.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_DONE))
        assertEquals(EnterBehavior.PerformAction(EditorInfo.IME_ACTION_DONE), behavior)
    }

    @Test
    fun nextActionResolvesToPerformAction() {
        val behavior = EnterActionResolver.resolve(editorInfo(imeOptions = EditorInfo.IME_ACTION_NEXT))
        assertEquals(EnterBehavior.PerformAction(EditorInfo.IME_ACTION_NEXT), behavior)
    }

    @Test
    fun actionWithFlagsStillResolvesToPerformAction() {
        val imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_FULLSCREEN
        val behavior = EnterActionResolver.resolve(editorInfo(imeOptions = imeOptions))
        assertEquals(EnterBehavior.PerformAction(EditorInfo.IME_ACTION_GO), behavior)
    }

    @Test
    fun actionNoneWithMultilineResolvesToNewline() {
        val multiline = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        val behavior = EnterActionResolver.resolve(
            editorInfo(imeOptions = EditorInfo.IME_ACTION_NONE, inputType = multiline),
        )
        assertEquals(EnterBehavior.Newline, behavior)
    }

    @Test
    fun unspecifiedActionWithMultilineResolvesToNewline() {
        val multiline = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        val behavior = EnterActionResolver.resolve(
            editorInfo(imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED, inputType = multiline),
        )
        assertEquals(EnterBehavior.Newline, behavior)
    }

    @Test
    fun actionTakesPriorityOverMultiline() {
        val multiline = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        val behavior = EnterActionResolver.resolve(
            editorInfo(imeOptions = EditorInfo.IME_ACTION_SEND, inputType = multiline),
        )
        assertEquals(EnterBehavior.PerformAction(EditorInfo.IME_ACTION_SEND), behavior)
    }

    @Test
    fun plainSingleLineResolvesToSendKeyEvent() {
        val behavior = EnterActionResolver.resolve(editorInfo())
        assertEquals(EnterBehavior.SendKeyEvent, behavior)
    }

    @Test
    fun numberFieldResolvesToSendKeyEvent() {
        val behavior = EnterActionResolver.resolve(editorInfo(inputType = InputType.TYPE_CLASS_NUMBER))
        assertEquals(EnterBehavior.SendKeyEvent, behavior)
    }
}
