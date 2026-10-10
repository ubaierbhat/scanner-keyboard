# Store Listing Copy — Scanner Keyboard (v1.1.0)

Play Store: https://play.google.com/store/apps/details?id=org.ubaierbhat.android.barcodekeyboard

Paste targets map to Play Console: Main store listing (Title, Short description,
Full description, Promo text is under Marketing materials in newer consoles) and
Release > Release notes ("What's new") for the first track upload.

All limits verified: Title ≤ 30, Short ≤ 80, Promo ≤ 85, Full ≤ 4000, What's new ≤ 500.
Rules followed: no superlatives, no competitor comparisons, no health/financial
claims, every statement true of the artifact (offline proven by aapt2 in
docs/security/masvs-compliance.md appendix).

---

## Title (primary)

```
Scanner Keyboard
```

Alternatives (keyword-forward, both ≤ 30):

```
Scanner Keyboard: QR & Barcode
Scan Keyboard — Barcode & QR
```

Note: "Scanner Keyboard: QR & Barcode" is 30 chars — fits exactly. If chosen, keep
the app label unchanged; only the Play display title changes.

## Short description (≤ 80)

Primary (70 chars):

```
Scan barcodes and QR codes straight into any text field. 100% offline.
```

Alternative (73 chars):

```
A keyboard with a built-in scanner. Point, scan, type — no app switching.
```

## Promo text (≤ 85)

(70 chars):

```
The barcode scanner that lives in your keyboard. No cloud. No account.
```

## Full description

```
Scan it. Don't type it.

Scanner Keyboard puts a live barcode and QR scanner inside your keyboard. Tap the
scan key, point the camera, and the code lands in the field you are typing in — in
any app. No app switching. No copy-paste steps. The keyboard closes back and you
keep working.

Who it helps

- Logistics and courier teams: waybill and tracking numbers into TMS apps, forms,
  and email.
- Warehouses and stock rooms: EAN, UPC, Code 128 and ITF labels into WMS, sheets,
  and inventory tools — less typing on small screens.
- Retail and online sellers: scan a product code to name, search, or list it.
- Field technicians: serial numbers and part codes from machines, labels, and plates.
- Event and door staff: QR tickets and badges.
- Offices and libraries: asset tags and quick stock checks with no extra software.

It is a keyboard, so it works everywhere you can type: ERP, CRM, browser, forms,
email, notes, spreadsheets.

Built for privacy-sensitive workplaces

- 100% offline. The app declares no INTERNET permission. Android itself makes data
  transfer impossible.
- Works in airplane mode and on isolated or zero-signal devices.
- No account, no ads, no tracking, no analytics.
- Camera frames are decoded on the device and never stored.
- Open source (Apache-2.0). Audit every claim yourself.

Made for real shifts

- Smart Enter: runs Search, Go, Next, or Done when the field supports it.
- Scan and clipboard history: your last 20 codes, one tap to insert again.
- A complete keyboard too: accents, symbols, and a familiar dial-pad for phone and
  number fields.

Supported codes: QR, EAN-8, EAN-13, UPC-A, Code 128, Code 39, Code 93, ITF,
Codabar, PDF417, Aztec, Data Matrix.

Setup: enable Scanner Keyboard in system settings, allow the camera, done.

Please note: Scanner Keyboard is a typing tool, not an inventory app. It reads a
code and types it into the app you already use.

Source code and feedback: https://github.com/ubaierbhat/scanner-keyboard
```

(1,977 chars — well under 4,000; first line "Scan it. Don't type it." hooks the
truncated list view.)

## Release notes / What's new (≤ 500)

### v1.1.0 (this upload)

```
Scanner Keyboard 1.1.0

New scanner controls directly on the keyboard:

- Continuous scanning: keep the camera rolling and scan code after code — duplicate repeats are ignored.
- Form filling: turn on Form mode to convert newlines and tabs in barcodes into Enter and Tab presses, filling multiple fields in one scan.

Plus a fix for a Backspace issue when editing text.
Still 100% offline: no account, no ads, no data collection.
```

### v1.0.0 / v1.0.1 / v1.0.2 drafts (superseded, do not reuse)

```
Welcome to Scanner Keyboard 1.0.0!

A keyboard with a built-in, fully offline barcode and QR scanner. Point the camera
and the code is typed into any field, in any app.

In this release:
- Real-time scanning: QR, EAN, UPC, Code 128/39/93, ITF, PDF417, Aztec
- Scan and clipboard history with one-tap insert
- Smart Enter for search, next, and go actions
- Full QWERTY with accents, symbols, and a phone dial-pad
- Torch for low light

100% offline. No account, no ads, no data collection.
```

## Graphics already covered

Feature graphic + screenshots guidance lives in console-checklist.md §6; ship with
`docs/images/keyboard.png`, `phone-dialpad.png`, `scanner.png`, `history-panel.png`,
`settings-key.png` (≥ 2 phone screenshots required, list order matters).
