# Changelog

All notable changes to Scanner Keyboard are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Version numbers use the form `MAJOR.MINOR.PATCH`.

## [Unreleased]

No unreleased changes yet.

## [1.1.1] - 2026-10-10

### Fixed

- Continuous scanning inserted successive codes back-to-back with no
  separator. Every accepted scan is now terminated like a handheld scanner
  wedge: a line break in multiline fields, or the field's editor action
  (Done/Go/Search/Send/Next) in single-line fields, so codes stack one per
  line. Payloads that already end with a newline are not double-entered, and
  suppressed duplicate scans add nothing.

## [1.1.0] - 2026-10-10

### Added

- Continuous scanning: a switch in the scanner's option strip keeps the camera
  rolling and commits every new code; consecutive duplicate scans are ignored.
  State is remembered.
- Scan actions (opt-in via the scanner's Form switch): newline and tab
  characters in scanned data are translated into Enter (the field's editor
  action, or a real newline in multiline fields) and Tab focus moves, so
  multi-field forms can be filled with a single scan.
- Option strip in the scanner viewfinder holds the Continuous and Form
  switches. The separate Scanning card on the setup screen was removed — scan
  behavior now lives next to the viewfinder where it is used.

### Fixed

- Backspace with the cursor in the middle of text deleted a character on both
  sides of the cursor. It now deletes only the character before the cursor.

## [1.0.2] - 2026-10-06

### Fixed

- Last keyboard row hidden behind the on-screen navigation buttons on devices
  with forced edge-to-edge (Android 15+ with targetSdk 36). The input view now
  consumes the system-bar insets as padding.

## [1.0.1] - 2026-10-06

### Fixed

- Settings gear icon was asymmetric (a broken tooth). Replaced with the standard
  Material Design cog.
- `#+=` layer was missing the backtick (`` ` ``) and caret (`^`) and the closing
  parenthesis (`)`), so markdown code blocks could not be typed. Added them.

### Added

- Unit tests for the `#+=` symbol row (63 JVM tests total).

## [1.0.0] - 2026-10-04

First public release.

### Added

- QWERTY keyboard with shift (one-shot and caps lock), a numbers layer, and a
  `#+=` symbols layer.
- Smart Enter: fires the field's Search/Go/Send/Done action when present, and
  inserts a newline in multiline fields.
- Long-press backspace auto-repeat and key-press haptics.
- Long-press accent popups with drag-to-select on 17 letters.
- Underscore and euro keys on the extended symbols layer.
- Long-press popups: hyphen offers underscore; dollar offers euro, pound, yen;
  percent offers per-mille, cent.
- Phone and number field detection with a Samsung-style 4x4 dialpad.
- Fully offline barcode and QR scanner inside the keyboard window. Uses
  CameraX and the bundled ML Kit model. Supports QR, EAN-8/13, UPC-A,
  Code 39/93/128, ITF, Codabar, PDF417, Aztec, and Data Matrix. Includes a
  torch toggle, auto-insert at the cursor, and haptic confirmation.
- Scan history with clipboard capture: last 20 entries, newest first,
  deduplicated, tap-to-insert, and one-tap clear.
- Function toolbar with scanner, history, and settings keys. The settings key
  opens the app; Back returns you to the text field.
- Two-step setup wizard with live camera permission and IME status chips.
- Release build with R8 shrinking, resource shrinking, and upload-key signing.
- `targetSdk` 36 to meet the Google Play new-app policy.
- Apache-2.0 license with copyright and third-party attribution.
- GitHub Pages privacy policy and third-party license list.
- Architecture document, manual acceptance checklist, and Play Console
  preparation checklist.

### Changed

- Clamped keyboard height to the window measurement; rows scale proportionally
  when the space is smaller.
- Rendered all keys, icons, and toolbar glyphs in white on one dark theme.
- Centered toolbar icons in their keys and enlarged the Enter glyph.
- Kept the debug field tester in debug builds only.

### Fixed

- Aligned the dialpad to a 4x4 grid matching the Samsung reference, verified
  on device.
- Enter key newline handling for fields without an editor action, via the
  `NO_ENTER_ACTION` flag and unhandled-action fallback.
- Zombie scanner and history views after rotation: they now reparent to the
  live input container.
- Stale camera provider callbacks from finished scanner sessions are skipped.
- The field tester log no longer grows forever; it is capped to the current
  field.

[Unreleased]: https://github.com/ubaierbhat/scanner-keyboard/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/ubaierbhat/scanner-keyboard/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/ubaierbhat/scanner-keyboard/compare/v1.0.2...v1.1.0
[1.0.2]: https://github.com/ubaierbhat/scanner-keyboard/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/ubaierbhat/scanner-keyboard/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/ubaierbhat/scanner-keyboard/compare/cb647eb...v1.0.0
