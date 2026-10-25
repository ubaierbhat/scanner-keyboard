# Password-Field Scan History Exclusion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a barcode is scanned into a password field, the text is still typed into the field but is never written to scan history.

**Architecture:** A small pure detector (`PasswordFieldDetector`) classifies `EditorInfo.inputType` as a password field. `BarcodeKeyboardService.handleBarcodeResult` checks the detector against the live `currentInputEditorInfo` at scan time and skips `recordScan()` when it is a password field. Detection is also exposed as an `EditorInfo` overload so the service change is a single guarded call.

**Tech Stack:** Kotlin, JUnit4 unit tests, Robolectric for the service-level test (existing pattern in `app/src/test/.../service/`).

**Spec:** `docs/superpowers/specs/2026-10-25-password-scan-history-design.md`

## Global Constraints

- Kotlin; no new dependencies.
- Follow existing package layout: detector in `keyboard` package beside `InputTypeLayouts.kt`; tests under `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/`.
- Test style: JUnit4 (`org.junit.Assert` / `org.junit.Test`), plain JVM where possible, `@RunWith(RobolectricTestRunner::class)` only where Android framework classes need real behavior.
- No user-visible copy, strings, or settings change. Behavior is unconditional.
- Fail open: missing/non-password editor info still records history; only confirmed password field types are excluded.
- Commit style: conventional prefixes (`feat:`, `test:`).

## Review Focus

1. `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` ("show password" fields) — expected: treated as password, history skipped. Pinned by detector truth-table test (Task 1).
2. WebView password fields (`TYPE_TEXT_VARIATION_WEB_PASSWORD`) — expected: history skipped. Pinned by detector truth-table test (Task 1).
3. Numeric PIN fields (`TYPE_CLASS_TEXT`? no — `TYPE_CLASS_NUMBER` + `TYPE_NUMBER_VARIATION_PASSWORD`) — expected: history skipped. Pinned by detector truth-table test (Task 1).
4. Missing/`TYPE_NULL` editor info (e.g., scan before any `onStartInput`) — expected: fail open, scan IS recorded. Pinned by detector null/TYPE_NULL tests (Task 1) and by service test scenario B (Task 2).
5. Focus changing between scans in continuous mode (password field, then plain field) — expected: history record follows the field state at scan time, via read of `currentInputEditorInfo` at scan time. Pinned by service test scenario order (Task 2).

---

### Task 1: `PasswordFieldDetector`

**Files:**
- Create: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/keyboard/PasswordFieldDetector.kt`
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/keyboard/PasswordFieldDetectorTest.kt`

**Interfaces:**
- Consumes: nothing (pure helper on `android.text.InputType` constants and `android.view.inputmethod.EditorInfo`).
- Produces:
  - `object PasswordFieldDetector` with
    - `fun isPasswordField(inputType: Int): Boolean`
    - `fun isPasswordField(info: EditorInfo?): Boolean` (delegates to the int overload; null info → false)

- [ ] **Step 1: Write the failing truth-table test**

`PasswordFieldDetectorTest.kt`, plain JUnit4, mirroring `EnterActionResolverTest.kt` style:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordFieldDetectorTest {

    private fun info(inputType: Int): EditorInfo =
        EditorInfo().apply { this.inputType = inputType }

    @Test
    fun textPasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun visiblePasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun webPasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun numberPasswordIsPasswordField() {
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun plainTextIsNotPasswordField() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        assertFalse(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun plainNumberIsNotPasswordField() {
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        assertFalse(PasswordFieldDetector.isPasswordField(inputType))
    }

    @Test
    fun nullTypeIsNotPasswordField() {
        assertFalse(PasswordFieldDetector.isPasswordField(InputType.TYPE_NULL))
    }

    @Test
    fun nullEditorInfoIsNotPasswordField() {
        assertFalse(PasswordFieldDetector.isPasswordField(null as EditorInfo?))
    }

    @Test
    fun editorInfoDelegatesToInputType() {
        val password = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertTrue(PasswordFieldDetector.isPasswordField(info(password)))
        assertFalse(PasswordFieldDetector.isPasswordField(info(InputType.TYPE_CLASS_TEXT)))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.keyboard.PasswordFieldDetectorTest"`
Expected: FAIL — compilation error, `PasswordFieldDetector` unresolved.

- [ ] **Step 3: Implement `PasswordFieldDetector.kt`**

Match `InputTypeLayouts.kt` shape (top-level `object`, class-mask switch):

```kotlin
package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

object PasswordFieldDetector {

    fun isPasswordField(info: EditorInfo?): Boolean =
        info != null && isPasswordField(info.inputType)

    fun isPasswordField(inputType: Int): Boolean =
        when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> inputType and InputType.TYPE_MASK_VARIATION in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            )
            InputType.TYPE_CLASS_NUMBER ->
                inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.keyboard.PasswordFieldDetectorTest"`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/org/ubaierbhat/android/barcodekeyboard/keyboard/PasswordFieldDetector.kt \
  app/src/test/java/org/ubaierbhat/android/barcodekeyboard/keyboard/PasswordFieldDetectorTest.kt
git commit -m "feat: add password-field detection for scanned input targets"
```

---

### Task 2: Skip history recording in the service

**Files:**
- Modify: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt:266` (the `recordScan(text)` call inside `handleBarcodeResult`)
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardServiceScanHistoryTest.kt` (new)

**Interfaces:**
- Consumes: `PasswordFieldDetector.isPasswordField(info: EditorInfo?): Boolean` from Task 1 (import from `keyboard` package).
- Produces: behavior change only — scans committed into password fields are absent from history; no API change.

- [ ] **Step 1: Write the failing service test (Robolectric)**

New file `BarcodeKeyboardServiceScanHistoryTest.kt`, modeled on `BarcodeKeyboardServiceRotationTest.kt`. Reflection reaches the private `handleBarcodeResult` and sets `InputMethodService.mInputEditorInfo` directly (the field behind `getCurrentInputEditorInfo`). History is asserted through the `scan_history` prefs file, exactly what `ScanHistoryStore` writes. `openScanMode` wraps `onScanRequested` quietly, matching `requestQuietly` in the rotation test.

```kotlin
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

    private fun startInputWith(inputType: Int) {
        val info = EditorInfo().apply { this.inputType = inputType }
        val field = InputMethodService::class.java.getDeclaredField("mInputFieldInfo")
        field.isAccessible = true
        field.set(service, info)
    }

    private fun injectScan(text: String) {
        val method = BarcodeKeyboardService::class.java
            .getDeclaredMethod("handleBarcodeResult", String::class.java)
        method.isAccessible = true
        method.invoke(service, text)
    }

    @Test
    fun scanIntoTextFieldIsRecorded() {
        startInputWith(InputType.TYPE_CLASS_TEXT)
        injectScan("normal123")
        assertEquals("normal123", history().getString(0))
    }

    @Test
    fun scanIntoPasswordFieldIsNotRecorded() {
        startInputWith(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        injectScan("secretPass1")
        assertEquals(0, history().length())
    }

    @Test
    fun scanRecordFollowsFieldAtScanTime() {
        startInputWith(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        injectScan("secretPass2")
        startInputWith(InputType.TYPE_CLASS_TEXT)
        injectScan("normal456")
        assertEquals(1, history().length())
        assertEquals("normal456", history().getString(0))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.service.BarcodeKeyboardServiceScanHistoryTest"`
Expected: FAIL — `scanIntoPasswordFieldIsNotRecorded` asserts 0 entries but the current unguarded code records `secretPass1` (expected: 0, actual: 1). The other two scenarios pass already.

- [ ] **Step 3: Guard the history record in `handleBarcodeResult`**

In `BarcodeKeyboardService.kt`, replace line 266 (`recordScan(text)`, the only call inside `handleBarcodeResult`) with:

```kotlin
if (!PasswordFieldDetector.isPasswordField(currentInputEditorInfo)) {
    recordScan(text)
}
```

Add the import `org.ubaierbhat.android.barcodekeyboard.keyboard.PasswordFieldDetector`. Do not touch `emitScanEvents`, `recordScan` itself, or `captureClipboardEntry`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.service.BarcodeKeyboardServiceScanHistoryTest" --tests "org.ubaierbhat.android.barcodekeyboard.keyboard.PasswordFieldDetectorTest"`
Expected: PASS (3 + 9 tests).

- [ ] **Step 5: Run the whole suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — no regressions (existing service/keyboard/scanner/history tests all green).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/org/ubaierbhat.android/barcodekeyboard/service/BarcodeKeyboardService.kt \
  app/src/test/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardServiceScanHistoryTest.kt
git commit -m "feat: skip scan history recording for password fields"
```
