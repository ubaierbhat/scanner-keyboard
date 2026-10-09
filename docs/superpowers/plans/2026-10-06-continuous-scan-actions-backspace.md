# Continuous Scan, Scan Actions & Backspace Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add continuous scanning with consecutive-duplicate suppression, opt-in Enter/Tab translation of special characters in scanned data, and fix the backspace bug that deletes a character on both sides of the cursor.

**Architecture:** Three new pure/testable units (`DuplicateSuppressor`, `ScanTranslator`, `ScanSettings`) plus a `BackspaceDispatcher` planning object keep the service thin; `ScannerView` owns the continuous switch UI and persists it; `BarcodeKeyboardService.handleBarcodeResult` consults the setting, suppressor, and translator before committing; the setup screen gains one Material card for the Actions toggle. All follow existing patterns (`ScanThrottle`, `EnterDispatcher`, `ScanHistoryStore`).

**Tech Stack:** Kotlin, Android IME (`InputMethodService`), CameraX + ML Kit (unchanged), Material Components 1.14.0 (`MaterialSwitch`), JUnit4 + Robolectric (existing harness), SharedPreferences.

**Spec:** `docs/superpowers/specs/2026-10-06-continuous-scan-actions-backspace-design.md` (commit `296facb`).

## Global Constraints

- **DO NOT bump version.** `app/build.gradle.kts:19-20` must stay `versionCode = 3` / `versionName = "1.0.2"` in every task. Release cut happens only after the product owner's final device sign-off.
- minSdk 24: no API guarded above 24 without a version check (nothing here needs one).
- No new dependencies; no new permissions; all settings local (SharedPreferences) — the app stays 100% offline and collects nothing.
- Do not touch keyboard geometry (`keyboard_view.xml`, `ScaledKeyboardLayout`), nav-bar inset code (`attachNavigationBarInsets`), or the `#+=`/gear fixes.
- Work directly on `main`. Commit after each task with the exact message given. Push only in Task 8.
- Test command form: `./gradlew testDebugUnitTest --tests "<FQCN>"` ; full suite `./gradlew testDebugUnitTest`.
- All user-visible strings go in `app/src/main/res/values/strings.xml`.

---

### Task 1: Backspace fix (deleteSurroundingText 1,1 → 1,0)

**Files:**
- Create: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/keyboard/BackspaceDispatcher.kt`
- Modify: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt:104-122` (`onBackspace`)
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/keyboard/BackspaceDispatcherTest.kt`
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/service/BackspaceConnectionTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `object BackspaceDispatcher { sealed interface Action; data object DeleteSelection : Action; data object DeleteOneBeforeCursor : Action; fun plan(selectedText: CharSequence?): Action }` — Task 6 does not use this; service uses it directly.

- [ ] **Step 1: Write the failing dispatcher test**

Create `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/keyboard/BackspaceDispatcherTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run test, verify it fails to compile (class missing)**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.keyboard.BackspaceDispatcherTest"`
Expected: `compileDebugUnitTestKotlin FAILED` — unresolved reference `BackspaceDispatcher`.

- [ ] **Step 3: Implement the dispatcher**

Create `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/keyboard/BackspaceDispatcher.kt`:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.keyboard

object BackspaceDispatcher {

    sealed interface Action {
        data object DeleteSelection : Action
        data object DeleteOneBeforeCursor : Action
    }

    fun plan(selectedText: CharSequence?): Action =
        if (selectedText.isNullOrEmpty()) {
            Action.DeleteOneBeforeCursor
        } else {
            Action.DeleteSelection
        }
}
```

Run the dispatcher test again; Expected: PASS (2 tests).

- [ ] **Step 4: Write the failing connection-level regression test**

Create `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/service/BackspaceConnectionTest.kt` (Robolectric, real `EditableInputConnection`, proving one backspace deletes only the character before the cursor):

```kotlin
package org.ubaierbhat.android.barcodekeyboard.service

import android.content.Context
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.ubaierbhat.android.barcodekeyboard.keyboard.BackspaceDispatcher

@RunWith(RobolectricTestRunner::class)
class BackspaceConnectionTest {

    private fun dispatch(inputConnection: InputConnection) {
        val selectedText = inputConnection.getSelectedText(0)
        when (BackspaceDispatcher.plan(selectedText)) {
            BackspaceDispatcher.Action.DeleteSelection -> {
                val down = android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_DEL,
                )
                inputConnection.sendKeyEvent(down)
                inputConnection.sendKeyEvent(
                    android.view.KeyEvent(
                        down.downTime,
                        android.os.SystemClock.uptimeMillis(),
                        android.view.KeyEvent.ACTION_UP,
                        android.view.KeyEvent.KEYCODE_DEL,
                        0,
                    ),
                )
            }
            BackspaceDispatcher.Action.DeleteOneBeforeCursor ->
                inputConnection.deleteSurroundingText(1, 0)
        }
    }

    @Test
    fun backspaceWithCursorInMiddleDeletesOnlyLeftCharacter() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val editText = EditText(context)
        editText.setText("abcdef")
        editText.setSelection(3)
        val inputConnection = android.view.inputmethod.EditableInputConnection(editText)

        dispatch(inputConnection)

        assertEquals("abdef", editText.text.toString())
    }
}
```

Note for the implementer: `EditableInputConnection` is referenced by its full name in the test body so no extra import is needed. If `getSelectedText(0)` returns an empty (not null) string when nothing is selected on this Robolectric version, the plan still routes correctly (`plan("")` → `DeleteOneBeforeCursor`).

- [ ] **Step 5: Run test, verify it fails against current behavior**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.service.BackspaceConnectionTest"`
Expected: FAIL with `expected:<abdef> but was:<abef>` — this is the exact bug (both-sides delete) reproduced in a test.

- [ ] **Step 6: Apply the fix in the service**

In `BarcodeKeyboardService.kt`, replace the body of `onBackspace()` (lines 104-122) with:

```kotlin
    override fun onBackspace() {
        val inputConnection = currentInputConnection ?: return
        val selectedText = inputConnection.getSelectedText(0)
        when (BackspaceDispatcher.plan(selectedText)) {
            BackspaceDispatcher.Action.DeleteSelection -> {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                inputConnection.sendKeyEvent(down)
                inputConnection.sendKeyEvent(
                    KeyEvent(
                        down.downTime,
                        SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_DEL,
                        0,
                    ),
                )
            }
            BackspaceDispatcher.Action.DeleteOneBeforeCursor ->
                inputConnection.deleteSurroundingText(1, 0)
        }
    }
```

Add import `org.ubaierbhat.android.barcodekeyboard.keyboard.BackspaceDispatcher` to the service.

- [ ] **Step 7: Run both new tests + full suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL; previous 66 tests + 3 new = 69 passing.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/org/ubaierbhat/android/barcodekeyboard/keyboard/BackspaceDispatcher.kt app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt app/src/test/java/org/ubaierbhat/android/barcodekeyboard/keyboard/BackspaceDispatcherTest.kt app/src/test/java/org/ubaierbhat/android/barcodekeyboard/service/BackspaceConnectionTest.kt
git commit -m "fix: backspace with cursor in middle now deletes only the character before it"
```

---

### Task 2: DuplicateSuppressor (consecutive-duplicate blocking)

**Files:**
- Create: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/DuplicateSuppressor.kt`
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/DuplicateSuppressorTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `class DuplicateSuppressor { fun shouldEmit(value: String): Boolean; fun reset() }` — used by Task 6 in the service.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/DuplicateSuppressorTest.kt`:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateSuppressorTest {

    @Test
    fun firstValueIsEmitted() {
        val suppressor = DuplicateSuppressor()
        assertTrue(suppressor.shouldEmit("A"))
    }

    @Test
    fun consecutiveSameValueIsSuppressed() {
        val suppressor = DuplicateSuppressor()
        suppressor.shouldEmit("A")
        assertFalse(suppressor.shouldEmit("A"))
        assertFalse(suppressor.shouldEmit("A"))
    }

    @Test
    fun differentValueRearmsTheSameValue() {
        val suppressor = DuplicateSuppressor()
        assertTrue(suppressor.shouldEmit("A"))
        assertTrue(suppressor.shouldEmit("B"))
        assertTrue(suppressor.shouldEmit("A"))
    }

    @Test
    fun resetForgetsTheLastValue() {
        val suppressor = DuplicateSuppressor()
        suppressor.shouldEmit("A")
        suppressor.reset()
        assertTrue(suppressor.shouldEmit("A"))
    }
}
```

- [ ] **Step 2: Run test, verify compile failure**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.DuplicateSuppressorTest"`
Expected: unresolved reference `DuplicateSuppressor`.

- [ ] **Step 3: Implement**

Create `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/DuplicateSuppressor.kt`:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.scanner

class DuplicateSuppressor {

    private var lastValue: String? = null

    fun shouldEmit(value: String): Boolean {
        if (value == lastValue) {
            return false
        }
        lastValue = value
        return true
    }

    fun reset() {
        lastValue = null
    }
}
```

- [ ] **Step 4: Run test, verify PASS**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.DuplicateSuppressorTest"`
Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/DuplicateSuppressor.kt app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/DuplicateSuppressorTest.kt
git commit -m "feat: DuplicateSuppressor blocks consecutive identical scan results"
```

---

### Task 3: ScanTranslator (special characters → typed events)

**Files:**
- Create: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanEvent.kt`
- Create: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanTranslator.kt`
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanTranslatorTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `sealed interface ScanEvent { data class Type(val text: String) : ScanEvent; data object Enter : ScanEvent; data object Tab : ScanEvent }` and `object ScanTranslator { fun translate(raw: String): List<ScanEvent> }` — Task 6 applies these events.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanTranslatorTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run test, verify compile failure**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.ScanTranslatorTest"`
Expected: unresolved references `ScanEvent` / `ScanTranslator`.

- [ ] **Step 3: Implement**

Create `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanEvent.kt`:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.scanner

sealed interface ScanEvent {
    data class Type(val text: String) : ScanEvent
    data object Enter : ScanEvent
    data object Tab : ScanEvent
}
```

Create `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanTranslator.kt`:

```kotlin
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
```

Note: `last == event` compares a `data object` by identity-safe equality; `ScanEvent.Type` never equals an `Enter`/`Tab`, so segments are untouched by collapsing.

- [ ] **Step 4: Run test, verify PASS**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.ScanTranslatorTest"`
Expected: 11 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanEvent.kt app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanTranslator.kt app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanTranslatorTest.kt
git commit -m "feat: ScanTranslator maps newline/tab in scanned data to Enter/Tab events"
```

---

### Task 4: ScanSettings (persisted feature toggles)

**Files:**
- Create: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanSettings.kt`
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanSettingsTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `class ScanSettings(context: Context) { var continuousScan: Boolean; var translateScanActions: Boolean }` backed by prefs file `scanner_settings`. Used by Tasks 5, 6, 7.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanSettingsTest.kt`:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScanSettingsTest {

    @Test
    fun defaultsAreOff() {
        val settings = ScanSettings(ApplicationProvider.getApplicationContext())
        assertFalse(settings.continuousScan)
        assertFalse(settings.translateScanActions)
    }

    @Test
    fun valuesPersistAcrossInstances() {
        val first = ScanSettings(ApplicationProvider.getApplicationContext())
        first.continuousScan = true
        first.translateScanActions = true

        val second = ScanSettings(ApplicationProvider.getApplicationContext())
        assertTrue(second.continuousScan)
        assertTrue(second.translateScanActions)

        second.continuousScan = false
        assertEquals(false, first.continuousScan)
    }
}
```

- [ ] **Step 2: Run test, verify compile failure**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.ScanSettingsTest"`
Expected: unresolved reference `ScanSettings`.

- [ ] **Step 3: Implement**

Create `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanSettings.kt`:

```kotlin
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
```

- [ ] **Step 4: Run test, verify PASS**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.ScanSettingsTest"`
Expected: 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanSettings.kt app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScanSettingsTest.kt
git commit -m "feat: ScanSettings persists continuous-scan and scan-action toggles"
```

---

### Task 5: Continuous switch in the scanner preview

**Files:**
- Create: `app/src/main/res/drawable/scanner_pill_background.xml`
- Modify: `app/src/main/res/values/colors.xml` (add one color)
- Modify: `app/src/main/res/values/dimens.xml` (add two dimens)
- Modify: `app/src/main/res/values/strings.xml` (add one string)
- Modify: `app/src/main/res/layout/scanner_view.xml` (add pill)
- Modify: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerView.kt` (bind switch)
- Test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerViewContinuousTest.kt`

**Interfaces:**
- Consumes: `ScanSettings` (Task 4).
- Produces: `ScannerView.setCallbacks(onClose: () -> Unit, onError: (String) -> Unit, onBarcodeResult: (String) -> Unit, onContinuousChanged: (Boolean) -> Unit)` (signature CHANGES — Task 6 updates the caller) and `val ScannerView.isContinuousEnabled: Boolean` (getter reading `ScanSettings`).

- [ ] **Step 1: Add resources**

`colors.xml` — inside `<resources>` add:

```xml
<color name="scanner_pill_scrim">#B3000000</color>
```

`dimens.xml` — add:

```xml
<dimen name="scanner_pill_margin_top">12dp</dimen>
<dimen name="scanner_pill_padding">14dp</dimen>
```

`strings.xml` — add:

```xml
<string name="scanner_continuous_label">Continuous</string>
```

Create `app/src/main/res/drawable/scanner_pill_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/scanner_pill_scrim" />
    <corners android:radius="24dp" />
</shape>
```

- [ ] **Step 2: Add the pill to scanner_view.xml**

Inside the `<merge>` root, AFTER the `scanner_hint` TextView and BEFORE the bottom `LinearLayout`, insert:

```xml
    <LinearLayout
        android:id="@+id/scanner_continuous_pill"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="top|center_horizontal"
        android:layout_marginTop="@dimen/scanner_pill_margin_top"
        android:background="@drawable/scanner_pill_background"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingStart="@dimen/scanner_pill_padding"
        android:paddingEnd="@dimen/scanner_pill_padding">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/scanner_continuous_label"
            android:textColor="@color/key_text"
            android:textSize="14sp" />

        <com.google.android.material.materialswitch.MaterialSwitch
            android:id="@+id/scanner_continuous_switch"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:contentDescription="@string/scanner_continuous_label" />
    </LinearLayout>
```

Note: `@color/key_text` is the token the `ScannerText` style already uses for scanner text — verified present. The scanner view inflates under the service context whose theme is the app's Material3 theme; if `MaterialSwitch` ever throws on a missing theme attribute during device QA, wrap the inflation context with `ContextThemeWrapper(context, R.style.Theme_ScannerKeyboard)` in `ScannerView` — do not do this preemptively.

- [ ] **Step 3: Write the failing view test**

Create `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerViewContinuousTest.kt`:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.ubaierbhat.android.barcodekeyboard.R

@RunWith(RobolectricTestRunner::class)
class ScannerViewContinuousTest {

    @Test
    fun switchReflectsPersistedStateAndPersistsToggling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = ScanSettings(context)
        settings.continuousScan = false

        var reported: Boolean? = null
        val view = ScannerView(context)
        view.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = { reported = it },
        )

        val switch = view.findViewById<MaterialSwitch>(R.id.scanner_continuous_switch)
        assertFalse(switch.isChecked)

        switch.isChecked = true
        assertTrue(settings.continuousScan)
        assertTrue(view.isContinuousEnabled)
        assertEquals(true, reported)

        val second = ScannerView(context)
        second.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = {},
        )
        val secondSwitch = second.findViewById<MaterialSwitch>(R.id.scanner_continuous_switch)
        assertTrue("new scanner view restores persisted state on start-equivalent sync",
            second.isContinuousEnabled)
        settings.continuousScan = false
    }
}
```

- [ ] **Step 4: Run test, verify it fails**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.ScannerViewContinuousTest"`
Expected: compile failure (4-arg `setCallbacks`, `isContinuousEnabled` missing). If it compiles against old 3-arg calls it fails on arity — both count as red.

- [ ] **Step 5: Implement ScannerView changes**

In `ScannerView.kt`:

Add imports:

```kotlin
import com.google.android.material.materialswitch.MaterialSwitch
import org.ubaierbhat.android.barcodekeyboard.R // already present
```

Add fields (next to the existing key fields):

```kotlin
    private val continuousSwitch: MaterialSwitch
    private val settings = ScanSettings(context)
    private var onContinuousChanged: ((Boolean) -> Unit)? = null
```

In `init`, after `torchKey.onPress = { toggleTorch() }`:

```kotlin
        continuousSwitch = findViewById(R.id.scanner_continuous_switch)
        continuousSwitch.isChecked = settings.continuousScan
        continuousSwitch.setOnCheckedChangeListener { _, checked ->
            settings.continuousScan = checked
            onContinuousChanged?.invoke(checked)
        }
```

Replace `setCallbacks` with:

```kotlin
    fun setCallbacks(
        onClose: () -> Unit,
        onError: (String) -> Unit,
        onBarcodeResult: (String) -> Unit,
        onContinuousChanged: (Boolean) -> Unit,
    ) {
        this.onClose = onClose
        this.onError = onError
        this.onBarcodeResult = onBarcodeResult
        this.onContinuousChanged = onContinuousChanged
    }

    val isContinuousEnabled: Boolean
        get() = settings.continuousScan
```

In `start(context: Context)`, first line inside the function (after the `if (running) return`): re-sync the switch UI silently:

```kotlin
        continuousSwitch.setOnCheckedChangeListener(null)
        continuousSwitch.isChecked = settings.continuousScan
        continuousSwitch.setOnCheckedChangeListener { _, checked ->
            settings.continuousScan = checked
            onContinuousChanged?.invoke(checked)
        }
```

- [ ] **Step 6: Fix the compile break in the service (temporary minimal change)**

`BarcodeKeyboardService.obtainScannerView()` (~line 224): add the new callback argument so the module compiles; Task 6 replaces this whole wiring:

```kotlin
            setCallbacks(
                onClose = { closeScanner() },
                onError = { scheduleScannerDismiss() },
                onBarcodeResult = { text -> handleBarcodeResult(text) },
                onContinuousChanged = { },
            )
```

- [ ] **Step 7: Run test + full suite, verify PASS**

Run: `./gradlew testDebugUnitTest`
Expected: all green (69 + 1 new = 70).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/res/drawable/scanner_pill_background.xml app/src/main/res/values/colors.xml app/src/main/res/values/dimens.xml app/src/main/res/values/strings.xml app/src/main/res/layout/scanner_view.xml app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerView.kt app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerViewContinuousTest.kt
git commit -m "feat: continuous-scan switch on scanner preview with persisted state"
```

---

### Task 6: Service integration — continuous mode + scan actions in handleBarcodeResult

**Files:**
- Modify: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt` (imports, fields, `onCreate`, `openScanner`, `closeScanner`, `obtainScannerView`, `handleBarcodeResult`, `onEnter`, new private helpers)

**Interfaces:**
- Consumes: `DuplicateSuppressor` (Task 2), `ScanTranslator`/`ScanEvent` (Task 3), `ScanSettings` (Task 4), `ScannerView.isContinuousEnabled` + 4-arg `setCallbacks` (Task 5), `EnterDispatcher.dispatch(behavior, performAction, sendNewline, sendEnterKey)` (existing).
- Produces: final `handleBarcodeResult` behavior used by Task 8 QA.

- [ ] **Step 1: Add fields and imports**

Imports to add to `BarcodeKeyboardService.kt`:

```kotlin
import org.ubaierbhat.android.barcodekeyboard.scanner.DuplicateSuppressor
import org.ubaierbhat.android.barcodekeyboard.scanner.ScanEvent
import org.ubaierbhat.android.barcodekeyboard.scanner.ScanSettings
import org.ubaierbhat.android.barcodekeyboard.scanner.ScanTranslator
import android.view.inputmethod.InputConnection
```

Fields (after `private var enterBehavior`):

```kotlin
    private val duplicateSuppressor = DuplicateSuppressor()
    private var scanSettings: ScanSettings? = null
```

In `onCreate()` after `historyStore = ScanHistoryStore(this)`:

```kotlin
        scanSettings = ScanSettings(this)
```

- [ ] **Step 2: Extract dispatchEnter and add sendTab**

Add private methods near `onEnter`:

```kotlin
    private fun dispatchEnter(inputConnection: InputConnection) {
        EnterDispatcher.dispatch(
            enterBehavior,
            { actionId -> inputConnection.performEditorAction(actionId) },
            { inputConnection.commitText("\n", 1) },
            {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                inputConnection.sendKeyEvent(down)
                inputConnection.sendKeyEvent(
                    KeyEvent(
                        down.downTime,
                        SystemClock.uptimeMillis(),
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_ENTER,
                        0,
                    ),
                )
            },
        )
    }

    private fun sendTab(inputConnection: InputConnection) {
        val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB)
        val up = KeyEvent(
            down.downTime,
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_TAB,
            0,
        )
        val handled = inputConnection.sendKeyEvent(down) || inputConnection.sendKeyEvent(up)
        if (!handled) {
            inputConnection.commitText("\t", 1)
        }
    }
```

Rewrite `onEnter()` (current lines 124-143) to delegate:

```kotlin
    override fun onEnter() {
        val inputConnection = currentInputConnection ?: return
        dispatchEnter(inputConnection)
    }
```

- [ ] **Step 3: Rewrite handleBarcodeResult**

Replace the existing method (current lines 232-240) with:

```kotlin
    private fun handleBarcodeResult(text: String) {
        if (mode != Mode.SCANNER) {
            return
        }
        val continuous = scannerView?.isContinuousEnabled == true
        if (continuous && !duplicateSuppressor.shouldEmit(text)) {
            return
        }
        val inputConnection = currentInputConnection
        if (inputConnection != null && scanSettings?.translateScanActions == true) {
            for (event in ScanTranslator.translate(text)) {
                when (event) {
                    is ScanEvent.Type -> inputConnection.commitText(event.text, 1)
                    ScanEvent.Enter -> dispatchEnter(inputConnection)
                    ScanEvent.Tab -> sendTab(inputConnection)
                }
            }
        } else {
            inputConnection?.commitText(text, 1)
        }
        inputContainer?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        recordScan(text)
        if (!continuous) {
            closeScanner()
        }
    }
```

- [ ] **Step 4: Reset the suppressor on open and close**

In `openScanner()`, immediately after `scanner.start(this)`:

```kotlin
        duplicateSuppressor.reset()
```

In `closeScanner()`, after `handler.removeCallbacks(dismissScannerRunnable)`:

```kotlin
        duplicateSuppressor.reset()
```

In `obtainScannerView()`, replace the temporary no-op from Task 5 with:

```kotlin
                onContinuousChanged = { duplicateSuppressor.reset() },
```

- [ ] **Step 5: Run the full suite**

Run: `./gradlew testDebugUnitTest`
Expected: all 70 green (no new tests in this task — the units it composes are already covered; device QA in Task 8 verifies wiring).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt
git commit -m "feat: continuous scanning with duplicate suppression and Enter/Tab scan actions"
```

---

### Task 7: Setup screen — Scanning card with Actions toggle

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (three strings)
- Modify: `app/src/main/res/layout/activity_setup.xml` (new card after `ime_card`)
- Modify: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/MainActivity.kt` (bind toggle)

**Interfaces:**
- Consumes: `ScanSettings` (Task 4).
- Produces: persisted `translate_scan_actions` toggled from the UI; no interfaces consumed later.

- [ ] **Step 1: Add strings**

`strings.xml`:

```xml
<string name="setup_step_scan_title">Scanning</string>
<string name="setup_step_scan_description">Translate Enter and Tab keys in scanned data (form filling).</string>
<string name="setup_step_scan_switch_label">Scan actions</string>
```

- [ ] **Step 2: Add the card**

In `activity_setup.xml`, after the closing `</com.google.android.material.card.MaterialCardView>` of `ime_card` and before the remaining views (test-fields button / overall status), insert a card that reuses the established card pattern (`?attr/materialCardViewFilledStyle`, `@dimen/setup_card_spacing`, `@dimen/setup_card_content_padding`, `?attr/textAppearanceTitleMedium`/`BodyMedium` — all verified against `camera_card`):

```xml
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/scan_card"
            style="?attr/materialCardViewFilledStyle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/setup_card_spacing">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="@dimen/setup_card_content_padding">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/setup_step_scan_title"
                    android:textAppearance="?attr/textAppearanceTitleMedium" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="@string/setup_step_scan_description"
                    android:textAppearance="?attr/textAppearanceBodyMedium" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:gravity="center_vertical"
                    android:orientation="horizontal">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="@string/setup_step_scan_switch_label"
                        android:textAppearance="?attr/textAppearanceBodyLarge" />

                    <com.google.android.material.materialswitch.MaterialSwitch
                        android:id="@+id/scan_actions_switch"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:contentDescription="@string/setup_step_scan_switch_label" />
                </LinearLayout>
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 3: Bind in MainActivity**

In `MainActivity.kt`: add imports `com.google.android.material.materialswitch.MaterialSwitch` and `org.ubaierbhat.android.barcodekeyboard.scanner.ScanSettings`. Add fields:

```kotlin
    private lateinit var scanActionsSwitch: MaterialSwitch
    private lateinit var scanSettings: ScanSettings
```

In `onCreate()`, after `overallStatus = findViewById(R.id.overall_status)`:

```kotlin
        scanSettings = ScanSettings(this)
        scanActionsSwitch = findViewById(R.id.scan_actions_switch)
        scanActionsSwitch.isChecked = scanSettings.translateScanActions
        scanActionsSwitch.setOnCheckedChangeListener { _, checked ->
            scanSettings.translateScanActions = checked
        }
```

- [ ] **Step 4: Verify compile + suite + debug build installs**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, 70 tests green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/layout/activity_setup.xml app/src/main/java/org/ubaierbhat/android/barcodekeyboard/MainActivity.kt
git commit -m "feat: setup screen toggle for Enter/Tab translation in scanned data"
```

---

### Task 8: Documentation, dual-device QA, push

**Files:**
- Modify: `CHANGELOG.md` (Unreleased section)
- Modify: `README.md` (Features: continuous scan + scan actions lines; fix note)
- Modify: `docs/architecture.html` (new classes if the doc enumerates components — check its current structure first)
- No version changes anywhere (Global Constraints).

- [ ] **Step 1: Update CHANGELOG Unreleased**

Under `## [Unreleased]` replace the "No unreleased changes yet." placeholder with:

```markdown
### Added

- Continuous scanning: switch in the scanner preview keeps the camera rolling
  and commits every new code; consecutive duplicate scans are ignored. State
  is remembered.
- Scan actions (opt-in, on the setup screen): newline and tab characters in
  scanned data are translated into Enter (field's editor action, or a real
  newline in multiline fields) and Tab focus moves, so multi-field forms can
  be filled with a single scan.

### Fixed

- Backspace with the cursor in the middle of text deleted a character on both
  sides of the cursor. It now deletes only the character before the cursor.
```

- [ ] **Step 2: Update README feature list**

In the Features section add two bullets (match the file's existing style): continuous scan with duplicate suppression, and opt-in Enter/Tab translation for form filling. Add the backspace fix only if README keeps a changelog-like list; otherwise skip.

- [ ] **Step 3: Full verification build**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass, no new permission/version diffs (`git diff app/build.gradle.kts` must be empty).

- [ ] **Step 4: Install debug build on BOTH connected devices**

```bash
adb -s R58RB1N07TD install -r app/build/outputs/apk/debug/app-debug.apk
adb -s FIRE60000000005065 install -r app/build/outputs/apk/debug/app-debug.apk
```

Re-enable the IME on each (reinstall can reset it): `adb -s <serial> shell ime enable org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService && adb -s <serial> shell ime set org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService`, then verify `settings get secure default_input_method` returns our service.

- [ ] **Step 5: Manual QA matrix on each device**

Use the debug field tester (`am start -n org.ubaierbhat.android.barcodekeyboard/.MainActivity --activity-clear-task`, open test fields). Required passes per device:

1. Backspace: cursor mid-word → one char deleted left of cursor only (XCover5 regression + Fire 6).
2. Continuous OFF (default): scan commits and closes (unchanged flow).
3. Continuous ON: two different QR codes both commit, view stays open; third scan of the second code → nothing happens; scan of first code again → commits (re-armed). Close/reopen scanner → suppression reset; switch state remembered.
4. Actions ON + single-line field: payload `A\nB` (QR) types A, performs field editor action/next-focus; `A\tB` tabs.
5. Actions ON + multiline field: `\n` inserts a real newline.
6. Actions OFF: same payloads insert literal characters.
7. v1.0.2 non-regression: gear key, `#+=` layer (backtick/caret), nav padding untouched.
Capture screenshots per numbered pass into `/tmp/qa8_<serial>_<n>.png`.

QR payloads can be generated with any generator; for automated display the agent may use `qrencode -t PNG` locally and show on a second screen, or use the tester's own history.

- [ ] **Step 6: Architecture doc + commit + push**

If `docs/architecture.html` enumerates classes, add `DuplicateSuppressor`, `ScanTranslator`, `ScanSettings`, `BackspaceDispatcher`. Then:

```bash
git add -A
git commit -m "docs: changelog and readme for continuous scan, scan actions, backspace fix"
GIT_SSH_COMMAND="ssh -i ~/.ssh/id_github -o IdentitiesOnly=yes" git push origin main
```

Do NOT create a tag, do NOT build/release AAB, do NOT bump versions — product owner runs final testing first.

---

## Self-review notes (plan author)

- Spec coverage: F1 UI/persistence/dedupe/teardown (Tasks 4,5,6, QA 8.5) ✓; F2 translator rules + EnterDispatcher reuse + Tab fallback + only-scan-commits scope + setup switch (Tasks 3,6,7) ✓; bug fix + regression test (Task 1) ✓; no version bump (Global Constraints + 8.3 guard) ✓.
- Type consistency: `shouldEmit(value: String): Boolean`, `translate(raw: String): List<ScanEvent>`, `continuousScan`/`translateScanActions`, `isContinuousEnabled`, 4-arg `setCallbacks`, `scanner_continuous_switch`, `scan_actions_switch` — used identically across tasks.
- Known risk carried from spec: Task 1's `EditableInputConnection.getSelectedText(0)` under Robolectric — the test routes through `plan("")` if it returns empty, behavior is identical; Task 8.5.1 is the authoritative selection-path check on device.
