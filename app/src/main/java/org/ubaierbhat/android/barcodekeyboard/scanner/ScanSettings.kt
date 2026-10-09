package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import android.content.SharedPreferences

class ScanSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var continuousScan: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS, false)
        set(value) = prefs.edit().putBoolean(KEY_CONTINUOUS, value).apply()

    var translateScanActions: Boolean
        get() = prefs.getBoolean(KEY_TRANSLATE, false)
        set(value) = prefs.edit().putBoolean(KEY_TRANSLATE, value).apply()

    private companion object {
        const val PREFS_NAME = "scanner_settings"
        const val KEY_CONTINUOUS = "continuous_scan"
        const val KEY_TRANSLATE = "translate_scan_actions"
    }
}
