package org.ubaierbhat.android.barcodekeyboard.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanTranslatorTest {

    private fun type(text: String) = ScanEvent.Type(text)

    @Test
    fun plainTextIsSingleTypeEvent() {
        assertEquals(listOf(type("ABC123")), ScanTranslator.translate("ABC123"))
    }

    @Test
    fun newlineSplitsSegments() {
        assertEquals(
            listOf(type("A"), ScanEvent.Enter, type("B")),
            ScanTranslator.translate("A\nB"),
        )
    }

    @Test
    fun tabSplitsSegments() {
        assertEquals(
            listOf(type("A"), ScanEvent.Tab, type("B")),
            ScanTranslator.translate("A\tB"),
        )
    }

    @Test
    fun carriageReturnNewlineIsOneEnter() {
        assertEquals(
            listOf(type("A"), ScanEvent.Enter, type("B")),
            ScanTranslator.translate("A\r\nB"),
        )
    }

    @Test
    fun loneCarriageReturnIsEnter() {
        assertEquals(
            listOf(type("A"), ScanEvent.Enter),
            ScanTranslator.translate("A\r"),
        )
    }

    @Test
    fun consecutiveIdenticalSpecialsCollapseToOne() {
        assertEquals(
            listOf(type("A"), ScanEvent.Enter, type("B")),
            ScanTranslator.translate("A\n\nB"),
        )
        assertEquals(
            listOf(type("A"), ScanEvent.Tab, type("B")),
            ScanTranslator.translate("A\t\t\tB"),
        )
    }

    @Test
    fun differentSpecialsDoNotCollapse() {
        assertEquals(
            listOf(type("A"), ScanEvent.Enter, ScanEvent.Tab, type("B")),
            ScanTranslator.translate("A\n\tB"),
        )
    }

    @Test
    fun trailingNewlineFiresEnter() {
        assertEquals(
            listOf(type("ABC"), ScanEvent.Enter),
            ScanTranslator.translate("ABC\n"),
        )
    }

    @Test
    fun leadingTabFiresTab() {
        assertEquals(
            listOf(ScanEvent.Tab, type("ABC")),
            ScanTranslator.translate("\tABC"),
        )
    }

    @Test
    fun emptyStringProducesNoEvents() {
        assertEquals(emptyList<ScanEvent>(), ScanTranslator.translate(""))
    }

    @Test
    fun onlySpecialsProducesOnlyActions() {
        assertEquals(
            listOf(ScanEvent.Enter, ScanEvent.Tab),
            ScanTranslator.translate("\n\n\t"),
        )
    }
}
