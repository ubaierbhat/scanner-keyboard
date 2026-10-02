package org.ubaierbhat.android.barcodekeyboard.history

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException

class ScanHistoryStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(entry: String) {
        val trimmed = entry.trim()
        if (trimmed.isEmpty()) {
            return
        }
        val updated = entries().toMutableList()
        updated.remove(trimmed)
        updated.add(0, trimmed)
        while (updated.size > MAX_ENTRIES) {
            updated.removeAt(updated.size - 1)
        }
        persist(updated)
    }

    fun entries(): List<String> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        } catch (error: JSONException) {
            reset()
            emptyList()
        }
    }

    fun clear() {
        reset()
    }

    private fun reset() {
        prefs.edit().remove(KEY_ENTRIES).commit()
    }

    private fun persist(entries: List<String>) {
        val array = JSONArray()
        entries.forEach { array.put(it) }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).commit()
    }

    private companion object {
        private const val PREFS_NAME = "scan_history"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_ENTRIES = 20
    }
}
