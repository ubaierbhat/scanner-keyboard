package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

sealed interface EnterBehavior {
    data class PerformAction(val actionId: Int) : EnterBehavior
    data object Newline : EnterBehavior
    data object SendKeyEvent : EnterBehavior
}

object EnterActionResolver {

    fun resolve(editorInfo: EditorInfo): EnterBehavior {
        val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
        return when {
            editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0 ->
                EnterBehavior.SendKeyEvent
            action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED ->
                EnterBehavior.PerformAction(action)
            editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0 ->
                EnterBehavior.Newline
            else ->
                EnterBehavior.SendKeyEvent
        }
    }
}
