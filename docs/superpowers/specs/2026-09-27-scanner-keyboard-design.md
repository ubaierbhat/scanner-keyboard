# Design Spec — Scanner Keyboard

Date: 2026-09-27
Status: Approved (user-approved design + clarifications)

## 1. Objective

An open-source Android custom keyboard (InputMethodService) combining a standard QWERTY
text-input keyboard with an integrated, real-time barcode/QR code scanner. Tapping the
SCAN key swaps the keyboard for a camera viewfinder; on detection, the decoded string is
injected at the cursor of the active field via InputConnection, and the scanner closes
back to the keyboard automatically.

Scanning is fully offline and on-device: ML Kit barcode-scanning, bundled model, no
network access at any point.

## 2. Decisions (user-approved)

| Decision | Choice |
|---|---|
| Scanner engine | ML Kit barcode-scanning, bundled only (no ZXing) |
| Barcode formats | FORMAT_ALL_FORMATS (QR, EAN-8/13, UPC-A, Code 39/93/128, ITF, Codabar, PDF417, Aztec, Data Matrix) |
| Detection behavior | Auto-insert + auto-close (haptic buzz, commitText, return to keyboard) |
| UI framework | XML View system everywhere; zero Compose |
| Keyboard scope | Standard QWERTY + long-press accents + clipboard/scan history (no themes customization, no split/one-hand) |
| Package | org.ubaierbhat.android.barcodekeyboard |
| SDKs | minSdk 24, compileSdk 36, targetSdk 35, Kotlin, JDK 17 (AGP 9 from android CLI template) |

## 3. Stack

- Kotlin, XML Views (AppCompat/Material for the setup activity only)
- CameraX: camera-core, camera-camera2, camera-lifecycle, camera-view
- ML Kit: com.google.mlkit:barcode-scanning (bundled)
- InputMethodService with three modes: KEYBOARD, SCANNER, HISTORY
- Tooling: `android` CLI (scaffold, install, emulator, screenshots), `./gradlew`

## 4. Components (one responsibility each)

```
app/src/main/kotlin (or java)/org/ubaierbhat/android/barcodekeyboard/
 ├─ MainActivity.kt                 setup wizard (grant camera, enable IME, live status)
 ├─ service/BarcodeKeyboardService.kt  InputMethodService; mode switching, text injection
 ├─ keyboard/KeyboardView.kt         QWERTY container; rows, key events, layer toggle
 ├─ keyboard/KeyView.kt              single key; press states, long-press, accent trigger
 ├─ keyboard/KeyboardState.kt        pure shift-state machine (OFF/ON/LOCKED)  [tested]
 ├─ keyboard/AccentMap.kt           pure letter → accent-variants data
 ├─ scanner/ScannerView.kt           PreviewView + close/torch controls; camera bind
 ├─ scanner/ScannerLifecycleOwner.kt LifecycleRegistry wrapper for CameraX in an IME
 ├─ scanner/BarcodeAnalyzer.kt       ImageAnalysis.Analyzer → ML Kit → callback
 ├─ scanner/ScanThrottle.kt          pure detection cooldown guard  [tested]
 ├─ history/ScanHistoryStore.kt      recent scanned/copied texts, SharedPreferences  [tested]
 └─ history/HistoryPanelView.kt      list panel; tap to re-insert
```

No deprecated `android.inputmethodservice.KeyboardView`/`Keyboard` classes — fully custom
views.

## 5. Keyboard (mode 1)

- Rows: QWERTYUIOP / ASDFGHJKL / ZXCVBNM + backspace at row end / bottom row:
  SHIFT, ?123 toggle, SCAN, HISTORY (added with Task 7), SPACE, ENTER
- Shift: tap → ON for next key then auto-off; long-press SHIFT → caps LOCK until re-tap
- Backspace: deleteSurroundingText; long-press auto-repeat (~400 ms delay, ~50 ms interval)
- ENTER: respects EditorInfo.imeOptions (performEditorAction) else newline in multiline
  fields else ENTER key event
- ?123 toggles a numbers/symbols layer; ABC returns to letters
- Long-press a letter → accent popup (PopupWindow above the key, drag-to-select,
  release-on-key inserts base letter)
- Haptics: View.performHapticFeedback only — no Vibrator, no VIBRATE permission

## 6. Scanner (mode 2)

- Layout taller than the keyboard (~45% of screen height; IME window auto-grows to the
  visible input view height): PreviewView, CLOSE button, TORCH toggle, hint text
- ScannerView owns a ScannerLifecycleOwner (LifecycleRegistry) — CameraX binds to it,
  never to the service (an InputMethodService is not a LifecycleOwner)
- ImageAnalysis with STRATEGY_KEEP_ONLY_LATEST; single background executor; the analyzer
  wraps ML Kit InputImage.fromMediaImage with rotationDegrees and FORMAT_ALL_FORMATS
- Detection flow: first non-null rawValue → post to main thread → commitText via
  null-checked InputConnection → haptic → save to history → auto-close scanner →
  mode resets to KEYBOARD
- ScanThrottle: 1000 ms cooldown guard so a reopen cannot re-fire the same code
- SCAN key: if camera permission denied → inline permission state with a button that
  launches MainActivity with FLAG_ACTIVITY_NEW_TASK
- Camera busy/init failure → inline error + auto-return to keyboard
- Camera unbinds in onFinishInputView/onWindowHidden/onDestroy; mode resets to
  KEYBOARD whenever the IME re-shows
- Torch toggle resets off on stop

## 7. History (mode 3)

- ScanHistoryStore: SharedPreferences JSON array; newest-first; dedup (re-adding moves
  to front); cap 20 entries; scan detections and clipboard captures both feed it
- Clipboard capture on IME show (default-IME clipboard exemption, Android 10+);
  non-blank, ≤500 chars, deduped; SecurityException during read is caught and ignored
- History panel: list of entries, tap inserts at cursor and closes panel; CLEAR and
  close buttons; empty state text

## 8. Setup wizard (MainActivity)

Two step cards with live status (refreshed in onResume/onRequestPermissionsResult):
1. Grant CAMERA permission (requestPermissions)
2. Enable the keyboard (ACTION_INPUT_METHOD_SETTINGS; enabled-check via
   InputMethodManager.getEnabledInputMethodList())
Overall "ready" line when both pass. All copy in strings.xml.

## 9. Permissions & manifest

- uses-permission CAMERA; uses-feature android.hardware.camera not required
- Service with BIND_INPUT_METHOD permission, intent-filter android.view.InputMethod,
  meta-data android.view.im → @xml/method (android:settingsActivity → MainActivity)
- No INTERNET permission; no VIBRATE permission

## 10. Edge cases

| Area | Behavior |
|---|---|
| Permission revoked at runtime | SCAN key → inline permission state + open-setup button |
| IME hidden while camera active | unbind camera; release hardware |
| Rapid/multiple detections | KEEP_ONLY_LATEST backpressure + 1000 ms throttle + auto-close |
| Focus loss / no InputConnection | null-check before every commitText |
| Rotation | input view rebuilt by system; mode resets to KEYBOARD (deliberate: no surprise camera) |
| Clipboard read blocked (non-default IME) | catch + ignore |

## 11. Testing

- Robolectric/JVM unit tests: KeyboardState (shift transitions), enter-action resolution,
  ScanThrottle, ScanHistoryStore (dedup/cap/persist)
- Task-scoped verification: assembleDebug green per task; on-emulator checks where
  meaningful (IME enable/set, keyboard render, setup wizard flow, scanner preview)
- Final acceptance: QR + EAN-13 scans insert into Chrome URL bar/Notes; airplane-mode
  (offline) scan works; torch; rotate; revoke mid-use; IME switch away/back releases
  camera (privacy indicator off)

## 12. Out of scope (v1 roadmap)

Theme customization, split/one-hand mode, glide typing/autocorrect, swipe gestures,
per-app keyboard profiles, multi-IME subtypes.
