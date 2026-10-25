package org.ubaierbhat.android.barcodekeyboard.service

import android.Manifest
import android.app.Application
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONArray
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.ubaierbhat.android.barcodekeyboard.R

@RunWith(RobolectricTestRunner::class)
class BarcodeKeyboardServiceScanHistoryTest {

    private lateinit var service: BarcodeKeyboardService
    private lateinit var prefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        application.setTheme(R.style.Theme_ScannerKeyboard)
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        service = Robolectric.setupService(BarcodeKeyboardService::class.java)
        service.onCreateInputView()
        val requestQuietly = { try { service.onScanRequested() } catch (ignored: Throwable) {} }
        requestQuietly()
        prefs = service.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
        prefs.edit().remove("entries").commit()
    }

    @After
    fun tearDown() {
        prefs.edit().remove("entries").commit()
    }

    private fun history(): JSONArray =
        JSONArray(prefs.getString("entries", "[]") ?: "[]")

    private fun startInput(inputType: Int) {
        val info = EditorInfo().apply { this.inputType = inputType }
        val field = InputMethodService::class.java.getDeclaredField("mInputEditorInfo")
        field.isAccessible = true
        field.set(service, info)
    }

    private fun startInput(info: EditorInfo?) {
        val field = InputMethodService::class.java.getDeclaredField("mInputEditorInfo")
        field.isAccessible = true
        field.set(service, info)
    }

    private fun injectScan(text: String) {
        val method = BarcodeKeyboardService::class.java
            .getDeclaredMethod("handleBarcodeResult", String::class.java)
        method.isAccessible = true
        method.invoke(service, text)
    }

    private fun reopenScannerQuietly() {
        try {
            service.onScanRequested()
        } catch (ignored: Throwable) {
        }
    }

    @Test
    fun scanIntoTextFieldIsRecorded() {
        startInput(InputType.TYPE_CLASS_TEXT)
        injectScan("normal123")
        assertEquals("normal123", history().getString(0))
    }

    @Test
    fun scanWithNullEditorInfoIsRecorded() {
        startInput(null as EditorInfo?)
        injectScan("failOpen1")
        assertEquals("failOpen1", history().getString(0))
    }

    @Test
    fun scanWithNullInputTypeIsRecorded() {
        startInput(InputType.TYPE_NULL)
        injectScan("failOpen2")
        assertEquals("failOpen2", history().getString(0))
    }

    @Test
    fun scanIntoPasswordFieldIsNotRecorded() {
        startInput(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        injectScan("secretPass1")
        assertEquals(0, history().length())
    }

    @Test
    fun scanRecordFollowsFieldAtScanTime() {
        startInput(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        injectScan("secretPass2")
        reopenScannerQuietly()
        startInput(InputType.TYPE_CLASS_TEXT)
        injectScan("normal456")
        assertEquals(1, history().length())
        assertEquals("normal456", history().getString(0))
    }
}
