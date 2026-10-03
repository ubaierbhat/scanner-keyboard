# Manual acceptance checklist (aim-dependent)

The phone lies camera-down on the desk; these checks need a human to aim it at the
Mac screen. Test assets live in this folder (no network is needed on the phone;
generators are listed in case you want fresh codes):

- `docs/acceptance/qr-SCAN-T9-QR-741258.png` — QR encoding the literal text
  `SCAN-T9-QR-741258` (regenerate: `curl -s "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=SCAN-T9-QR-741258" -o qr-SCAN-T9-QR-741258.png`)
- `docs/acceptance/ean13-5901234123457.png` — EAN-13 barcode encoding
  `5901234123457` (regenerate: `curl -s "https://barcode.tec-it.com/barcode.ashx?Data=5901234123457&Code=EAN13&Translate=on&ImageType=PNG&Download=Download" -o ean13-5901234123457.png`)

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
