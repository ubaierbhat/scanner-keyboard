# Play Release Preparation & Open-Source Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Scanner Keyboard publishable on Google Play (API-36 target, clean release artifact, privacy/licenses pages on GitLab Pages, Console checklist) and presentable as a trustworthy open-source project (STE-100 docs, community files, tagged v1.0.0), with a written OWASP MASVS v2.1 compliance record.

**Architecture:** Config-only release hardening in Gradle (R8 + signing from an untracked properties file), source-set isolation moves the debug-only field tester out of the release APK, a committed static `public/` site published by one GitLab Pages CI job, and new docs under `docs/` written in ASD-STE100-adapted Simplified English. Play Console forms are answered from a checked-in declaration sheet so every claim is auditable from the repo.

**Tech Stack:** Kotlin, AGP 9.0.1 (built-in Kotlin), Gradle 9.1, CameraX 1.6.2, ML Kit barcode-scanning 17.3.0, Robolectric/JUnit, GitLab CI (`pages` job), Apache-2.0.

## Global Constraints

- Package `org.ubaierbhat.android.barcodekeyboard`; minSdk 24; compileSdk 36; final targetSdk **36**.
- The app must never declare `INTERNET` or any new permission. `allowBackup="false"` stays.
- No code comments; strings/dimens/colors in XML; XML Views only (no Compose).
- Documentation language: ASD-STE100 adapted — active voice, one instruction per sentence, sentences ≤ 20 words, prefer approved words ("use", not "utilize"; "start", not "commence").
- Never commit keystores, passwords, or `local.properties`. Test device is XCover5 `R58RB1N07TD` only; after every `adb install -r` re-run `ime enable` + `ime set`; never `am force-stop` the IME package.
- Test suite currently 58 green (`./gradlew testDebugUnitTest`); it must stay green in every task.
- Release signing = local **upload key** + Play App Signing (Play holds the release key). Keystore lives outside git.
- Runtime input needed from the user before Tasks 4/6/8 (ask via question tool): GitLab project URL/namespace, public contact email, desired release name "1.0.0".

---

### Task 1: targetSdk 36 bump and on-device verification

**Files:**
- Modify: `app/build.gradle.kts:11` (`targetSdk = 35` → `36`), `app/build.gradle.kts:12-13` (`versionCode = 2`? keep 1 — first release; `versionName = "1.0.0"`)

**Interfaces:**
- Consumes: nothing.
- Produces: release-ready API level used by all later builds.

- [ ] **Step 1: Bump the API level and normalize the version string**

In `app/build.gradle.kts` change:

```kotlin
targetSdk = 36
```

and

```kotlin
versionName = "1.0.0"
```

(versionCode stays 1 — first published release.)

- [ ] **Step 2: Run the full suite and build both variants**

Run: `./gradlew testDebugUnitTest assembleDebug assembleRelease -q`
Expected: BUILD SUCCESSFUL, 58/58 tests. (assembleRelease must succeed unsigned: `unsigned` outputs or a signing error → if it errors asking for a password, ignore via `-Pandroid.injected.signing.release.enabled=false`; the debug build is what installs on device.)

- [ ] **Step 3: Install the debug APK on the XCover5 and re-select the IME**

```bash
adb -s R58RB1N07TD install -r app/build/outputs/apk/debug/app-debug.apk
adb -s R58RB1N07TD shell ime enable org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService
adb -s R58RB1N07TD shell ime set org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService
```

- [ ] **Step 4: On-device smoke matrix under API-36 target**

Drive `TestFieldsActivity` through MainActivity → "Open field tester" (bounds from `uiautomator dump`, currently [195,1036][524,1132] center (359,1084)). Verify with screenshots:
1. QWERTY types, shift, accent popup.
2. Phone field shows 4×4 dialpad, digit commit works.
3. Multiline field inserts newlines with ENTER.
4. Toolbar gear opens MainActivity, Back returns to field.
5. Scanner opens, finds `docs/acceptance/qr-SCAN-T9-QR-741258.png` (display it on the other phone / monitor), inserts text, buzzes.
6. History panel opens and re-inserts an entry.
Expected: no visual insets regression on Android 16-target: MainActivity content must not sit under status/nav bars — capture `/tmp/ste100-sdk36-main.png` and confirm readable margins.

- [ ] **Step 5: Copy evidence and commit**

```bash
cp /tmp/ste100-sdk36-main.png .superpowers/sdd/2026-09-27-scanner-keyboard-plan/
git add -A && git commit -m "build: raise targetSdk to 36 for Play new-app policy"
```

---

### Task 2: Debug-gate the field tester (remove from release APK)

**Files:**
- Create: `app/src/debug/AndroidManifest.xml`, `app/src/debug/java/org/ubaierbhat/android/barcodekeyboard/TestFieldsActivity.kt` (moved)
- Move: `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/TestFieldsActivity.kt` → debug; `app/src/main/res/layout/activity_test_fields.xml` → `app/src/debug/res/layout/`
- Modify: `app/src/main/AndroidManifest.xml` (delete TestFieldsActivity block), `app/src/main/java/org/ubaierbhat/android/barcodekeyboard/MainActivity.kt` (class literal → explicit ComponentName)

**Interfaces:**
- Consumes: MainActivity debug button guarded by `BuildConfig.DEBUG` (MainActivity.kt:39).
- Produces: release APK containing no `TestFieldsActivity` class or layout; debug behavior unchanged.

- [ ] **Step 1: Move the files and write the debug manifest**

`git mv` the activity and layout into `app/src/debug/...`. Create `app/src/debug/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity
            android:name=".TestFieldsActivity"
            android:exported="false"
            android:label="@string/open_test_fields"
            android:windowSoftInputMode="adjustResize" />
    </application>
</manifest>
```

Delete the `<activity android:name=".TestFieldsActivity" .../>` block from `app/src/main/AndroidManifest.xml`. Strings stay in main `strings.xml` (one debug-only string block in the release resource table is acceptable; no functional surface).

- [ ] **Step 2: De-reference the class from main sources**

`MainActivity` must not name the class directly (release compile would fail). Replace the intent construction inside the `BuildConfig.DEBUG` block with:

```kotlin
val intent = Intent().setComponent(
    android.content.ComponentName(
        packageName,
        "org.ubaierbhat.android.barcodekeyboard.TestFieldsActivity",
    ),
)
try {
    startActivity(intent)
} catch (e: android.content.ActivityNotFoundException) {
}
```

(Adapt to the existing click-handler body; keep `BuildConfig.DEBUG` guard.)

- [ ] **Step 3: Prove the release artifact is clean**

```bash
./gradlew assembleRelease -q
SDK=$(grep sdk.dir local.properties | cut -d= -f2)
"$SDK"/cmdline-tools/*/bin/apkanalyzer dex packages \
  app/build/outputs/apk/release/app-release-unsigned.apk | grep -ci testfields || echo CLEAN
```

Expected: `CLEAN` (0 matches). If apkanalyzer is absent, fall back to: `unzip -p app-release.apk classes*.dex | strings | grep -c TestFieldsActivity` → `0`.

- [ ] **Step 4: Full build + tests + debug device check**

`./gradlew testDebugUnitTest assembleDebug -q` green; install debug per Task 1 Step 3 commands; open field tester from MainActivity button; keyboard works in the phone field.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "build: keep the field tester in debug builds only"
```

---

### Task 3: Release build — R8, resource shrink, upload-key signing, gitignore

**Files:**
- Modify: `app/build.gradle.kts` (buildTypes + signingConfigs), `app/proguard-rules.pro` (empty today), `.gitignore`
- Create (untracked, user-local): `keystore.properties`, `upload-keystore.jks`

**Interfaces:**
- Produces: `bundleRelease` output `app/build/outputs/bundle/release/app-release.aab` — the exact artifact uploaded to Play; signed debug-equivalent release APK for on-device QA.
- Consumes: nothing.

- [ ] **Step 1: Harden .gitignore first (never risk a secret commit)**

Append:

```
*.jks
*.keystore
keystore.properties
```

- [ ] **Step 2: Generate the upload key (user machine; document, do not commit)**

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then `keystore.properties`:

```properties
storeFile=../upload-keystore.jks
storePassword=<from keytool>
keyAlias=upload
keyPassword=<from keytool>
```

If the device has no headless keytool issue, generate into the project root (gitignored by Step 1).

- [ ] **Step 3: Wire signing + R8**

In `app/build.gradle.kts`:

```kotlin
import java.util.Properties

val signingProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use(::load)
}
```

Inside `android { }`, above `buildTypes`:

```kotlin
signingConfigs {
    if (signingProps.isNotEmpty()) {
        create("upload") {
            storeFile = rootProject.file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }
}
```

Release block becomes:

```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    signingConfigs.findByName("upload")?.let { signingConfig = it }
}
```

- [ ] **Step 4: R8 rules**

`app/proguard-rules.pro`:

```
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
-keep class org.ubaierbhat.android.barcodekeyboard.service.BarcodeKeyboardService { <init>(); }
```

(The `-keep` is required: the IME is instantiated reflectively by the system; CameraX/ML Kit/AppCompat ship consumer rules — add none for them.)

- [ ] **Step 5: Build everything, install the signed release, re-run the matrix**

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease bundleRelease -q
# uninstall first: release (upload key) and debug (debug key) signatures differ;
# install -r would fail with INSTALL_FAILED_UPDATE_INCOMPATIBLE
adb -s R58RB1N07TD uninstall org.ubaierbhat.android.barcodekeyboard
adb -s R58RB1N07TD install app/build/outputs/apk/release/app-release.apk
# ime enable + ime set per Global Constraints
```

Because release builds strip the field tester, QA the keyboard inside a real notes app or dialer instead: type letters, open the phone dialpad field, scan `docs/acceptance/qr-SCAN-T9-QR-741258.png`, reopen keyboard → history shows the scan, gear opens MainActivity. R8 must not break reflection: confirm no crash in `adb logcat -d | grep -i barcodekeyboard` and the scanner still decodes.

- [ ] **Step 6: Record sizes + commit**

Note APK/AAB sizes (report them to the user; minify should cut the APK materially vs the debug build).

```bash
git add -A && git commit -m "build: release R8 with shrink, upload-key signing, keystore ignores"
```

---

### Task 4: GitLab Pages site — privacy policy + third-party licenses

**Files:**
- Create: `public/privacy.html`, `public/licenses.html`, `public/index.html`, `.gitlab-ci.yml`

**Interfaces:**
- Produces: `https://<namespace>.gitlab.io/scanner-keyboard/privacy.html` — the URL pasted into Play Console (Privacy Policy field, both store listing and Data safety); `licenses.html` satisfies ML Kit/Apache attribution links referenced from README §Licenses.
- Consumes: user's GitLab namespace (ask before filling absolute links; relative links inside the site regardless).

- [ ] **Step 1: Ask the user for the namespace + contact email** (question tool)

- [ ] **Step 2: `public/privacy.html`** — plain HTML, no JS, no tracking, STE-100 sentences. Required content (all facts below are verifiable in this repo):

```
Privacy Policy — Scanner Keyboard · Last updated: 2026-10-04

What the app does
Scanner Keyboard is an on-screen keyboard. It scans barcodes and QR codes.
It puts the scanned text into the text field you use.

Data the app collects
None. The app does not collect data about you.

Data the app stores on your device
- The last 20 scanned or copied texts (scan history).
- Text you copy while this keyboard is active. The app saves it so you can re-use it.
- Two settings: camera permission state.
This data never leaves your device. You can clear the history in the app at any time.
Uninstalling the app deletes it. The app blocks cloud and device-to-device backup
(AndroidManifest.xml: allowBackup="false").

Network access
The app has no INTERNET permission. Android itself cannot let the app send data.

Camera
The camera runs only while the scanner view is on screen. It releases the camera when
the scanner closes or the keyboard hides. Camera images stay in memory. The app never
saves a picture. Code reading happens on the device with Google ML Kit's bundled model.

Children
The app is not directed to children and collects nothing.

Contact
Email <USER CONTACT EMAIL>. You can also open an issue at <GITLAB PROJECT URL>.
Changes: we will post the new policy here and show the new date above.
```

- [ ] **Step 3: `public/licenses.html`** — project Apache-2.0 (link `LICENSE` raw URL) plus a table; each row: artifact · version · license · link.

| Artifact | Version | License |
|---|---|---|
| androidx.core:core-ktx | 1.18.0 | Apache-2.0 |
| androidx.appcompat:appcompat | 1.8.0 | Apache-2.0 |
| com.google.android.material:material | 1.14.0 | Apache-2.0 |
| androidx.camera:* (core, camera2, lifecycle, view) | 1.6.2 | Apache-2.0 |
| com.google.mlkit:barcode-scanning | 17.3.0 | code Apache-2.0; model governed by [ML Kit Terms of Use](https://developers.google.com/ml-kit/terms) |
| org.jetbrains.kotlin:kotlin-stdlib | (via AGP 9.0.1) | Apache-2.0 |
| Test-only: junit 4.13.2 (EPL-1.0), robolectric 4.17 (MIT), androidx.test:core 1.7.0 (Apache-2.0) | | |

- [ ] **Step 4: `public/index.html`** — five lines: app name, what it is, links to privacy + licenses + repo. And `.gitlab-ci.yml`:

```yaml
stages:
  - deploy

pages:
  stage: deploy
  script:
    - echo "Publishing static site from public/"
  artifacts:
    paths:
      - public
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH
```

(No Android CI job: runners lack the SDK; unit tests stay local/pre-push. Say so in a README CI note.)

- [ ] **Step 5: Validate and commit**

Open both HTML files locally (they must render offline, contain zero external `<script>`, and every relative link resolves). 

```bash
git add public .gitlab-ci.yml && git commit -m "docs: privacy policy, license list and GitLab Pages site"
```

---

### Task 5: Play Console declaration sheet

**Files:**
- Create: `docs/play/console-checklist.md`

**Interfaces:**
- Consumes: privacy URL from Task 4, AAB from Task 3.
- Produces: verbatim answers for every Console form so the human session is copy-paste.

- [ ] **Step 1: Write the sheet** — one section per Console form, exact STE wording:
  - App access: "This is a keyboard IME. Testers must enable it in system settings; no login." (Play reviewer can enable IME via Settings > System > Languages > On-screen keyboard.)
  - Ads: no ads.
  - Target audience & content: everyone; no UGC, no chat.
  - IARC questionnaire suggested answers with one-line justification each (no violence/sexual/ gambling etc.; camera = utility).
  - Data safety form: "No data collected / no data shared" with the repo evidence pointers (no INTERNET permission, MODE_PRIVATE prefs, allowBackup=false); ML Kit/CameraX/AndroidX SDK declarations each "does not share data".
  - Sensitive permission justification text for CAMERA (reuse README Privacy bullets; state on-device, no persistence, user-initiated).
  - Store listing: title ≤30 chars "Scanner Keyboard"; short description ≤80 ("Offline barcode and QR scanner built into your keyboard."); full description = README Features+Privacy in STE; screenshot list = `docs/images/keyboard.png`, `phone-dialpad.png`, `scanner.png`, `history-panel.png`, `settings-key.png` (phone frame, ≥2 required); icon = `docs/store/icon-512.png`; feature graphic optional.
  - Testing: personal account → closed test, email list ≥12 testers, 14 continuous opted-in days, then Apply for production (review ≤7 days). Internal track recommended for the same build first.
  - Upload: `bundleRelease` AAB, Play App Signing enroll with upload key from Task 3.
- [ ] **Step 2: Commit**

```bash
git add docs/play && git commit -m "docs: Play Console declaration and store-listing checklist"
```

---

### Task 6: Repository presentation — README, community files, templates, tag

**Files:**
- Modify: `README.md`
- Create: `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md`, `.gitlab/issue_templates/Bug.md`, `.gitlab/merge_request_templates/Default.md`

**Interfaces:**
- Consumes: GitLab URL (Task 4 Step 1 answer), Pages URLs.
- Produces: first impression a reviewer needs for a security-sensitive app (an IME).

- [ ] **Step 1: README rework** (keep current structure — it's good; add/replace):
  - Top badge line: `![License](https://img.shields.io/badge/license-Apache--2.0-green)` · `![minSdk](https://img.shields.io/badge/Android-7.0%2B-blue)` · `![network](https://img.shields.io/badge/network-none-brightgreen)` · pipeline badge `<gitlab url>/badges/main/pipeline.svg`.
  - Screenshot strip from `docs/images/` (keyboard, phone-dialpad, scanner, history) after the intro.
  - New **Project layout** section: `app/src/{main,test,debug}`, `docs/` (architecture.html, acceptance, play, security), `public/` (pages site), `tools/` (icon generator).
  - New **Contributing / Security / Changelog** links to the three new files; **Documentation** line → architecture.html + privacy URL.
  - STE pass over whole file: split any sentence >20 words; commands stay exact.
- [ ] **Step 2: CONTRIBUTING.md** — setup (JDK 17, SDK 36, local.properties), run tests, "no code comments; strings in XML", style: device QA required for keyboard changes (link the XCover5 IME re-select rule), ASD-STE100 rule for docs, PR/commit-message conventions matching the repo log.
- [ ] **Step 3: SECURITY.md** — this app sees every keystroke and clipboard copy of the user: state threat model in 6 bullet lines (data never leaves device; history cleared in-app), report by email, 90-day disclosure, no public issue for vulnerabilities.
- [ ] **Step 4: CHANGELOG.md** (Keep a Changelog, ISO dates) + templates: Bug template (steps / expected / actual / device + Android version + IME screenshot); MR template (checklist: tests green, device QA done, STE for docs).
- [ ] **Step 5: Commit** `git add -A && git commit -m "docs: community files and README presentation pass"`

---

### Task 7: OWASP MASVS v2.1 assessment record

**Files:**
- Create: `docs/security/masvs-compliance.md`

**Interfaces:**
- Consumes: final release artifact properties from Tasks 1–3.
- Produces: the compliance table the user asked for; link it from README Security section.

- [ ] **Step 1: Verify the three live probes before writing verdicts**

```bash
aapt2 dump badging app/build/outputs/apk/release/app-release.apk | grep -E "uses-permission|uses-feature"
grep -rn "Intent(" app/src/main/java | grep -v "MainActivity\|ComponentName" 
adb -s R58RB1N07TD shell dumpsys package org.ubaierbhat.android.barcodekeyboard | grep -A2 "Activity"
```

Expected: only CAMERA permission; no outbound intents beyond the settings launch already reviewed; system sees 2 activities, 1 service.

- [ ] **Step 2: Write the assessment** — table per applicable family, clause id · verdict · evidence path:

```
MASVS-STORAGE-1  PASS  app-private MODE_PRIVATE prefs only; ScanHistoryStore.kt:10-11
MASVS-STORAGE-2  PASS  allowBackup=false (AndroidManifest.xml:11); no auto-restore/adb-backup path
MASVS-STORAGE-3  PASS  history + clipboard never leave the device (no network, no export code)
MASVS-STORAGE-4  INFO  history is plaintext on purpose: re-enterable public codes (barcodes/URLs);
                       not account credentials; encryption would add key mgmt with no threat gain
                       (attacker with root reads keystrokes anyway — see PLATFORM-6 note)
MASVS-AUTH/SESSION — N/A  no accounts, no tokens
MASVS-PLATFORM-1 PASS  only exported components: launcher MainActivity + IME service gated by
                       BIND_INPUT_METHOD (AndroidManifest.xml:33-44); no receivers/providers;
                       release APK carries no debug activity (apkanalyzer-verified, Task 2)
MASVS-PLATFORM-2 PASS  camera released on hide/finish/switch (onFinishInputView, onWindowHidden)
MASVS-PLATFORM-3 PASS  no WebView/JS/Deep links, no file:// URIs
MASVS-PLATFORM-4 PASS  no root/native exec; stdlib JVM runtime only
MASVS-CODE-2     PASS  deps current at build (libs.versions.toml); wrapper pinned; no binaries in repo
MASVS-CODE-3     PASS  release = R8-minified, shrinkResources, non-debuggable, uploaded via Play App
                       Signing; debug-only surface separated by source set
MASVS-RESILIENCE L2 not adopted (out of scope for an offline FOSS utility; no high-risk backend)
MASVS-PRIVACY-1  PASS  clipboard/history capture is user-beneficial, visible in history UI,
                       one-tap clearable; documented in privacy policy (public/privacy.html)
MASVS-PRIVACY-2  PASS  camera active only while preview on screen; LED release verified on XCover5
Header notes: IME = trusted-component threat class; keystrokes are never stored or analyzed
(beyond composing buffers in memory); v1.0.0 assessed 2026-10-04.
```

- [ ] **Step 3: Cross-link from README and SECURITY.md; commit**

```bash
git add docs/security README.md && git commit -m "docs: OWASP MASVS v2.1 compliance record"
```

---

### Task 8: Ship — remote, push, Pages activation, v1.0.0 tag, release artifacts

**Files:**
- Modify: `.git/config` (remote), tags

- [ ] **Step 1: Add the remote and push** (URL from Task 4 Step 1)

```bash
git remote add origin <GITLAB URL> && git push -u origin main
```

- [ ] **Step 2: Confirm Pages job ran** — pipelines page shows `pages` success; open the privacy URL; `curl -s <pages url>/privacy.html | head -3` returns markup (proves anonymous reachability — Pages access control must stay OFF).
- [ ] **Step 3: Tag the release**

```bash
git tag -a v1.0.0 -m "Scanner Keyboard 1.0.0" && git push origin v1.0.0
```

- [ ] **Step 4: Report artifacts to the user**: AAB path + size (`app/build/outputs/bundle/release/app-release.aab`), signed release APK, pages URLs, and the remaining human steps from `docs/play/console-checklist.md` (create dev account, closed-test with 12 testers / 14 days, upload AAB).

---

## Self-review notes

- Coverage: blockers (T1 API36, T2 debug surface, T3 R8/signing), requirements docs (T4 privacy/licenses/Pages, T5 console sheet), presentation (T6), MASVS (T7), ship (T8). Play's 12-testers/14-days and Play App Signing are process steps → captured in T5/T8 with exact wording.
- ML Kit term used correctly (code Apache-2.0 + model under ML Kit ToS) in both T3? no — T4 row and README unchanged claim; consistent.
- `apkanalyzer` grep pattern `testfields` (case-insensitive) — dex class names use full package path `.../TestFieldsActivity`; check `TestFieldsActivity` exact token in Step 3 command to avoid a false CLEAN. (Implementer note.)
