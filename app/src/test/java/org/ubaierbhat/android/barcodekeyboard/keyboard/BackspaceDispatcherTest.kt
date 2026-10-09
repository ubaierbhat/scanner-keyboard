package org.ubaierbhat.android.barcodekeyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class BackspaceDispatcherTest {

    @Test
    fun plansSelectionDeleteWhenTextSelected() {
        assertEquals(
            BackspaceDispatcher.Action.DeleteSelection,
            BackspaceDispatcher.plan("cd"),
        )
    }

    @Test
    fun plansSingleDeleteWhenNothingSelected() {
        assertEquals(
            BackspaceDispatcher.Action.DeleteOneBeforeCursor,
            BackspaceDispatcher.plan(null),
        )
        assertEquals(
            BackspaceDispatcher.Action.DeleteOneBeforeCursor,
            BackspaceDispatcher.plan(""),
        )
    }
}
