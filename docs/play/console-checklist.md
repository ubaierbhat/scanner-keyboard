# Google Play Console Checklist — Scanner Keyboard

Use this sheet during one Console session. Copy each block verbatim.
Every fact cites its source file in this repo (verified at commit `11786d4`, 2026-10-04).

App facts (source: `app/build.gradle.kts:13-21`):

| Field | Value |
|---|---|
| Application ID | `org.ubaierbhat.android.barcodekeyboard` |
| Version code | 1 |
| Version name | 1.0.0 |
| minSdk | 24 (Android 7.0) |
| targetSdk / compileSdk | 36 |
| Privacy Policy URL | `https://ubaierbhat.github.io/scanner-keyboard/privacy.html` |
| Licenses page | `https://ubaierbhat.github.io/scanner-keyboard/licenses.html` |
| Source repo | `https://github.com/ubaierbhat/scanner-keyboard` |

---

## 1. App access

Select: "App provides access via username and password" → **No**.
Paste this text for the reviewer:

```text
This app is a keyboard input method (IME). No login or account is needed.
Testers must enable the keyboard once in system settings.
Path: Settings > System > Languages and input > On-screen keyboard >
Manage keyboards > enable "Scanner Keyboard". Some devices label the
menu "Languages". Then tap the keyboard switcher and pick Scanner Keyboard.
The launcher app opens a setup wizard that shows these two steps live.
The app exposes two components: MainActivity (launcher icon) and the
IME service BarcodeKeyboardService (protected by BIND_INPUT_METHOD).
No other entry points exist.
```

Evidence: `app/src/main/AndroidManifest.xml:18-38`; strings `app_name`/`ime_label` = "Scanner Keyboard" (`app/src/main/res/values/strings.xml:2,19`).

## 2. Ads

Select: "Does your app contain ads?" → **No**.
The app shows no ads and bundles no ad SDK (`app/build.gradle.kts:69-77`).

## 3. Target audience and content

1. Age range: do not target under-13 users. Keep the default mixed-audience declaration; the app is a general utility.
2. Select "Everyone" for target audience (no UGC, no chat, no social features).
3. IARC content questionnaire — suggested answers:

| Question | Answer | One-line justification |
|---|---|---|
| Online gambling for real money | No | The app contains no gambling. |
| Sexual content or nudity | No | The app shows keys, codes, and user text only. |
| Violence or serious violence | No | The app contains no violent imagery. |
| Hate speech or abhorrent conduct | No | The app displays no such content. |
| Profanity or crude language | No | All app labels are neutral. |
| Tobacco, alcohol, or drug use | No | The app depicts no substances. |
| Controversial topics | No | The app has no editorial content. |
| Horror or fear themes | No | The UI is a plain keyboard. |
| User-generated content | No | History entries stay on the device; nothing is published. |
| Chat or messaging with strangers | No | The app has no communication feature. |
| Sharing of personal data | No | Nothing leaves the device (`app/build.gradle.kts:69-77`). |

The only interactive surface is the keyboard itself and a camera preview the user opens on purpose (utility feature).

## 4. Data safety form

1. "Does your app collect or share any of the required user data types?" → **No**.
2. Data collected: **No**. Data shared: **No**. All follow-up questions (encryption, deletion, ad targeting) stay hidden or **N/A**.
3. Paste justification note:

```text
The app collects and shares no user data. It declares no INTERNET
permission, so Android blocks every network call
(app/src/main/AndroidManifest.xml:4 declares only CAMERA).
Scan history and copied-text captures live only in app-private
SharedPreferences with MODE_PRIVATE
(app/src/main/java/org/ubaierbhat/android/barcodekeyboard/history/ScanHistoryStore.kt:10-11).
Backup is disabled: allowBackup="false" (app/src/main/AndroidManifest.xml:11).
The clipboard read happens only when the keyboard opens, and the text
is stored on the same device
(app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt:84,309-330).
Uninstall removes all data. See the published policy:
https://ubaierbhat.github.io/scanner-keyboard/privacy.html
```

4. Third-party SDK declarations — for each SDK the scan detects, answer "does not collect or share data" (versions from `gradle/libs.versions.toml`):

| SDK | Version | Declaration |
|---|---|---|
| `com.google.mlkit:barcode-scanning` (bundled model variant) | 17.3.0 | Runs fully on-device; no data collected or shared. |
| `androidx.camera:*` (CameraX core/camera2/lifecycle/view) | 1.6.2 | Local camera preview only; no data collected or shared. |
| `androidx.appcompat:appcompat` | 1.8.0 | UI library; no data collected or shared. |
| `androidx.core:core-ktx` | 1.18.0 | Platform helpers; no data collected or shared. |
| `com.google.android.material:material` | 1.14.0 | UI components; no data collected or shared. |
| `com.google.firebase:firebase-components` (transitive inside ML Kit) | 16.1.0 | Dependency-injection registry only; no data collected or shared. |

No SDK can transmit anything: the app holds no INTERNET permission and ships no networking code.

5. Evidence note (internal, do not paste): the JDK-21 test-JVM block (`app/build.gradle.kts:61-67`) affects only local unit tests. It ships nothing in the APK or AAB. Ignore it for data safety.

## 5. Sensitive permission justification (CAMERA)

Paste for the CAMERA declaration:

```text
This app needs CAMERA for one purpose: scanning barcodes and QR codes
inside the keyboard. Scanning is the app's core feature.
Processing is fully on-device with ML Kit's bundled model. The app
declares no INTERNET permission, so image data cannot leave the device.
The app never saves or persists camera frames. Frames exist only in
memory during the live preview, then are discarded.
The camera opens only after the user taps the scan key: the preview is
user-initiated and visible on screen.
The app releases the camera as soon as the scanner closes, the keyboard
hides, or the device rotates. The camera is never used in the
background. If the permission is missing, the app shows an inline
prompt and never opens the camera.
```

Evidence: camera bind/unbind in `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/scanner/ScannerView.kt:85-142` (`unbindAll` at :118); rotation closes the camera (README "Rotation while the scanner is open closes the camera"); fresh-install permission gate verified on device (`.superpowers/sdd/2026-10-04-play-release-prep/task-3-report.md` row 3).

## 6. Store listing

1. Title (16/30 characters):

   `Scanner Keyboard`

2. Short description (56/80 characters):

   `Offline barcode and QR scanner built into your keyboard.`

3. Full description (STE wording, mirrors README Features + Privacy; keep ≤4000 characters):

```text
Scanner Keyboard puts a barcode and QR scanner inside your keyboard.
Tap the scan key. The keyboard opens a live camera view. Point it at a code.
The app inserts the decoded text at your cursor, then buzzes once.

Features
- Keyboard: full QWERTY with shift and caps lock, a numbers layer, and a
  symbols layer. Smart Enter runs the field's action or adds a new line.
  Long-press backspace repeats. Key presses vibrate.
- Barcode scanner: a live camera view inside the keyboard window. It
  detects QR, EAN-8, EAN-13, UPC-A, Code 39, Code 93, Code 128, ITF,
  Codabar, PDF417, Aztec, and Data Matrix. A torch toggle helps in dark places.
- Accents: long-press a letter. Slide to an accented variant and release.
- History: the app keeps your last 20 scans and copies on your device.
  Tap an entry to insert it again. Clear history with one tap.
- Toolbar: scan, history, and settings keys sit above the letters.
- Setup screen: the first-run wizard shows camera permission and
  keyboard enable status live.

Privacy
- Scanning runs 100% on your device.
- The app declares no INTERNET permission. Android blocks every network call.
- Decoding uses the bundled ML Kit model. No image or text ever leaves your phone.
- The camera opens only while you keep the scanner on screen. It releases the
  camera when the scanner closes, the keyboard hides, or you rotate the device.
- The app never saves camera images.
- Scan history and copied text stay in the app's private storage. Cloud backup
  is disabled. Uninstalling deletes everything.

No ads. No in-app purchases. No account or login needed. The app works fully
offline.
```

4. Privacy Policy URL (all locations): `https://ubaierbhat.github.io/scanner-keyboard/privacy.html`
5. Website (optional): `https://ubaierbhat.github.io/scanner-keyboard/index.html`
6. Category: Tools.
7. Screenshots (phone, ≥2 required; 5 supplied — all exist in `docs/images/`):
   - `docs/images/keyboard.png`
   - `docs/images/phone-dialpad.png`
   - `docs/images/scanner.png`
   - `docs/images/history-panel.png`
   - `docs/images/settings-key.png`
8. App icon (512×512 PNG, verified): `docs/store/icon-512.png`
9. Feature graphic (1024×500): **REQUIRED-BY-HUMAN — optional for release, not present in repo.** Create one before publishing the listing.
10. Contact email: **REQUIRED-BY-HUMAN** (not stored in this repo).

## 7. Testing plan

Rules for a personal developer account:

1. Internal testing track: optional. Skip it, or use the same AAB first for fast smoke checks.
2. Closed testing: create an email list with **at least 12 opted-in Gmail accounts**.
3. Keep the build in closed testing for **14 continuous days** with the testers opted in. This also satisfies the personal-account policy gate for production access.
4. After 14 days: press "Apply for production". Production review takes **≤7 days** (usually faster).
5. The production form requires a published Privacy Policy URL — already satisfied (see Section 6 item 4; policy shipped by Task 4).

Testing-readiness evidence (verified device QA):

- 58 unit tests pass on the release configuration (`.superpowers/sdd/2026-10-04-play-release-prep/task-1-report.md`, task-3-report.md "Build results").
- Signed R8 release QA passed on a physical Samsung Galaxy XCover5 (`R58RB1N07TD`, Android 14/API 34): typing, dialpad, accents, scanner open/close with camera lifecycle, history persist + re-insert, settings jump, zero FATAL entries (task-3-report.md QA matrix rows 1-9).
- Known caveat: QA ran on API 34; targetSdk-36 runtime specifics are unverified there (task-1-report.md Concern 1).
- One human step remains before release: aim the rear camera at `docs/acceptance/qr-SCAN-T9-QR-741258.png` and confirm decode → insert → buzz → close (task-3-report.md row 5).

## 8. Upload steps

1. Build the AAB: run `./gradlew bundleRelease`.
2. Artifact path: `app/build/outputs/bundle/release/app-release.aab`.
   Current on-disk artifact: **13,427,613 bytes (12.8 MB)**, built 2026-10-04 with the final committed config (task-3-report.md "Build results"; matches this file). It is signed with the upload key. Never commit it (`build/` is gitignored).
3. In the Console: open your release track → "Create new release" → upload `app-release.aab` from "App bundle".
4. First upload triggers **Play App Signing enrollment**. Keep Google-managed signing ("Play App Signing by Google").
5. Register the existing upload key: keystore `upload-keystore.jks` (alias `upload`), repo root, untracked; wiring in `app/build.gradle.kts:7-10,27-36,43` reads `keystore.properties`. The password lives in task-3-report.md "Keystore passwords" — move it to a password manager.
6. **Do NOT select "Manage your own signing keys (app signing key management opt-out)".**
7. Bump versionCode only on later uploads; this release ships versionCode 1 / versionName 1.0.0.

## 9. Human steps (one-time, outside the repo)

- [ ] Create the Google Play developer account. The fee is 25 US dollars, one time.
- [ ] Use a personal account. A D-U-N-S number is not needed (only organization accounts require it).
- [ ] Complete the payments profile (bank) and the tax profile.
- [ ] Read the personal-account policy: the 12-tester / 14-day closed test in Section 7 unlocks production.
- [ ] Create the feature graphic 1024×500 (Section 6 item 9).
- [ ] Supply the contact email (Section 6 item 10).
- [ ] Run the manual decode-by-aim check (Section 7) before applying for production.
- [ ] Back up `upload-keystore.jks`, `keystore.properties`, and its password off-device.
