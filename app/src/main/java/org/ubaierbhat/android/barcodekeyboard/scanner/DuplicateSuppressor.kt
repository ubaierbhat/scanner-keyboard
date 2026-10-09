package org.ubaierbhat.android.barcodekeyboard.scanner

class DuplicateSuppressor {

    private var lastValue: String? = null

    fun shouldEmit(value: String): Boolean {
        if (value == lastValue) {
            return false
        }
        lastValue = value
        return true
    }

    fun reset() {
        lastValue = null
    }
}
