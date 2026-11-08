# Manual acceptance checklist (aim-dependent)

The phone lies camera-down on the desk; these checks need a human to aim it at the
Mac screen. Test assets live in this folder (no network is needed on the phone;
generators are listed in case you want fresh codes):

- `docs/acceptance/qr-SCAN-T9-QR-741258.png` — QR encoding the literal text
  `SCAN-T9-QR-741258` (regenerate: `curl -s "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=SCAN-T9-QR-741258" -o qr-SCAN-T9-QR-741258.png`)
- `docs/acceptance/ean13-5901234123457.png` — EAN-13 barcode encoding
  `5901234123457` (regenerate: `curl -s "https://barcode.tec-it.com/barcode.ashx?Data=5901234123457&Code=EAN13&Translate=on&ImageType=PNG&Download=Download" -o ean13-5901234123457.png`)
- `docs/acceptance/qr-form-F1SCAN-F2SCAN-F3SCAN.png` — QR encoding
  `F1SCAN`-TAB-`F2SCAN`-TAB-`F3SCAN` (TAB = URL-encoded `%09` in the data
  parameter) for form-fill Tab routing (regenerate:
  `curl -s "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=F1SCAN%09F2SCAN%09F3SCAN" -o qr-form-F1SCAN-F2SCAN-F3SCAN.png`)

Helper pages (serve while testing, do not leave them running):

- `docs/acceptance/qa-viewer.html` — one page with the QR and EAN-13 codes
  and a switch between them. Serves any folder over HTTP: `python3 -m
  http.server 8936` in `docs/acceptance`, then open
  `http://localhost:8936/qa-viewer.html`.
- `docs/acceptance/qa-multi.html` — a multiline field and a three-field form
  the phone can fill over the browser. The phone reaches the Mac through
  `adb reverse tcp:8936 tcp:8936`, then the phone browser opens
  `http://127.0.0.1:8936/qa-multi.html`.

Prep (one time):

```bash
adb -s R58RB1N07TD shell ime enable org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService
adb -s R58RB1N07TD shell ime set org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService
adb -s R58RB1N07TD shell pm grant org.ubaierbhat.android.barcodekeyboard android.permission.CAMERA
```

Raise Mac screen brightness to max. Open both PNGs in Preview
(`open docs/acceptance/`) and zoom to fill the screen.

1. **Live QR insert (+ history write).** Put focus in any text field (e.g.
   Settings → search) → tap **SCAN** → lift the phone and aim its BACK camera
   ~15-25 cm at the QR on screen.
   Expect: field receives exactly `SCAN-T9-QR-741258`, one haptic buzz, scanner
   auto-closes to the keyboard. Then tap **HIST** → the value is the newest entry.
2. **Live EAN-13 insert.** Same flow, aiming at the barcode PNG.
   Expect: field receives `5901234123457` (1D format proves non-QR decoding),
   auto-close + buzz, HIST gets a new entry.
3. **Back-to-back throttle behavior + airplane-mode live decode.**
   a. With a code visible to the camera, scan twice in a row (aim QR → close →
      reopen → aim the *other* code within ~1 s): each new scan session must insert
      its code once — no double-insert, no suppression of a genuinely different
      code. (Same-session multi-detection is impossible by construction since the
      first detection auto-closes; the 1000 ms window is unit-pinned.)
   b. Enable airplane mode (`adb -s R58RB1N07TD shell cmd connectivity airplane-mode
      enable`), aim the QR → expect the same successful insert with no network.
      Then `airplane-mode disable`. Expect: identical result to step 1 — proving
      end-to-end offline decode.
4. **Mid-stream revoke re-check (Android 14).** Open SCAN, let it stream (live
   preview visible), then run
   `adb -s R58RB1N07TD shell pm revoke org.ubaierbhat.android.barcodekeyboard android.permission.CAMERA`.
   Expect: no crash dialog, keyboard returns (note: on stock Android 14 `pm revoke`
   force-stops the app, so the IME process may be system-killed and immediately
   recreated — that is acceptable; the key outcomes are no FATAL in logcat, camera
   released, and the scanner cleanly re-openable after re-granting via the setup
   wizard). Then tap **SCAN** again → expect the inline "Camera access is needed to
   scan" prompt with an **Open setup** button.
5. **Password-field scan is typed but not saved (v1.2.0+).** Focus a real
   password field (for example a Wi-Fi "Add network" password box, or any
   login form) → open **SCAN** → aim at the QR → scan. Expect: the text is
   typed into the password field, one buzz, auto-close; then **HIST** shows no
   new entry. Then focus a normal text field and scan again → expect: the scan
   is inserted and **HIST** gains (or moves to the top) an entry.
6. **Form-mode Tab routing (v1.2.0 helper page).** Serve `qa-multi.html` and
   reach it from the phone as described above. Focus form field 1 → open
   **SCAN** with the **Form** switch on → aim at
   `qr-form-F1SCAN-F2SCAN-F3SCAN.png`. Expect in one scan: `F1SCAN` in field
   1, focus moves, `F2SCAN` in field 2, focus moves, `F3SCAN` in field 3,
   auto-close.

## Results log

- **2026-11-08 — v1.2.0 (versionCode 6), signed release.** Manual verification
  passed on Fire 6 (`FIRE60000000005065`, API 34) with full operator support
  (USB driving + human aim). Covered, step by step: (1) live QR decode → exact
  insert `SCAN-T9-QR-741258` in Settings search → buzz → auto-close → history
  newest entry; (2) EAN-13 decode → `5901234123457` insert → history gains the
  entry; (3) **password-field exclusion**: Wi-Fi "Add network" password box —
  scan typed into the field as masked dots, no history entry (history kept
  its two prior entries); (3b) next scan into the normal search field was
  inserted and returned to the top of history, isolating the skip to password
  fields; (4) Continuous mode in the browser multiline field (`qa-multi.html`
  via `adb reverse`): QR then EAN-13 landed one per line, camera stayed on;
  (5) Form mode in the three-field form: one scan of
  `qr-form-F1SCAN-F2SCAN-F3SCAN.png` filled `F1SCAN`/`F2SCAN`/`F3SCAN` with
  Tab focus routing, auto-close. Zero FATAL/AndroidRuntime entries in the
  full session logcat; camera released on close (privacy dot off, camera
  service UNCONFIGURED). Continuous and Form switches returned to the off
  defaults after the session.
- **2026-10-10 — v1.1.1 (versionCode 5), signed release.** Manual scanning
  verification passed on all three test devices after user testing: Samsung
  Galaxy XCover5 (`R58RB1N07TD`, API 34), Amazon Fire 6
  (`FIRE60000000005065`, API 34), Samsung Galaxy S22 (`RFCT71FS61W`, API 36 /
  Android 16). Covered: live camera decode → insert → buzz → close (one-shot),
  continuous mode with the per-scan separator (line break in multiline
  editors, field action elsewhere), consecutive-duplicate suppression, and
  Form-mode newline/tab routing. Decode-by-aim — the standing human-only
  caveat — is hereby cleared for v1.1.1.
