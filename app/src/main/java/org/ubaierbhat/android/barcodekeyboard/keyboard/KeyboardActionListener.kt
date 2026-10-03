package org.ubaierbhat.android.barcodekeyboard.keyboard

interface KeyboardActionListener {
    fun onText(text: String)
    fun onBackspace()
    fun onEnter()
    fun onScanRequested()
    fun onHistoryRequested()
}
