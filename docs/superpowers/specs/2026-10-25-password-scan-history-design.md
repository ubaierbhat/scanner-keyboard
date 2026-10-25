# Design: Don't store scanned codes to history when scanning into a password field

Date: 2026-10-25

## Intent & why

Scanner Keyboard currently records every accepted barcode scan into
`ScanHistoryStore`, regardless of what kind of field the code was typed
into. When the user scans into a password field, the scanned value is
sensitive (it is exactly what the history panel would then display in
plain text). The scan should still be typed into the password field —
barcode-driven password entry keeps working — but history must not
remember that particular value.

## Goals

- A scan that is committed into a password field is typed into the field
  as usual.
- Such a scan is NOT added to scan history (no new entry, no reordering
  of existing entries, no dedupe/prune side effects).
- Detection is accurate at the moment of scanning, including when focus
  moves to a different field between scans (continuous scan mode with
  Tab/Enter actions).

## Non-goals / anti-goals

- Do NOT block or alter committing text into password fields.
- Do NOT affect the keyboard's own typed characters (regular key input
  into password fields is not history-related anyway).
- Do NOT change the clipboard-capture history entry
  (`captureClipboardEntry`) — that captures the system clipboard on
  session start, is not scan-derived, and follows different rules.
- No user-facing setting for this; the behavior is unconditional.

## Approaches considered

1. **Detached flag captured in `onStartInputView`** — compute
   `isPasswordField` on the captured `info` and store it in a service
   field like `enterBehavior` already is. Trade-off: stale when focus
   changes between scans inside one keyboard session (continuous mode),
   since only `onStartInputView` refreshes it.
2. **Read editor info at scan time (chosen)** — evaluate
   `currentInputEditorInfo` in `recordScan`'s caller at the moment of the
   scan. The IME framework keeps this up to date across focus changes
   within a session, so it is correct for the Tab-then-scan-tab pattern
   that continuous mode produces. Small extra cost, none noticeable.
3. **Inspection via InputConnection** (`getSelectedText` / extracted
   text) to sniff field content or transformation method — rejected:
   fragile across apps and restrictions, unnecessary since `inputType`
   is the standard signal.

## Approach (as picked: #2)

- New small pure helper next to the other keyboard-input helpers, e.g.
  `keyboard/PasswordFieldDetector.kt`:

  A field is a password field when its `inputType` is:
  - `TYPE_CLASS_TEXT` with variation
    `TYPE_TEXT_VARIATION_PASSWORD` (covers the standard password field),
    `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` (covers "show password"
    toggles and password-visibility fields),
    `TYPE_TEXT_VARIATION_WEB_PASSWORD` (WebView password inputs), or
  - `TYPE_CLASS_NUMBER` with variation `TYPE_NUMBER_VARIATION_PASSWORD`
    (numeric PIN fields).

  Anything else — including `TYPE_NULL` and missing editor info — is not
  a password field (fail open: scan still recorded).

- `BarcodeKeyboardService`: at scan time in `handleBarcodeResult`, only
  call `recordScan(text)` when
  `!PasswordFieldDetector.isPasswordField(currentInputEditorInfo?.inputType ?: InputType.TYPE_NULL)`.
  Committing the scan (`emitScanEvents`) is unchanged and happens
  regardless.

## What's left to the builder

- Naming/placement of the helper consistent with existing files
  (`InputTypeLayouts`, `EnterActionResolver` are the pattern).
- Unit tests: `PasswordFieldDetector` truth table (all password
  variations, visible-password, web-password, number-password, plain
  text, number, datetime, TYPE_NULL); service-level behavior covered by
  the detector test plus whatever fits existing service test style.

## Verification

- Unit tests for the detector pass.
- Existing test suite passes (`./gradlew test`).
- Debug app `TestFieldsActivity` has a password test field to sanity
  check by hand: scan into it → value is typed, history panel does not
  show it; scan into a normal field → history shows it.
