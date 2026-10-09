package org.ubaierbhat.android.barcodekeyboard.scanner

object ScanTranslator {

    fun translate(raw: String): List<ScanEvent> {
        val events = mutableListOf<ScanEvent>()
        val segment = StringBuilder()

        fun flushSegment() {
            if (segment.isNotEmpty()) {
                events.add(ScanEvent.Type(segment.toString()))
                segment.setLength(0)
            }
        }

        fun add(event: ScanEvent) {
            flushSegment()
            val last = events.lastOrNull()
            if (last == event) {
                return
            }
            events.add(event)
        }

        var index = 0
        while (index < raw.length) {
            val char = raw[index]
            when {
                char == '\r' && index + 1 < raw.length && raw[index + 1] == '\n' -> {
                    add(ScanEvent.Enter)
                    index += 2
                }
                char == '\r' || char == '\n' -> {
                    add(ScanEvent.Enter)
                    index++
                }
                char == '\t' -> {
                    add(ScanEvent.Tab)
                    index++
                }
                else -> {
                    segment.append(char)
                    index++
                }
            }
        }
        flushSegment()
        return events
    }
}
