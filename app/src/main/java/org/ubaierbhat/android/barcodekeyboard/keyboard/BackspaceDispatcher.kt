package org.ubaierbhat.android.barcodekeyboard.keyboard

object BackspaceDispatcher {

    sealed interface Action {
        data object DeleteSelection : Action
        data object DeleteOneBeforeCursor : Action
    }

    fun plan(selectedText: CharSequence?): Action =
        if (selectedText.isNullOrEmpty()) {
            Action.DeleteOneBeforeCursor
        } else {
            Action.DeleteSelection
        }
}
