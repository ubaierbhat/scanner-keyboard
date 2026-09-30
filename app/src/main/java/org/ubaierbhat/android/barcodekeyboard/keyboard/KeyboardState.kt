package org.ubaierbhat.android.barcodekeyboard.keyboard

enum class ShiftMode {
    OFF,
    ON,
    LOCKED,
}

class KeyboardState {

    var shiftMode: ShiftMode = ShiftMode.OFF
        private set

    fun toggle() {
        shiftMode = if (shiftMode == ShiftMode.OFF) ShiftMode.ON else ShiftMode.OFF
    }

    fun toggleLock() {
        shiftMode = if (shiftMode == ShiftMode.LOCKED) ShiftMode.OFF else ShiftMode.LOCKED
    }

    fun applyTo(letter: String): String =
        if (isUppercase()) letter.uppercase() else letter.lowercase()

    fun consumeLetter() {
        if (shiftMode == ShiftMode.ON) {
            shiftMode = ShiftMode.OFF
        }
    }

    fun isUppercase(): Boolean = shiftMode != ShiftMode.OFF

    fun reset() {
        shiftMode = ShiftMode.OFF
    }
}
