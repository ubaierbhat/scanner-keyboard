# Scanner Keyboard

A source-available Android keyboard (IME) with a built-in, fully offline barcode
and QR code scanner (license pending — see [License](#license)). Tap **SCAN** and
the keyboard swaps to a live camera viewfinder; when a
code is detected, the decoded text is inserted at the cursor of the field you are
typing in, the phone buzzes, and the scanner closes back to the keyboard.

Built with Kotlin, the XML View system, CameraX, and ML Kit barcode scanning
(bundled model). Package: `org.ubaierbhat.android.barcodekeyboard`.

## Privacy

Scanning is 100% on-device:

- The app declares **no `INTERNET` permission**, so it cannot make network calls at
  any point — this is enforced by the Android platform, not just by convention.
- Barcode/QR decoding runs entirely on your phone using ML Kit's bundled model; no
  images or decoded values are ever uploaded or logged.
- The camera is opened only while the scanner view is on screen and is released as
  soon as the scanner closes, the keyboard hides, or the app is switched away from.
- Scan history and clipboard captures are stored only in the app's private storage on
  your device.

## Features

- **Keyboard** — full QWERTY with shift (tap for one-shot, long-press for caps
  lock), a numbers layer and a second `#+=` symbols layer, smart Enter (performs the
  field's Search/Go/Send/Done editor action when present, inserts a newline in
  multiline fields), long-press backspace auto-repeat, and key-press haptics.
- **Barcode scanner** — real-time camera viewfinder inside the keyboard window.
  Detects QR, EAN-8/13, UPC-A, Code 39/93/128, ITF, Codabar, PDF417, Aztec, and
  Data Matrix. Auto-inserts the decoded text, buzzes, and closes. Includes a torch
  toggle for dark environments.
- **Accents** — long-press any supported letter (a, c, d, e, g, h, i, j, l, n, o,
  r, s, t, u, y, z) to pop up its accented variants; slide to one and release to
  insert it (uppercase when shift/caps is active).
- **History** — the last 20 scanned and copied texts (newest first, deduplicated).
  While Scanner Keyboard is your default keyboard, copied text is picked up into
  history the next time the keyboard opens; tap a history entry to re-insert it at the
  cursor. Clearable with one tap.
- **Setup wizard** — first-run screen with live status for camera permission and
  keyboard enablement.

## Setup

1. Install the app (see [Build](#build) for the debug APK, or sideload an APK file).
2. Open **Scanner Keyboard** from your app drawer. The setup screen shows two steps
   with live status chips:
   - **Allow camera access** — tap *Grant camera permission* and accept the system
     dialog.
   - **Enable the keyboard** — tap *Open keyboard settings*, turn on **Scanner
     Keyboard** in the input methods list (confirm any popup warning).
3. When both chips read *Granted* / *Enabled*, the screen shows **Scanner Keyboard
   ready**.
4. Select it as your active keyboard: tap any text field to bring up the current
   keyboard, then open the input-method picker (the small keyboard/globe icon in the
   navigation bar or the "Choose input method" notification) and choose **Scanner
   Keyboard**.

No account, no network, no configuration — you're done.

## Usage

- **Typing** — standard QWERTY; `123` switches to numbers/punctuation, `#+=` to
  more symbols, `ABC` returns to letters.
- **Shift / caps** — tap ⇧ to capitalize the next letter; long-press ⇧ to lock caps
  (the key stays lit); tap again to unlock.
- **Accents** — press and hold a letter, slide to a variant in the popup strip, and
  release to insert it. Release back over the original key to type the plain letter,
  or outside the strip to cancel.
- **Scanning** — tap **SCAN** in the bottom row. Point the back camera at a code; the
  live preview shows what the camera sees (aim so the code fits the frame). Tap
  **Torch** to fire the LED
  in low light. On detection the code text is inserted and the view closes. Tap ✕ to
  close without scanning. If camera access was revoked, SCAN shows an inline prompt
  with an *Open setup* button.
- **History** — tap **HIST** to browse recent scans and copies. Tap an entry to
  insert it (the panel closes); CLEAR empties the list; CLOSE returns to the
  keyboard.

## Build

Requirements: JDK 17 and an Android SDK (minSdk 24, compileSdk 36). Point
`local.properties` (`sdk.dir=...`) or `ANDROID_HOME` at your SDK.

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew test                 # JVM unit tests (Robolectric)
./gradlew installDebug         # build + install on a connected device/emulator
```

If you use the [Android CLI](https://developer.android.com/studio), `android run`
also deploys the app to a connected device; Gradle is the only hard requirement.

After installing, re-enable/re-select the keyboard in system settings (Android may
reset the active IME on reinstall).

## Limitations and roadmap

v1 is deliberately minimal. Not included (planned roadmap items):

- No autocorrect, glide/swipe typing, or swipe gestures.
- No theme/keyboard customization (single dark-ish static look).
- No split or one-hand mode.
- Single layout/subtype: English QWERTY with long-press accents (no per-language
  keyboard layouts).
- No per-app keyboard profiles.

Rotation while the scanner is open closes the camera and lands you back on the
keyboard — this is intentional, so no camera ever stays open "by surprise".

## License

License: not yet chosen (see project tracker). No `LICENSE` file is present until
one is picked; until then all rights are reserved by the author.
