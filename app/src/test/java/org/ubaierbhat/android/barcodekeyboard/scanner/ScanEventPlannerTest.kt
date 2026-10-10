package org.ubaierbhat.android.barcodekeyboard.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanEventPlannerTest {

    @Test
    fun `one shot plain scan plans a single commit`() {
        assertEquals(
            listOf(ScanEvent.Type("AAA111")),
            ScanEventPlanner.plan("AAA111", continuous = false, translateActions = false)
        )
    }

    @Test
    fun `continuous plain scan appends an enter terminator`() {
        assertEquals(
            listOf(ScanEvent.Type("AAA111"), ScanEvent.Enter),
            ScanEventPlanner.plan("AAA111", continuous = true, translateActions = false)
        )
    }

    @Test
    fun `plain scan keeps newline characters literal`() {
        assertEquals(
            listOf(ScanEvent.Type("A\nB")),
            ScanEventPlanner.plan("A\nB", continuous = false, translateActions = false)
        )
    }

    @Test
    fun `translate plans payload specials as events without terminator`() {
        assertEquals(
            listOf(ScanEvent.Type("A"), ScanEvent.Enter, ScanEvent.Type("B")),
            ScanEventPlanner.plan("A\nB", continuous = false, translateActions = true)
        )
    }

    @Test
    fun `continuous translate appends terminator when payload has no trailing enter`() {
        assertEquals(
            listOf(ScanEvent.Type("AAA111"), ScanEvent.Enter),
            ScanEventPlanner.plan("AAA111", continuous = true, translateActions = true)
        )
    }

    @Test
    fun `continuous translate does not double the trailing enter`() {
        assertEquals(
            listOf(ScanEvent.Type("A"), ScanEvent.Enter),
            ScanEventPlanner.plan("A\n", continuous = true, translateActions = true)
        )
    }

    @Test
    fun `continuous translate ends with enter even when payload ends with tab`() {
        assertEquals(
            listOf(ScanEvent.Type("A"), ScanEvent.Tab, ScanEvent.Enter),
            ScanEventPlanner.plan("A\t", continuous = true, translateActions = true)
        )
    }

    @Test
    fun `empty payload plans no events`() {
        assertEquals(
            emptyList<ScanEvent>(),
            ScanEventPlanner.plan("", continuous = true, translateActions = true)
        )
    }
}
