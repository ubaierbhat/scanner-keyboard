# Continuous Scan, Scan Actions, Backspace Fix — Design

Date: 2026-10-06
Status: approved by product owner (session 2026-10-06)
Builds on: v1.0.2 (`c6f3725`). Version is NOT bumped until all testing completes.

## Problem

1. One-shot scanning forces a reopen of the scanner for every barcode — unusable
   for batch workflows (inventory, form filling).
2. Barcode payloads often embed control characters (`\n`, `\r`, `\t`). Today they
   are committed as literal text, so multi-field forms cannot be filled in one scan
   and trailing newlines cannot submit.
3. Backspace with the cursor in the middle of text deletes a character on BOTH
   sides of the cursor (`deleteSurroundingText(1, 1)` at
   `BarcodeKeyboardService.kt:121`).

## Decisions (from product owner Q&A)

| Question | Decision |
|---|---|
| Duplicate definition | Block only **consecutive** identical values; a different scan in between re-arms the same code |
| Continuous switch state | **Persisted** (remembered across scanner opens / IME restarts) |
| Enter translation | Follows the field's editor action (Next/Go/Done); multiline fields receive a real newline |
| Tab translation | Real `KEYCODE_TAB` key event (focus move); fallback literal `\t` |
| Trailing `\n` in payload | **Fires** the Enter event (enables scan-to-submit) |
| Continuous switch location | On the scanner preview itself (it is a scan-mode control) |
| Actions switch location | App setup screen (it is an app-level behavior) |
| Versioning | No versionCode/versionName change during development; cut only after final QA |

## Feature 1 — Continuous scan

### UI

- `scanner_view.xml`: a labeled Material switch (`Continuous`) inside a small
  rounded pill, overlay gravity top|center_horizontal, above the hint text.
  Touch target stays clear of the center scan area.
- Label string: `scanner_continuous` ("Continuous").

### Behavior

- State read when the scanner opens (`ScannerView.start`); default OFF.
- Turning it ON persists immediately to SharedPreferences.
- `BarcodeKeyboardService.handleBarcodeResult`:
  - OFF → unchanged one-shot flow (commit, haptic, history, close scanner).
  - ON → scanner stays open; per accepted scan: commit text (through Feature 2's
    translator if actions enabled), haptic feedback, history record. The viewfinder
    keeps running.
- All existing teardown paths stay: `onFinishInputView`, `onWindowHidden`,
  `onDestroy` still call `closeScanner()` (camera never stays live off-screen).

### Dedupe — `DuplicateSuppressor`

New pure class in `scanner/` package (mirrors `ScanThrottle`):

```
class DuplicateSuppressor {
    fun shouldEmit(value: String): Boolean  // false iff value == last emitted
    fun reset()
}
```

- State: only the last emitted raw string.
- Reset when the scanner opens, when the continuous toggle changes, and when the
  scanner closes.
- Called from the main thread only (results are already posted to main).
- The existing 1s `ScanThrottle` stays unchanged underneath (rate floor).

### Persistence

- New file `scanner_settings` (mode private, same pattern as `ScanHistoryStore`):
  - `continuous_scan: Boolean` (default false)
  - `translate_scan_actions: Boolean` (default false)
- Service reads fresh values on scanner open and per result — toggling takes effect
  without restarting the IME.

## Feature 2 — Scan actions

### UI

- `activity_setup.xml`: a new "Scanning" `MaterialCardView` (same visual pattern as
  the camera/IME cards) with title, one-line description, and a switch bound to
  `translate_scan_actions`: "Translate Enter & Tab in scanned data".
- Copy must not promise more than the feature does: only scanned text is affected.

### Translation — `ScanTranslator`

New pure class in `scanner/` package:

```
sealed interface ScanEvent {
    data class Type(val text: String) : ScanEvent
    data object Enter : ScanEvent
    data object Tab : ScanEvent
}
fun translate(raw: String): List<ScanEvent>
```

Rules:
- `\r\n` and lone `\r` normalize to one `\n` boundary.
- Consecutive identical special characters collapse to ONE event
  (`"A\n\nB"` → Type A, Enter, Type B) to prevent accidental double-submit.
- Leading and trailing delimiters produce events too
  (`"ABC\n"` → Type ABC, Enter).
- Empty segments are skipped (no empty `Type` events).
- Plain text without specials → single `Type` event equal to input.

### Application

- `handleBarcodeResult` (and only it — typed keys and history re-send stay raw):
  - When `translate_scan_actions` is OFF: today's single `commitText`.
  - When ON: iterate events:
    - `Type` → `commitText(segment, 1)`
    - `Enter` → reuse `EnterDispatcher.dispatch` with the service's current
      `enterBehavior` (resolved per field in `onStartInput`): form fields perform
      their editor action, multiline fields receive `\n`
    - `Tab` → `sendKeyEvent` DOWN+UP `KEYCODE_TAB`
- Combined with continuous scan: each accepted scan emits its event sequence and
  the scanner keeps running.

## Bug — backspace deletes both sides

- `BarcodeKeyboardService.onBackspace()`, no-selection branch:
  `deleteSurroundingText(1, 1)` → `deleteSurroundingText(1, 0)`.
- Selection branch unchanged (DEL key event pair).
- Behavior with cursor mid-text becomes: delete exactly the character before the
  cursor, cursor stays between the same two neighbors.

## Testing

### Unit tests (Robolectric/JVM, TDD — write first, watch them fail)

1. `BackspaceBehaviorTest` — Editable at `abcdef`, selection `(3,3)`, one backspace
   → `abdef`; selection `(2,4)` → DEL event path leaves `abef`.
2. `DuplicateSuppressorTest` — repeat suppressed, different value re-arms same
   value later, reset clears memory.
3. `ScanTranslatorTest` — plain text; `A\nB`; `A\tB`; `\r\n`; lone `\r`; consecutive
   `\n\n`; trailing newline; leading tab; empty string; only-specials string.
4. Service-level wiring tests where the existing test harness allows (extracted
   classes keep the service thin; the delta in `handleBarcodeResult`/`onBackspace`
   is covered through the units above plus device QA).

### Device QA (both connected devices, API 34)

- XCover5 (`R58RB1N07TD`, 720×1480) and Fire 6 (`FIRE60000000005065`, 720×1612):
  install debug build, run the standard field-tester flow.
- Backspace mid-word in tester field → single left delete, both devices.
- Continuous ON: scan two different QR codes → both commit, scanner stays open;
  rescan same code → nothing happens; scan other code then the first again → it
  commits; close and reopen scanner → suppressed memory reset.
- Actions ON in a two-field form: payload `A\nB` fills field 1 then jumps to field 2;
  `A\tB` tabs; multiline field: newline inserted literally.
- Actions OFF: specials land as literal characters (today's behavior).
- No regressions to v1.0.2 flows: gear key, `#+=` layer, history, permission gate,
  nav-bar padding unchanged on API 34 (zero insets).

### Release note

No version bump in this development line. When the product owner signs off, next
release is v1.1.0 / versionCode 4 (two features + one fix ⇒ minor).

## Out of scope

- Configurable prefix/suffix rules engine (per-code transformations).
- Suppressing the trailing-newline Enter (policy decided: it fires).
- Translating actions for history re-send or typed input.
- A/B toggle in the keyboard layer (switches live in scanner + setup only).
