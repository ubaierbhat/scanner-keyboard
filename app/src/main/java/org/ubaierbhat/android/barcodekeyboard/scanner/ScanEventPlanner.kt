package org.ubaierbhat.android.barcodekeyboard.scanner

/**
 * Decides the [ScanEvent] chain for one accepted scan.
 *
 * Continuous mode terminates every scan with [ScanEvent.Enter] — a line break in
 * multiline fields, the field's editor action (Done/Go/Search/Send/Next) elsewhere —
 * so successive codes stack one per line like a hardware scanner wedge. A payload
 * that already ends in Enter (Form mode with a trailing newline) is not doubled.
 */
object ScanEventPlanner {

    fun plan(raw: String, continuous: Boolean, translateActions: Boolean): List<ScanEvent> {
        val events = when {
            raw.isEmpty() -> emptyList()
            translateActions -> ScanTranslator.translate(raw)
            else -> listOf(ScanEvent.Type(raw))
        }
        return if (continuous && events.isNotEmpty() && events.last() != ScanEvent.Enter) {
            events + ScanEvent.Enter
        } else {
            events
        }
    }
}
