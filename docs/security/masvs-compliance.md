# OWASP MASVS v2.1 Compliance Record — Scanner Keyboard

| | |
|---|---|
| App | Scanner Keyboard (`org.ubaierbhat.android.barcodekeyboard`) |
| Version assessed | 1.0.0 (versionCode 1), repo commit `1a573bd` |
| Artifact assessed | `app/build/outputs/apk/release/app-release.apk` — R8-minified, resource-shrunk, signed with the upload key (`apksigner verify`: `CN=Scanner Keyboard`, per task-3 report) |
| Date | 2026-10-04 |
| Assessor | Maintainer, AI-assisted static audit (no third-party pen test) |
| Framework | OWASP MASVS v2.1 |
| Level target | MASVS-L1 (Essential Security). L2 is deliberately not adopted: this is an offline, FOSS, account-free utility whose trust story is source auditability and reproducible builds, not reverse-engineering resistance against a targeted attacker. |

**ID normalization note.** The release-prep draft table used provisional control IDs
(`STORAGE-3`, `STORAGE-4`, `PLATFORM-4`, etc.) that do not exist in MASVS v2.1. Every
draft topic is retained below, renumbered to the canonical v2.1 controls
(verification: mas.owasp.org, repo tag `v2.1.0`), with the draft reference in
parentheses. The draft's Step-1 expectation "only `CAMERA`, no `INTERNET` line" was
**not confirmed** — see the PRIVACY-1 row and Finding 5.

## Scope and app model

Scanner Keyboard is an input method editor (IME). An IME is a **trusted component**:
while selected, it can see every keystroke it types and the primary clipboard at the
moment the keyboard opens — this is inherent to the platform role, identical for all
third-party keyboards (see `SECURITY.md`). The app's own code contains **no networking
API usage** (Appendix A2) and no dynamic/native code execution (A6); barcode decoding
runs fully on-device with the ML Kit *bundled* model. The only data the app keeps is
the scan/copy history (max 20 entries) in app-private storage. One caveat against a
clean "OS-enforced offline" claim: the **packaged** APK inherits `INTERNET` and
`ACCESS_NETWORK_STATE` from third-party library manifests even though no app code uses
them (Finding 5, open at assessment time).

## Verdict table

Verdicts: **PASS** — control met, evidence below · **INFO** — deliberate, documented
deviation or risk-acceptance · **N/A** — control has no subject matter in this app ·
**OPEN** — finding requiring action before sign-off.

| Control (draft ref) | Verdict | Evidence (file:line) |
|---|---|---|
| MASVS-STORAGE-1 — the app securely stores sensitive data (draft STORAGE-1) | PASS | Sole persistence mechanism: app-private `SharedPreferences` with `MODE_PRIVATE` — `ScanHistoryStore.kt:11`. No external storage, no plaintext files, no DB (A5). |
| MASVS-STORAGE-1, encryption-at-rest sub-point (draft STORAGE-4) | INFO | History (scans + clipboard captures) is stored **plaintext, deliberately**. Threat model: entries are re-enterable public codes (barcode values, URLs, copied text) the user can regenerate by scanning again — not account credentials. Encryption at rest needs a KeyStore key the app itself always holds, so an attacker able to read the file (root, or `adb` as a rooted/`run-as`-able debuggable build) is equally able to read it through the app's own APIs; encryption would add key management without removing a realistic threat. Residual risk, stated plainly: **adb backup is closed** (`allowBackup=false`), but **root/`run-as` still reads `shared_prefs/scan_history.xml` verbatim** on an attacker-controlled device. Mitigations: one-tap in-app CLEAR (`BarcodeKeyboardService.kt:283-300`), uninstall wipe, FOSS auditability. |
| MASVS-STORAGE-2 — the app prevents leakage of sensitive data (draft STORAGE-2 + STORAGE-3) | PASS | `android:allowBackup="false"` (`AndroidManifest.xml:11`) — no cloud auto-restore, no `adb backup` path. No export/share/sync/upload code path exists (A2). Keystrokes never persist (Finding 1). Logging: exactly three `Log.e` call sites, fixed error strings only, no data payloads (`ScannerView.kt:96,99,149`); v/d/i logging is compile-time stripped as future insurance (`proguard-rules.pro:1-5`). |
| MASVS-AUTH-1..3 — authentication & authorization (draft AUTH/SESSION) | N/A | No accounts, tokens, sessions, or privileged app functionality. Camera access uses the standard Android runtime-permission flow only (`MainActivity.kt:55-66,119-126`). |
| MASVS-NETWORK-1..2 — secure network communication (added: mandatory given Finding 5) | N/A | No network communication is initiated by app code (A2: zero matches for http/socket/url APIs). Packaging caveat: the inherited `INTERNET` permission is assessed under PRIVACY-1 below, not as network *usage*. |
| MASVS-PLATFORM-1 — the app uses IPC mechanisms securely (draft PLATFORM-1) | PASS | App-authored exported components: exactly two — `MainActivity` (MAIN/LAUNCHER only, `AndroidManifest.xml:18-25`) and the IME service, reachable only by the system via `android:permission="android.permission.BIND_INPUT_METHOD"` (`AndroidManifest.xml:27-38`). The only other exported entry is the library `androidx.profileinstaller.ProfileInstallReceiver`, gated by the system-held `android.permission.DUMP` (see below). The release APK carries no field-tester activity: merged release manifest `TestFieldsActivity` count **0** (A4; task-2 dex/`resources.arsc` checks). Device `dumpsys package` (A8; captured while the debug variant was installed — release differs only by the non-exported debug activity): 1 exported launcher activity, 1 IME service, and library-supplied components only — `androidx.startup.InitializationProvider` + `MlKitInitProvider` (`exported=false`, A4), `GoogleApiActivity` (`exported=false`), `ProfileInstallReceiver` (exported, `DUMP`-gated). No app-defined receivers/providers. No data-scheme intent-filters → no incoming deep links (A6: `<data` count 0). No `PendingIntent`, no `registerReceiver`, no `file://` URI handoffs (A6). Outbound intents — full inventory, 4 total: (1) `MainActivity.kt:42-51` explicit `ComponentName` to the debug-only field tester, `BuildConfig.DEBUG`-guarded, `try/catch ActivityNotFoundException`; (2) `MainActivity.kt:68` `Settings.ACTION_INPUT_METHOD_SETTINGS` (implicit, system settings); (3) `MainActivity.kt:129-133` `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` with own package URI + `NEW_TASK` (system settings, used after permanent camera denial); (4) `BarcodeKeyboardService.kt:353-357` explicit `Intent(this, MainActivity::class)` + `NEW_TASK or REORDER_TO_FRONT` (the toolbar gear). No intent carries user data extras. |
| MASVS-PLATFORM-2 — the app uses WebViews securely (draft PLATFORM-3) | PASS | No WebView anywhere in `app/src/main` (A6). No JS bridges, no `file://` URIs, no URL loading. |
| MASVS-PLATFORM-3 — the app uses the user interface securely (added row) | PASS | History entries are the user's own captured codes, rendered inside the IME overlay on explicit user tap (`BarcodeKeyboardService.kt:255-276`); no notifications, no share sheets, no secondary displays. `FLAG_SECURE` is not set — accepted: nothing shown meets the "sensitive data" bar beyond what the user just scanned/copied on their own screen. |
| MASVS-CODE-1 — the app requires an up-to-date platform version (added row) | INFO | `minSdk = 24` (Android 7.0, 2016) is a deliberate reach decision, not a security claim; `targetSdk = 36` keeps the app on the current Play behavior baseline (`build.gradle.kts:17-18`). L1 platform-currency expectation partially met via targetSdk only; documented trade-off. |
| MASVS-CODE-2 — mechanism for enforcing app updates (added row) | N/A | Distribution is Play-managed (and self-build for the audit crowd); no in-app update channels exist, so no update-enforcement surface to secure. |
| MASVS-CODE-3 — only components without known vulnerabilities (draft CODE-2 + CODE-3 + PLATFORM-4) | PASS | Dependencies catalog-pinned and current at build time: AGP 9.0.1, CameraX 1.6.2, ML Kit barcode 17.3.0 (bundled), appcompat 1.8.0, core 1.18.0, Material 1.14.0 (`gradle/libs.versions.toml`). Wrapper pinned (`gradle-9.1.0-bin.zip`, `gradle/wrapper/gradle-wrapper.properties`). Only tracked binary in the repo is the Gradle wrapper jar itself (A6); no native libs, no `Runtime.exec`/`ProcessBuilder`/`DexClassLoader` (A6). Release build is R8-minified + `shrinkResources`, non-debuggable, signed (`build.gradle.kts:39-44`; signature + 58-test evidence in task-3 report). Library supply-chain caveat (ML Kit transport/media3 manifests) is Finding 5. |
| MASVS-RESILIENCE-1..4 — reverse-engineering & tampering resistance (draft RESILIENCE) | INFO — not adopted, deliberate | The entire family is L2-only; not adopted for an offline FOSS utility (header rationale). Honesty note: R8 name-minification is on (`build.gradle.kts:40`) as a by-product of the standard release pipeline, and it also carries the ML Kit `ComponentRegistrar` keep rule that fixed a real R8 breakage (`proguard-rules.pro:8-14`, task-3 report) — but no *intentional* anti-RE defenses (root/emulator/attestation checks) exist. `v/d/i` log stripping (`proguard-rules.pro:1-5`) is future-proofing; current code logs only 3 `Log.e` error paths. |
| MASVS-PRIVACY-1 — the app minimizes access to sensitive data and resources: **declared capabilities** (draft STORAGE-3 header claim "no network") | **OPEN** | Own manifest declares exactly one dangerous permission: `CAMERA` (`AndroidManifest.xml:4`; `uses-feature` not-required, `:6-8`). **But the merged release artifact also carries** `android.permission.INTERNET` (blame: `transport-backend-cct:2.3.3`, pulled by `com.google.mlkit:barcode-scanning` → `mlkit:common` → `datatransport`) and `android.permission.ACCESS_NETWORK_STATE` (blame: `androidx.media3:media3-common:1.9.0`, pulled via `camera-view` → `camera-video`), plus androidx.core's self-defined signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (A1, A4, A7). No app code uses these; the capability is nevertheless present, so "offline is OS-enforced" is **false as packaged**. Remediation decision required before upload — Finding 5. |
| MASVS-PRIVACY-1 — runtime minimization: camera & clipboard scope (draft PLATFORM-2 + PRIVACY-2) | PASS | Camera is requested only at the point of first scan use with a graceful inline denial path (`BarcodeKeyboardService.kt:144-153`, `MainActivity.kt:55-66`); while the scanner shows, frames are analyzed in-memory only — nothing is written to disk. Camera is bound only while the preview is on screen (`ScannerView.kt:72-106`) and released on every exit path: close ✕, error auto-dismiss, keyboard hide, input-view finish, rotation, service destroy — `ScannerView.stop()` (torch off, `unbindAll()`, lifecycle destroy, `ScannerView.kt:108-127`) called from `BarcodeKeyboardService.kt:86-90,167-178,249-253,180-192`. Device-verified on the XCover5: `dumpsys media.camera` `CONNECT device 0` during preview → `Active Camera Clients: []` after close (task-1 report, QA item 5; task-3 report, QA item 4). Clipboard: `captureClipboardEntry()` (`BarcodeKeyboardService.kt:309-330`) reads only the **first clip item** of the primary clip, via `coerceToText`, trimmed, rejects empty and **>500 chars** (`MAX_CLIPBOARD_ENTRY_LENGTH`, `:364`), **dedups against the newest entry** (`:321-323`), stores via `ScanHistoryStore` (`:324`); runs once per fresh keyboard session (`:83-85`), and swallows `SecurityException`/`IllegalStateException` rather than retrying. |
| MASVS-PRIVACY-2 — the app prevents identification of the user (added row) | PASS | No identifiers, analytics, advertising IDs, or fingerprinting in app code. The bundled telemetry transport that ships with ML Kit has no data to identify anyone with (offline app, Finding 5 covers its permission). |
| MASVS-PRIVACY-3 — the app is transparent about data collection and usage (draft PRIVACY-1, half) | PASS | Keystrokes are never stored (only scans and first-clip-item clipboard captures enter History, per the mechanism described above); this behavior is disclosed in the published privacy policy (`public/privacy.html`) and README Privacy section. **Consistency caveat:** those documents' "declares no INTERNET permission" sentence is inaccurate as packaged — see Finding 5 for the required wording/remediation decision. |
| MASVS-PRIVACY-4 — the app offers user control over their data (draft PRIVACY-1, half) | PASS | History/CLEAR one-tap wipe (`BarcodeKeyboardService.kt:283-300`); no server-side copy exists to request deletion of; uninstall removes app-private storage; backup extraction is blocked (`allowBackup=false`). |

## Findings & residual risks

1. **IME trust model (inherent).** While selected as the active keyboard, any text the
   user types passes through this app's process memory transiently. Nothing typed is
   ever persisted: keystrokes go only to `InputConnection.commitText/sendKeyEvent`
   (`BarcodeKeyboardService.kt:98-142`); the history store's `add()` has exactly two
   callers — `recordScan()` (`:240-242`) for decoded barcodes and `captureClipboardEntry()`
   (`:324`) for copies (A9). This is the same inherent risk as every third-party IME;
   the mitigation is FOSS auditability and self-build signature comparison
   (`SECURITY.md`).
2. **Clipboard capture is behavioral, not a toggle.** Every time the keyboard opens
   after a copy, the first clip item (≤500 chars) enters History. It is user-beneficial
   (phone numbers/codes copied elsewhere become one tap away), visible in the History
   panel, one-tap clearable, and documented in the privacy policy — but it *is*
   collection of user data; users with unusual clipboard habits should know. Flagged
   for the release checklist reviewer.
3. **Plaintext history in app-private storage — accepted.** Reasoning and residual
   risk (root/`run-as` reads the file; adb backup and auto-restore are closed) are in
   the STORAGE-1 INFO row above.
4. **Open manual checks before release sign-off.** (a) Decode-by-aim: insert-on-decode
   end-to-end needs a human aiming the rear camera at a code — automation cannot
   (task-3 report, concern 1; ML Kit init itself is proven working post-keep-rule).
   (b) Runtime QA ran on the API-34 XCover5; targetSdk 36 (Android 16) behavior is
   unverified on a real API-36 device (task-3 report, concern 2).
5. **OPEN — inherited `INTERNET`/`ACCESS_NETWORK_STATE` in the packaged APK.** The app
   declares neither, and no app code uses the network, but library manifests merge
   them in (A1/A4/A7). Consequences: the "cannot make network calls — platform
   enforced" sentence shipped in README (`README.md:26-27` and the network badge),
   `SECURITY.md:13-14`, `public/privacy.html:37`, and `docs/play/console-checklist.md`
   (lines 74, 99, 111, 157) is not accurate as packaged. Options for the maintainer
   before 1.0.0 upload: (i) add manifest-merger removal overrides for both permissions
   (`tools:node="remove"`), re-run the task-3 device QA (ML Kit bundled decoding has no
   runtime network dependency), or (ii) keep the artifact and reword the four documents
   to "no network code; inherited permission noted" — (i) is preferred because the
   stronger claim is the better privacy posture and the store-listing checklist already
   promises it. The four public documents are intentionally left unedited in this
   commit pending that decision.

## Method note

Static audit against the working tree at commit `1a573bd` plus the built release APK,
run 2026-10-04. No dynamic instrumentation, no pen test; device-behavior claims are
sourced from the recorded task-1/task-2/task-3 QA evidence. All Step-1/Step-2 probe
commands and raw outputs are pasted below verbatim as the audit trail.

## Appendix — probe outputs (2026-10-04)

### A1. Signed release APK badging (permissions/features)

```
$ SDK=$(grep sdk.dir local.properties | cut -d= -f2); echo "SDK=$SDK"
SDK=/Users/noone/Library/Android/sdk
$ "$SDK"/build-tools/36.0.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk | grep -E "package:|uses-permission|uses-feature"
package: name='org.ubaierbhat.android.barcodekeyboard' versionCode='1' versionName='1.0.0' platformBuildVersionName='16' platformBuildVersionCode='36' compileSdkVersion='36' compileSdkVersionCodename='16'
uses-permission: name='android.permission.CAMERA'
uses-permission: name='org.ubaierbhat.android.barcodekeyboard.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.INTERNET'
  uses-feature-not-required: name='android.hardware.camera'
  uses-feature: name='android.hardware.faketouch'
```

(`build-tools/*` globs two installed versions — 35.0.0 and 36.0.0 — so 36.0.0 is
pinned explicitly.)

Draft expectation "only CAMERA, no INTERNET" — **failed**; see Finding 5.
(`faketouch` is auto-added by the platform tooling, harmless.)

### A2. Network APIs / URI leakage in app source — none

```
$ grep -rniE "http|socket|urlconnection|okhttp|Retrofit|Websocket" app/src/main/java --include="*.kt"
(no output)
$ grep -rniE "file://|FileProvider|Uri\.parse|imagecapture" app/src/main --include="*.kt" --include="*.xml"
NO_MATCHES
```

No file-share URIs; camera is bound as Preview+ImageAnalysis only (no
`ImageCapture`), so frames are never written to disk (`ScannerView.kt:129-152`).

### A3. Full outbound-intent inventory

```
$ grep -rn "Intent(" app/src/main/java --include="*.kt"
app/src/main/java/org/ubaierbhat/android/barcodekeyboard/MainActivity.kt:42:                val intent = Intent().setComponent(
app/src/main/java/org/ubaierbhat/android/barcodekeyboard/MainActivity.kt:68:            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
app/src/main/java/org/ubaierbhat/android/barcodekeyboard/MainActivity.kt:129:        val intent = Intent(
app/src/main/java/org/ubaierbhat/android/barcodekeyboard/service/BarcodeKeyboardService.kt:353:        val intent = Intent(this, MainActivity::class.java)
```

Named in the PLATFORM-1 row. Permission flow uses `ActivityCompat.requestPermissions`
(`MainActivity.kt:60-64`) — not an intent; no `ACTION_MANAGE_APP_ALL_FILES_ACCESS` or
any other settings action exists in the tree.

### A4. Merged release manifest — component export flags & permission blame

```
$ grep -c TestFieldsActivity app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml
0
$ grep -A4 -E "android:name=\"(androidx.profileinstaller.ProfileInstallReceiver|androidx.startup.InitializationProvider|com.google.mlkit.common.internal.MlKitInitProvider|com.google.android.gms.common.api.GoogleApiActivity)\"" <merged release manifest>
            android:name="com.google.mlkit.common.internal.MlKitInitProvider"
            android:authorities="org.ubaierbhat.android.barcodekeyboard.mlkitinitprovider"
            android:exported="false"
            android:name="com.google.android.gms.common.api.GoogleApiActivity"
            android:exported="false"
            android:name="androidx.startup.InitializationProvider"
            android:authorities="org.ubaierbhat.android.barcodekeyboard.androidx-startup"
            android:exported="false" >
            android:name="androidx.profileinstaller.ProfileInstallReceiver"
            android:exported="true"
            android:permission="android.permission.DUMP" >
$ sed -n '11,23p' app/build/intermediates/manifest_merge_blame_file/release/processReleaseMainManifest/manifest-merger-blame-release-report.txt
11    <uses-permission android:name="android.permission.CAMERA" />
11-->/Users/noone/AiProjects/scanner-keyboard/app/src/main/AndroidManifest.xml:4:5-65
11-->/Users/noone/AiProjects/scanner-keyboard/app/src/main/AndroidManifest.xml:4:22-62
17    <permission
17-->[androidx.core:core:1.18.0] /Users/noone/.gradle/caches/9.1.0/transforms/babeedbed12e365599875f67b24adc22/transformed/core-1.18.0/AndroidManifest.xml:22:5-24:47
18        android:name="org.ubaierbhat.android.barcodekeyboard.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
18-->[androidx.core:core:1.18.0] /Users/noone/.gradle/caches/9.1.0/transforms/babeedbed12e365599875f67b24adc22/transformed/core-1.18.0/AndroidManifest.xml:23:9-81
19        android:protectionLevel="signature" />
19-->[androidx.core:core:1.18.0] /Users/noone/.gradle/caches/9.1.0/transforms/babeedbed12e365599875f67b24adc22/transformed/core-1.18.0/AndroidManifest.xml:24:9-44
21    <uses-permission android:name="org.ubaierbhat.android.barcodekeyboard.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
21-->[androidx.core:core:1.18.0] /Users/noone/.gradle/caches/9.1.0/transforms/babeedbed12e365599875f67b24adc22/transformed/core-1.18.0/AndroidManifest.xml:26:5-97
22    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
22-->[androidx.media3:media3-common:1.9.0] /Users/noone/.gradle/caches/9.1.0/transforms/0a34b40bd157ed081e07af8a37a68def/transformed/media3-common-1.9.0/AndroidManifest.xml:22:5-79
23    <uses-permission android:name="android.permission.INTERNET" />
23-->[com.google.android.datatransport:transport-backend-cct:2.3.3] /Users/noone/.gradle/caches/9.1.0/transforms/ec21175d435c50396c4f0ec42d25b443/transformed/transport-backend-cct-2.3.3/AndroidManifest.xml:26:5-67
```

(Excerpt of the blame report; repeated second attribution lines for the same entry
omitted. Full file: `app/build/intermediates/manifest_merge_blame_file/release/processReleaseMainManifest/manifest-merger-blame-release-report.txt`.)

### A5. Persistence surface

```
$ grep -rn "getSharedPreferences\|openFileOutput\|File(" app/src/main/java --include="*.kt"
app/src/main/java/org/ubaierbhat/android/barcodekeyboard/history/ScanHistoryStore.kt:11:        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
```

Single hit — one `MODE_PRIVATE` prefs store, internal storage only.

### A6. WebView / deep links / native exec / repo binaries

```
$ grep -rniE "webview|loadlibrary|dexclassloader|runtime\.getruntime|processbuilder|pendingintent|registerreceiver" app/src/main/java --include="*.kt"
NO_MATCHES
$ grep -c "<data" app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml
0
$ git ls-files | grep -iE "\.(so|dll|dex|jar|aar|apk)$"
gradle/wrapper/gradle-wrapper.jar
```

### A7. Dependency chains for the inherited permissions

```
$ ./gradlew -q app:dependencies --configuration releaseRuntimeClasspath | grep -B12 "androidx.media3:media3-common" | head -20
|    +--- androidx.camera:camera-core:1.6.2 (*)
|    +--- androidx.camera:camera-lifecycle:1.6.2 (*)
|    +--- androidx.camera:camera-video:1.6.2
|    |    +--- androidx.annotation:annotation:1.8.1 (*)
|    |    +--- androidx.annotation:annotation-experimental:1.4.1 (*)
|    |    +--- androidx.arch.core:core-common:2.2.0 (*)
|    |    +--- androidx.camera:camera-core:1.6.2 (*)
|    |    +--- androidx.concurrent:concurrent-futures:1.1.0 (*)
|    |    +--- androidx.concurrent:concurrent-futures-ktx:1.1.0 (*)
|    |    +--- androidx.core:core:1.1.0 -> 1.18.0 (*)
|    |    +--- androidx.media3:media3-container:1.9.0
|    |    |    +--- androidx.annotation:annotation:1.6.0 -> 1.8.1 (*)
|    |    |    \--- androidx.media3:media3-common:1.9.0
```

`camera-video` is on the classpath via `androidx.camera:camera-view` (declared in
`app/build.gradle.kts:76`).

```
$ ./gradlew -q app:dependencies --configuration releaseRuntimeClasspath | grep -E "transport|mlkit" | sort -u | head
+--- com.google.mlkit:barcode-scanning:17.3.0 (c)
+--- com.google.mlkit:common:18.11.0 (*)
+--- com.google.mlkit:vision-common:17.3.0 (*)
|    +--- com.google.android.datatransport:transport-api:2.2.1
|    +--- com.google.android.datatransport:transport-backend-cct:2.3.3
|    +--- com.google.android.datatransport:transport-runtime:2.2.6 (*)
|    +--- com.google.firebase:firebase-components:16.1.0
```

### A8. Device component & permission view (`dumpsys package`, R58RB1N07TD)

Captured while the **debug** variant was installed (`flags=[ DEBUGGABLE … ]`, API 34;
the default IME meanwhile reverted to Honeyboard). The release variant's component set
is identical except the non-exported debug activity, which the release merged manifest
does not contain (A4; task-2 dex/`resources.arsc` checks).

```
$ adb -s R58RB1N07TD shell dumpsys package org.ubaierbhat.android.barcodekeyboard
Activity Resolver Table:
  Non-Data Actions:
      android.intent.action.MAIN:
        fa79018 org.ubaierbhat.android.barcodekeyboard/.MainActivity filter 2cfda71
          Action: "android.intent.action.MAIN"
          Category: "android.intent.category.LAUNCHER"

Receiver Resolver Table:
  Non-Data Actions:
      androidx.profileinstaller.action.SAVE_PROFILE:
        b74a056 org.ubaierbhat.android.barcodekeyboard/androidx.profileinstaller.ProfileInstallReceiver filter 36acad
          Action: "androidx.profileinstaller.action.SAVE_PROFILE"
      androidx.profileinstaller.action.INSTALL_PROFILE:
        b74a056 org.ubaierbhat.android.barcodekeyboard/androidx.profileinstaller.ProfileInstallReceiver filter 206a8d7
          Action: "androidx.profileinstaller.action.INSTALL_PROFILE"
      androidx.profileinstaller.action.SKIP_FILE:
        b74a056 org.ubaierbhat.android.barcodekeyboard/androidx.profileinstaller.ProfileInstallReceiver filter 19195c4
          Action: "androidx.profileinstaller.action.SKIP_FILE"
      androidx.profileinstaller.action.BENCHMARK_OPERATION:
        b74a056 org.ubaierbhat.android.barcodekeyboard/androidx.profileinstaller.ProfileInstallReceiver filter ab26be2
          Action: "androidx.profileinstaller.action.BENCHMARK_OPERATION"

Service Resolver Table:
  Non-Data Actions:
      android.view.InputMethod:
        4f44a30 org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService filter bbe66a9 permission android.permission.BIND_INPUT_METHOD
          Action: "android.view.InputMethod"

Registered ContentProviders:
  org.ubaierbhat.android.barcodekeyboard/androidx.startup.InitializationProvider:
    Provider{e83d78e org.ubaierbhat.android.barcodekeyboard/androidx.startup.InitializationProvider}
  org.ubaierbhat.android.barcodekeyboard/com.google.mlkit.common.internal.MlKitInitProvider:
    Provider{fa43baf org.ubaierbhat.android.barcodekeyboard/com.google.mlkit.common.internal.MlKitInitProvider}

Domain verification status:
```

```
$ adb -s R58RB1N07TD shell dumpsys package org.ubaierbhat.android.barcodekeyboard | grep -E "versionCode|flags=|privateFlags=|android.permission.INTERNET" | head -8
flags=0x0
    versionCode=1 minSdk=24 targetSdk=36
    flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]
    privateFlags=[ PRIVATE_FLAG_ACTIVITIES_RESIZE_MODE_RESIZEABLE_VIA_SDK_VERSION ALLOW_AUDIO_PLAYBACK_CAPTURE PARTIALLY_DIRECT_BOOT_AWARE PRIVATE_FLAG_ALLOW_NATIVE_HEAP_POINTER_TAGGING ]
      android.permission.INTERNET
      android.permission.INTERNET: granted=true

$ adb -s R58RB1N07TD shell dumpsys package org.ubaierbhat.android.barcodekeyboard | grep -A6 "requested permissions"
    requested permissions:
      android.permission.CAMERA
      org.ubaierbhat.android.barcodekeyboard.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
      android.permission.ACCESS_NETWORK_STATE
      android.permission.INTERNET
    install permissions:
      android.permission.INTERNET: granted=true
```

Draft expectation "no receivers/providers" — **corrected**: one library receiver
(DUMP-permission gated) + two non-exported library providers ship in the merged
manifest; no app-defined ones. Draft expectation "only CAMERA, no INTERNET" —
**confirmed false at the artifact level** (requested-permissions block above matches
A1); see Finding 5.

### A9. Keystroke persistence audit

```
$ grep -rn "commitText\|sendKeyEvent\|setComposingText" app/src/main/java --include="*.kt" | grep -v BarcodeKeyboardService
(no output — all InputConnection use is confined to BarcodeKeyboardService)

$ grep -rn "historyStore\|\.add(" app/src/main/java --include="*.kt" | grep -v "updated.add\|array.put"
app/src/main/java/…/service/BarcodeKeyboardService.kt:45:    private var historyStore: ScanHistoryStore? = null
app/src/main/java/…/service/BarcodeKeyboardService.kt:55:        historyStore = ScanHistoryStore(this)
app/src/main/java/…/service/BarcodeKeyboardService.kt:190:        historyStore = null
app/src/main/java/…/service/BarcodeKeyboardService.kt:241:        historyStore?.add(text)
app/src/main/java/…/service/BarcodeKeyboardService.kt:270:        panel.refresh(historyStore?.entries().orEmpty())
app/src/main/java/…/service/BarcodeKeyboardService.kt:298:        historyStore?.clear()
app/src/main/java/…/service/BarcodeKeyboardService.kt:299:        historyPanelView?.refresh(historyStore?.entries().orEmpty())
app/src/main/java/…/service/BarcodeKeyboardService.kt:310:        val store = historyStore ?: return
app/src/main/java/…/service/BarcodeKeyboardService.kt:324:            store.add(text)
app/src/main/java/…/keyboard/KeyboardView.kt:201:            accentCandidates.add(candidate)
```

Typing paths (`onText`/`onBackspace`/`onEnter`, `BarcodeKeyboardService.kt:98-142`)
touch only `InputConnection`. The only other `.add(` in main is
`KeyboardView.kt:201 accentCandidates.add(...)` — an in-memory popup list.
