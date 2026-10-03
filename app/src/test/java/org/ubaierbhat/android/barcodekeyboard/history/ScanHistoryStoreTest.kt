package org.ubaierbhat.android.barcodekeyboard.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScanHistoryStoreTest {

    private fun newStore(): ScanHistoryStore = ScanHistoryStore(ApplicationProvider.getApplicationContext())

    @Test
    fun addPersistsAndReadsBackAcrossInstances() {
        newStore().add("https://example.com")
        assertEquals(listOf("https://example.com"), newStore().entries())
        newStore().add("second")
        assertEquals(listOf("second", "https://example.com"), newStore().entries())
    }

    @Test
    fun addTrimsEntry() {
        val store = newStore()
        store.add("  padded  ")
        assertEquals(listOf("padded"), store.entries())
    }

    @Test
    fun addIgnoresBlankEntries() {
        val store = newStore()
        store.add("keep")
        store.add("")
        store.add("   ")
        store.add("\t\n ")
        assertEquals(listOf("keep"), store.entries())
    }

    @Test
    fun duplicateMovesToMostRecent() {
        val store = newStore()
        store.add("a")
        store.add("b")
        store.add("c")
        store.add("a")
        assertEquals(listOf("a", "c", "b"), store.entries())
        assertEquals(3, newStore().entries().size)
    }

    @Test
    fun capKeepsTwentyMostRecentEntries() {
        val store = newStore()
        for (index in 1..25) {
            store.add("value-$index")
        }
        val entries = newStore().entries()
        assertEquals(20, entries.size)
        assertEquals("value-25", entries.first())
        assertEquals("value-6", entries.last())
    }

    @Test
    fun clearEmptiesStoreAcrossInstances() {
        newStore().add("x")
        newStore().clear()
        assertEquals(emptyList<String>(), newStore().entries())
    }

    @Test
    fun corruptJsonResetsStoreSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
            .edit()
            .putString("entries", "{ this is not a json array")
            .commit()
        assertEquals(emptyList<String>(), newStore().entries())
        val store = newStore()
        store.add("fresh")
        assertEquals(listOf("fresh"), store.entries())
    }
}
