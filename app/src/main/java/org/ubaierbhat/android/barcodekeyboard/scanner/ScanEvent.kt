package org.ubaierbhat.android.barcodekeyboard.scanner

sealed interface ScanEvent {
    data class Type(val text: String) : ScanEvent
    data object Enter : ScanEvent
    data object Tab : ScanEvent
}
