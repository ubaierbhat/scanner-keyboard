# Scanner Options Strip — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the Form-mode toggle from the setup screen into the scanner preview as a second labeled switch in an extensible top options strip.

**Architecture:** One pill container in `scanner_view.xml` becomes two label+switch cells with a divider (`scanner_continuous_switch` + new `scanner_form_switch`); `ScannerView` binds both through a shared `bindSwitches()` helper (also fixing the deferred duplicated-wiring minor); the setup-screen Scanning card and its MainActivity binding are deleted. Prefs keys and the service read-per-scan behavior are untouched, so the setting is honored immediately with no new plumbing.

**Tech Stack:** Kotlin, Material Components 1.14 (`MaterialSwitch`), Robolectric/JUnit4.

**Spec:** `docs/superpowers/specs/2026-10-10-scanner-options-strip-design.md`

## Global Constraints

- versionCode stays `3`, versionName `"1.0.2"` — do not touch `app/build.gradle.kts:19-20`; no tag.
- No new dependencies, no new permissions, minSdk 24.
- Prefs keys `continuous_scan` / `translate_scan_actions` in file `scanner_settings` are FROZEN (existing installs must keep state).
- Do not touch: service scan handling, `ScanTranslator`, `DuplicateSuppressor`, keyboard geometry, `attachNavigationBarInsets`.
- `scanner_continuous_switch` id must not change (existing Robolectric test + binding depend on it).
- Work on `main`; commit per task; push only in Task 3 after device QA.
- Test command: `./gradlew testDebugUnitTest` (currently 88 green); focused form: `./gradlew testDebugUnitTest --tests "<FQCN>"`.

---

### Task 1: Relocate the control — strip layout + setup card removal

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (add 1, remove 3)
- Modify: `app/src/main/res/values/colors.xml` (add 1)
- Modify: `app/src/main/res/layout/scanner_view.xml` (pill becomes 2-cell strip)
- Modify: `app/src/main/res/layout/activity_setup.xml:117-164` (delete scan_card block)
- Modify: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/MainActivity.kt` (delete binding)

**Interfaces:**
- Consumes: existing `scanner_pill_background`, `@dimen/scanner_pill_margin_top`, `@dimen/scanner_pill_padding`, `@color/key_text`.
- Produces: new id `scanner_form_switch` (MaterialSwitch) in the strip; new string `scanner_form_label`; new color `scanner_divider`. Task 2 binds them.

- [ ] **Step 1: Strings + color**

In `strings.xml`: add next to `scanner_continuous_label` (line ~34):

```xml
    <string name="scanner_form_label">Form</string>
```

and DELETE these three lines (~19-21):

```xml
    <string name="setup_step_scan_title">Scanning</string>
    <string name="setup_step_scan_description">Translate Enter and Tab keys in scanned data (form filling).</string>
    <string name="setup_step_scan_switch_label">Scan actions</string>
```

In `colors.xml` (inside `<resources>`):

```xml
    <color name="scanner_divider">#33FFFFFF</color>
```

- [ ] **Step 2: Rewrite the pill into a two-cell strip**

In `scanner_view.xml`, replace the entire `LinearLayout` whose id is `scanner_continuous_pill` (lines 28-52: opening tag through its closing `</LinearLayout>`, i.e. the block sitting between the `scanner_hint` TextView and the bottom toolbar LinearLayout) with:

```xml
    <LinearLayout
        android:id="@+id/scanner_options_strip"
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
            android:layout_marginEnd="6dp"
            android:text="@string/scanner_continuous_label"
            android:textColor="@color/key_text"
            android:textSize="14sp" />

        <com.google.android.material.materialswitch.MaterialSwitch
            android:id="@+id/scanner_continuous_switch"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:minWidth="0dp"
            android:contentDescription="@string/scanner_continuous_label" />

        <View
            android:layout_width="1dp"
            android:layout_height="28dp"
            android:layout_marginStart="10dp"
            android:layout_marginEnd="10dp"
            android:background="@color/scanner_divider" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginEnd="6dp"
            android:text="@string/scanner_form_label"
            android:textColor="@color/key_text"
            android:textSize="14sp" />

        <com.google.android.material.materialswitch.MaterialSwitch
            android:id="@+id/scanner_form_switch"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:minWidth="0dp"
            android:contentDescription="@string/scanner_form_label" />
    </LinearLayout>
```

(`android:minWidth="0dp"` keeps the strip compact — MaterialSwitch defaults to a 68dp minWidth which leaves dead space next to a label.)

- [ ] **Step 3: Delete the setup Scanning card**

In `activity_setup.xml`, delete the whole block from the line `        <com.google.android.material.card.MaterialCardView` carrying `android:id="@+id/scan_card"` (line ~117) through its matching closing tag `        </com.google.android.material.card.MaterialCardView>` (line ~164) INCLUDING both. The next sibling (`open_test_fields_button` MaterialButton) must remain, and the file must still have balanced tags — verify with:

```bash
python3 -c "import xml.dom.minidom; xml.dom.minidom.parse('app/src/main/res/layout/activity_setup.xml'); print('setup xml OK')"
```

- [ ] **Step 4: Delete MainActivity binding**

In `MainActivity.kt` remove exactly:
- imports: `import com.google.android.material.materialswitch.MaterialSwitch` and `import org.ubaierbhat.android.barcodekeyboard.scanner.ScanSettings`
- fields: `private lateinit var scanActionsSwitch: MaterialSwitch` and `private lateinit var scanSettings: ScanSettings`
- the binding block inside `onCreate` (the five lines starting `scanSettings = ScanSettings(this)` through the matching `}` of `setOnCheckedChangeListener`) plus the blank line it leaves behind.

- [ ] **Step 5: Verify**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, 88/88 green (the existing `ScannerViewContinuousTest` must still find `scanner_continuous_switch` — unchanged id proves it).
Also: `grep -rn "scan_card\|scan_actions_switch\|setup_step_scan" app/src/main/` → no matches.

- [ ] **Step 6: Commit**

```bash
git add app/src/main
git commit -m "refactor: Form toggle moves from setup screen into scanner options strip layout"
```

---

### Task 2: ScannerView form binding + tests (TDD)

**Files:**
- Create then rename-test: `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerViewSwitchesTest.kt` (delete old `ScannerViewContinuousTest.kt` in the same commit)
- Modify: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerView.kt`

**Interfaces:**
- Consumes: `scanner_form_switch` (Task 1), `ScanSettings.translateScanActions` (unchanged).
- Produces: `ScannerView` private `fun bindSwitches()`; no public API changes.

- [ ] **Step 1: Write the failing test file**

Create `app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerViewSwitchesTest.kt`:

```kotlin
package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import android.view.ContextThemeWrapper
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
class ScannerViewSwitchesTest {

    private fun themedContext(): Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext<Context>(),
        R.style.Theme_ScannerKeyboard,
    )

    @Test
    fun continuousSwitchReflectsPersistedStateAndPersistsToggling() {
        val context = themedContext()
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

        settings.continuousScan = false
    }

    @Test
    fun formSwitchReflectsPersistedStateAndPersistsToggling() {
        val context = themedContext()
        val settings = ScanSettings(context)
        settings.translateScanActions = false

        val view = ScannerView(context)
        view.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = {},
        )

        val switch = view.findViewById<MaterialSwitch>(R.id.scanner_form_switch)
        assertFalse(switch.isChecked)

        switch.isChecked = true
        assertTrue(settings.translateScanActions)

        settings.translateScanActions = false
    }

    @Test
    fun freshlyConstructedViewRestoresBothPersistedSwitches() {
        val context = themedContext()
        val settings = ScanSettings(context)
        settings.continuousScan = true
        settings.translateScanActions = true

        val view = ScannerView(context)
        view.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = {},
        )

        assertTrue(view.findViewById<MaterialSwitch>(R.id.scanner_continuous_switch).isChecked)
        assertTrue(view.findViewById<MaterialSwitch>(R.id.scanner_form_switch).isChecked)

        settings.continuousScan = false
        settings.translateScanActions = false
    }
}
```

Then delete the old file: `git rm app/src/test/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerViewContinuousTest.kt`

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew testDebugUnitTest --tests "org.ubaierbhat.android.barcodekeyboard.scanner.ScannerViewSwitchesTest"`
Expected: compile failure — `Unresolved reference 'scanner_form_switch'`... it EXISTS as an id from Task 1, so the actual red is: `formSwitchReflectsPersistedStateAndPersistsToggling` FAILS with `assertFalse` (switch defaults unchecked while settings may be true? no) — precisely: view init does not yet bind `scanner_form_switch`, so after `switch.isChecked = true`, `settings.translateScanActions` is still false → `assertTrue(settings.translateScanActions)` FAILS. And `freshlyConstructedViewRestores...` fails because init never sets the form switch from prefs. Both are behavior-red, not compile-red (ids exist).

- [ ] **Step 3: Implement in ScannerView.kt**

Add field beside `continuousSwitch`:

```kotlin
    private val formSwitch: MaterialSwitch
```

Replace the last three statements of `init` (findViewById + isChecked + setOnCheckedChangeListener for continuous) with:

```kotlin
        continuousSwitch = findViewById(R.id.scanner_continuous_switch)
        formSwitch = findViewById(R.id.scanner_form_switch)
        bindSwitches()
```

Add the private method (place after `setCallbacks`):

```kotlin
    private fun bindSwitches() {
        continuousSwitch.setOnCheckedChangeListener(null)
        formSwitch.setOnCheckedChangeListener(null)
        continuousSwitch.isChecked = settings.continuousScan
        formSwitch.isChecked = settings.translateScanActions
        continuousSwitch.setOnCheckedChangeListener { _, checked ->
            settings.continuousScan = checked
            onContinuousChanged?.invoke(checked)
        }
        formSwitch.setOnCheckedChangeListener { _, checked ->
            settings.translateScanActions = checked
        }
    }
```

In `start(context)`, replace the three re-sync lines (`setOnCheckedChangeListener(null)` / `isChecked = settings.continuousScan` / `setOnCheckedChangeListener { ... }`) with a single call:

```kotlin
        bindSwitches()
```

- [ ] **Step 4: Verify green**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: 90 tests green (88 − 1 removed + 3 new). If `settings.translateScanActions = false` in prefs from an earlier test leaks across tests, Robolectric isolates per-method Application state — no shared state; do not add cleanup hacks beyond the resets already in the tests.

- [ ] **Step 5: Commit**

```bash
git add app/src
git commit -m "feat: bind Form switch to persisted setting in scanner options strip"
```

---

### Task 3: Docs wording, full verification, device QA, push

**Files:**
- Modify: `CHANGELOG.md:15` (Unreleased wording)
- Modify: `README.md:50` (Scan actions bullet wording)

**Interfaces:** none produced.

- [ ] **Step 1: Docs**

`CHANGELOG.md` — in the Unreleased Added bullet, replace
`Scan actions (opt-in, on the setup screen): newline and tab characters in` with
`Scan actions (opt-in via the scanner's Form switch): newline and tab characters in`
(keep the rest of the bullet intact).

`README.md` — replace the bullet opening
`- **Scan actions** — opt-in on the setup screen: newline and tab characters in` with
`- **Scan actions** — opt-in via the **Form** switch in the scanner: newline and tab characters in`
(rest unchanged).

`grep -rn "setup screen" README.md CHANGELOG.md docs/ | grep -i "scan action\|form"` → no stale matches (docs/superpowers/ specs/plans are historical records — do NOT rewrite them).

- [ ] **Step 2: Full gate**

Run: `./gradlew testDebugUnitTest assembleDebug` → 90 green, BUILD SUCCESSFUL.
`git diff` on `app/build.gradle.kts` must be empty.

- [ ] **Step 3: Device QA (Fire 6 `FIRE60000000005065`; also XCover5 `R58RB1N07TD` if reconnected)**

Install debug, verify `settings get secure default_input_method`, then:
1. Open scanner → strip shows `Continuous [sw] │ Form [sw]`, no clipping at 720px width, hint visible, one screenshot to `/tmp/strip_<serial>_1.png`.
2. Toggle Form ON → close scanner → reopen → Form still ON; prefs file shows `translate_scan_actions=true` (run-as cat).
3. Form ON + Continuous OFF: focus single-line field, scan `A\nB` payload → segment routing (A lands, focus action, B next field) — re-proves service reads the toggle live.
4. Form OFF (toggle in scanner, no app restart): same payload → literal text, no focus jump.
5. Continuous ON + Form ON simultaneously: rescan duplicate → suppressed; different payload with `\n` → routed. Regression on both.
6. Setup screen has NO Scanning card; camera/IME cards + field-tester button intact (screenshot).
7. XCover5 (if online): steps 1-2 minimum.

- [ ] **Step 4: Commit + push**

```bash
git add -A
git commit -m "docs: scanner Form switch wording for changelog and readme"
GIT_SSH_COMMAND="ssh -i ~/.ssh/id_github -o IdentitiesOnly=yes" git push origin main
```

No tag, no version bump, no release artifacts — wave joins the existing unreleased line until product sign-off.

---

## Self-review notes

- Spec coverage: strip layout (T1), binding + deferred-minor dedup (T2), setup removal (T1), docs (T3), QA matrix incl. persistence + live-read proof (T3), frozen prefs + id constraint (Global Constraints) — complete; icon-strip migration explicitly out.
- Type consistency: `scanner_form_switch`, `scanner_form_label`, `scanner_divider`, `bindSwitches()`, `ScannerViewSwitchesTest` names used identically across tasks.
- Task 2 Step 2 states the red is behavior-level (ids exist post-Task 1) — do not expect compile failure there.
