package org.ubaierbhat.android.barcodekeyboard.keyboard

object EnterDispatcher {

    fun dispatch(
        behavior: EnterBehavior,
        performAction: (Int) -> Boolean,
        sendNewline: () -> Unit,
        sendEnterKey: () -> Unit,
    ) {
        when (behavior) {
            is EnterBehavior.PerformAction -> {
                if (!performAction(behavior.actionId)) {
                    sendEnterKey()
                }
            }
            EnterBehavior.Newline -> sendNewline()
            EnterBehavior.SendKeyEvent -> sendEnterKey()
        }
    }
}
