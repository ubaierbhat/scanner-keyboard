# Scanner Keyboard — Implementation Plan

Plan: docs/superpowers/plans/2026-09-27-scanner-keyboard-plan.md
Spec: docs/superpowers/specs/2026-09-27-scanner-keyboard-design.md
Project root: /Users/noone/AiProjects/scanner-keyboard (git repo, branch feature/scanner-keyboard-v1)

## Global Constraints

- Package/namespace/applicationId: org.ubaierbhat.android.barcodekeyboard
- UI: XML View system only — zero Compose code, deps, plugins, or feature flags anywhere
- Scanner engine: ML Kit com.google.mlkit:barcode-scanning, bundled variant only, Barcode.FORMAT_ALL_FORMATS, fully on-device/offline; no INTERNET permission anywhere
- Camera: CameraX with ImageAnalysis STRATEGY_KEEP_ONLY_LATEST; bindToLifecycle always receives the ScannerLifecycleOwner (the custom LifecycleOwner), never the service
- Detection behavior: commitText via null-checked InputConnection, View.performHapticFeedback (no Vibrator, no VIBRATE permission), then auto-close scanner and reset mode to KEYBOARD
- Camera is unbound/stopped in onFinishInputView, onWindowHidden, and onDestroy; mode resets to KEYBOARD whenever the IME re-shows; torch resets off on stop
- SDKs: minSdk 24, compileSdk 36, targetSdk 35, Kotlin, JDK 17 toolchain (AGP from the android CLI template)
- No code comments in committed source files
- Unit tests live in the app module's test source set; every task's finish state: full suite green via ./gradlew test plus ./gradlew assembleDebug
- Tooling: use the android CLI (scaffold/install/emulator/screenshots) and ./gradlew (build/tests); working directory is the project root
- Git: commit with concise conventional messages; never commit .superpowers/ or local.properties

## Task 1: Scaffold project, rename package, strip Compose, add dependencies, build green

1. Run: android create empty-activity --name "Scanner Keyboard" --minSdk 24 -o .
   From the project root /Users/noone/AiProjects/scanner-keyboard. If the CLI refuses a
   non-empty directory (docs/ exists) or creates a nested folder, create into a temporary
   sibling directory under /var/folders/cq/6phl_5g90v756w16mzhfp9vc0000gn/T/opencode and
   move the generated contents (except .git) into the project root.
2. Inspect the generated Gradle setup: settings.gradle.kts, app/build.gradle.kts (or
   .kts equivalent), version catalog if present, AGP and Kotlin versions, wrapper.
   Record the versions in your report.
3. Rename identity: set namespace and applicationId to org.ubaierbhat.android.barcodekeyboard
   in the app build file; move the template's source/resource dirs to
   app/src/main/java/org/ubaierbhat/android/barcodekeyboard/ (delete the old package dir);
   update any manifest/package references.
4. Set compileSdk 36, targetSdk 35, minSdk 24 (build-tools 36 and platform 36 are
   installed at /Users/noone/Library/Android/sdk).
5. Strip ALL Compose: remove Compose dependencies/BOM/plugins, compose compiler config,
   buildFeatures compose flags, and all Compose UI/theme source files. Convert
   MainActivity to a plain AppCompatActivity that inflates a new XML layout
   res/layout/activity_setup.xml containing only a centered placeholder TextView with
   text "Setup wizard — Task 4". Add appcompat and material themes to
   res/values/themes.xml (Theme.Material3.DayNight.NoActionBar or similar) and apply the
   theme to the application/manifest.
6. Add dependencies (resolve latest stable versions, e.g. via android studio
   version-lookup or Maven Central search; record chosen versions in the report):
   androidx.camera:camera-core, camera-camera2, camera-lifecycle, camera-view;
   com.google.mlkit:barcode-scanning (bundled); androidx.appcompat:appcompat;
   com.google.android.material:material; androidx.core:core-ktx.
   Add the robolectric plugin/deps for the unit test source set now so later tasks only
   write tests: org.robolectric:robolectric plus junit and androidx.test:core in
   testImplementation, with testOptions unitTests.includeAndroidResources true.
7. Keep the template's .gitignore (extend it if needed so local.properties and
   .superpowers/ are never tracked; .superpowers/sdd already carries a self-ignore).
8. Verify: ./gradlew assembleDebug succeeds with zero errors. If the template shipped
   unit tests that reference Compose, port or delete them so ./gradlew test is green.
9. Commit the scaffold as one or more clean commits.
10. Report: AGP/Kotlin/Gradle versions, all dependency versions chosen, final project
    tree (top two levels), any deviations and why.

## Task 2: IME skeleton — manifest, method.xml, service, basic QWERTY, text injection

Context: Task 1 produced a buildable app module at org.ubaierbhat.android.barcodekeyboard
with CameraX + ML Kit deps declared (unused until Tasks 5-6). This task creates the
keyboard service and a basic working QWERTY. Do not implement shift-state machine
refinements, auto-repeat backspace, enter-action dispatch, the symbols layer, accents,
or history — later tasks own those.

1. AndroidManifest.xml: add uses-permission android.permission.CAMERA; uses-feature
   android.hardware.camera with android:required false; declare the service
   org.ubaierbhat.android.barcodekeyboard.service.BarcodeKeyboardService with
   android:permission android.permission.BIND_INPUT_METHOD, android:exported true,
   intent-filter action android.view.InputMethod, and meta-data android.view.im pointing
   to @xml/method.
2. res/xml/method.xml: input-method element with
   android:settingsActivity="org.ubaierbhat.android.barcodekeyboard.MainActivity".
3. service/BarcodeKeyboardService.kt extends InputMethodService:
   - onCreateInputView inflates a container FrameLayout res/layout/input_view.xml that
     hosts the KeyboardView as its only child at this point (scanner and history views
     are added as siblings by Tasks 5 and 7; the container is the mode switch surface,
     children switched by visibility).
   - onStartInputView(EditorInfo, restarting): stash nothing yet beyond defaults; a
     later task refines enter behavior from EditorInfo.
   - Keyboard interaction contract: KeyboardView.setListener(KeyboardActionListener)
     with callbacks onText(String), onBackspace(), onEnter(), onScanRequested(),
     onHistoryRequested(). Service implementations: onText → currentInputConnection
     null-checked commitText(text, 1); onBackspace → deleteSurroundingText(1, 1) with a
     KeyEvent KEYCODE_DEL sendKeyEvent fallback if selection is active; onEnter →
     sendKeyChar('\n') as the basic behavior (Task 3 replaces it with EditorInfo-aware
     dispatch); onScanRequested/onHistoryRequested → no-op placeholders for now.
   - onFinishInputView and onDestroy: no camera exists yet; simply reset the keyboard
     layer to letters.
4. keyboard/KeyView.kt: a custom View (not AppCompatButton) that renders a centered
   text label with a rounded-rect background drawable having a pressed/checked state,
   key text size ~22sp, key height ~52dp (row heights fixed in XML, weights distribute
   width). It reports presses via a click callback; KeyboardView owns layout and
   listener wiring. Use View.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
   on key down.
5. keyboard/KeyboardView.kt: custom FrameLayout inflating res/layout/keyboard_view.xml
   with four rows (LinearLayouts of KeyViews):
   - Row 1: qwertyuiop; Row 2: asdfghjkl; Row 3: zxcvbnm + backspace key at row end;
     Row 4 (bottom): shift, ?123 (disabled/inert placeholder label "123" until Task 3
     wires it — still render it), SCAN, SPACE, ENTER. The HISTORY key is NOT in this
     task (Task 7 adds it between SCAN and SPACE).
   - Key labels: shift "⇧", backspace "⌫", scan "SCAN", space (no label, widest, weight
     ~3), enter "↵", placeholder "123".
   - Basic shift behavior for this task: tap shift toggles all letter labels between
     lower/upper case and letters commit the currently-cased letter; this is replaced
     by KeyboardState in Task 3. Keep it simple — no caps lock, no auto-off.
   - Row background dark theme-neutral: keyboard surface #1B1B1F with keys #2E2E33,
     key text white, special keys tinted accent #7CADF8 — values belong in
     res/values/colors.xml and drawable XML, never inline.
6. All user-visible strings (accessibility contentDescription for every key included)
   in res/values/strings.xml.
7. Verification: ./gradlew test assembleDebug green; then on-device: build the debug
   APK (./gradlew assembleDebug), install with android run --apks (or adb install),
   enable/set the IME via adb shell ime enable
   org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService and adb shell
   ime set org.ubaierbhat.android.barcodekeyboard/.service.BarcodeKeyboardService,
   focus any text field (e.g. Settings search), and capture an android screenshot
   showing the keyboard rendered; report the screenshot path. The AVD small_phone is
   available; boot with android emulator start small_phone.
8. Commit.

## Task 3: Keyboard completeness — shift/caps state, backspace repeat, enter dispatch, symbols layer

Context: Task 2 delivered the service + KeyboardView with placeholder shift/casing and
"123" key, enter via sendKeyChar, and no auto-repeat. This task makes those real. The
service contract KeyboardActionListener is unchanged; extend KeyboardView wiring only.

1. keyboard/KeyboardState.kt: pure Kotlin class — enum ShiftMode OFF, ON, LOCKED;
   methods: toggle() (OFF→ON→OFF), toggleLock() (sets LOCKED from any, or back to OFF
   when already LOCKED), applyTo(letter: String): String returning the cased letter,
   consumeLetter() resetting ON→OFF after one letter (LOCKED persists), isUppercase()
   for label rendering. KeyboardView delegates ALL casing to it — delete Task 2's
   placeholder shift logic.
2. Interaction: tap SHIFT → toggle(); long-press SHIFT → toggleLock(). Locked state
   shows a persistent pressed/checked visual on the shift key.
3. Backspace auto-repeat: long-press ⌫ on KeyView triggers repeat via Handler
   postDelayed — initial delay 400 ms, interval 50 ms — invoking onBackspace each tick,
   stopping on ACTION_UP/ACTION_CANCEL. Task 2's single-tap backspace remains.
4. Enter dispatch: extract pure resolver EnterActionResolver with
   resolve(editorInfo: EditorInfo): EnterBehavior returning PERFORM_ACTION(action id),
   NEWLINE, or SEND_KEY_EVENT: if (imeOptions and EditorInfo.IME_MASK_ACTION) is an
   action other than NONE/UNSPECIFIED → PERFORM_ACTION(action); else if inputType has
   TYPE_TEXT_FLAG_MULTI_LINE → NEWLINE; else SEND_KEY_EVENT. Service uses it in
   onStartInputView to store behavior; ENTER key invokes it: PERFORM_ACTION →
   currentInputConnection.performEditorAction(id); NEWLINE → sendKeyChar('\n');
   SEND_KEY_EVENT → sendKeyEvent with KEYCODE_ENTER.
5. Symbols layer: "123" key toggles KeyboardView between the letters layout and a
   numbers/symbols layout (two sibling ViewFlipper/stacked LinearLayouts inside
   keyboard_view.xml, switched by visibility): numbers row 1234567890; symbols rows
   with @ # $ % & - + ( ) and * " ' : ; , . ? ! / and a second-layer toggle key "#+="
   cycling to more symbols ( { } [ ] \ | ~ < > = °) with "123" returning; shift row and
   SCAN/SPACE/ENTER/⌫ persist across layers; "ABC" key returns to letters (replaces
   "123" position when on symbols). While on symbols layers, SHIFT is disabled
   (visually dimmed, inert).
6. Unit tests (test source set, plain JUnit — these are pure classes):
   KeyboardStateTest covering OFF→ON→OFF toggle, consumeLetter resets ON but not
   LOCKED, applyTo casing both directions, toggleLock cycles LOCKED→OFF;
   EnterActionResolverTest covering each EditorInfo shape (action search/done/next,
   multiline no-action, plain single-line) — build EditorInfo instances directly.
7. Verification: ./gradlew test assembleDebug green; reinstall on small_phone,
   screenshot letters + symbols layers, report paths.
8. Commit.

## Task 4: Setup wizard MainActivity (XML)

Context: Tasks 1-3 left activity_setup.xml a placeholder and MainActivity a bare
AppCompatActivity. This task builds the two-step setup wizard. No keyboard changes.

1. res/layout/activity_setup.xml: Material-friendly scroll layout: app title header;
   step 1 card (title, description, status chip, action button) for CAMERA permission;
   step 2 card for enabling the IME; an overall status line. All copy from
   strings.xml.
2. MainActivity.kt: onResume re-evaluates both statuses. Step 1 button →
   ActivityCompat.requestPermissions(CAMERA); onRequestPermissionsResult re-evaluates
   status. Step 2 button → startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)).
   Status logic: camera granted via ContextCompat.checkSelfPermission;
   IME enabled via InputMethodManager (getSystemService) getEnabledInputMethodList()
   containing our ComponentName. Overall status string "Scanner Keyboard ready" only
   when both pass, else the next actionable hint. Status chips colored via
   res/values/colors.xml (ok green / pending amber) and drawables.
3. Keep MainActivity as the exported launcher activity.
4. Verification: ./gradlew test assembleDebug green; install and drive on small_phone
   with adb shell input taps (grant flow: tap step 1 button, allow dialog via
   android screenshot verification; ime settings opens — verify via screenshot that
   the intent resolved); report screenshot paths for: fresh state, camera granted,
   both-complete (enable IME manually on device then re-launch activity if needed).
5. Commit.

## Task 5: Scanner core — ScannerLifecycleOwner, ScannerView, CameraX binding, torch, permission gate

Context: Service currently has an inert onScanRequested. This task adds camera
preview inside the IME window. ML Kit analysis is Task 6 — no ImageAnalysis yet.

1. scanner/ScannerLifecycleOwner.kt: implements LifecycleOwner with a LifecycleRegistry;
   start() moves to RESUMED (from INITIALIZED), destroy() → DESTROYED. Must be created
   fresh per scanner session; destroyed sessions are never reused.
2. scanner/ScannerView.kt: custom FrameLayout inflating res/layout/scanner_view.xml:
   PreviewView (match_parent), controls row overlaying bottom: CLOSE "✕" and TORCH
   buttons + hint text res string "Point at a barcode". Root height set in code: on the
   service adding it, LayoutParams.height = (displayMetrics.heightPixels * 0.45).
   toInt() so the IME window grows taller for aiming (the IME window height equals the
   visible input-view height).
3. Camera binding in ScannerView.start(context): ProcessCameraProvider.getInstance
   .addListener on main executor; on success create ScannerLifecycleOwner, then
   bind Preview.Builder().build() with preview.setSurfaceProvider(previewView
   .getSurfaceProvider()) via CameraSelector.DEFAULT_BACK_CAMERA to
   bindToLifecycle(lifecycleOwner, selector, preview); keep the returned Camera for
   torch. On failure (ExecutionException/InterruptedException or bind
   IllegalArgumentException) → error state: swap PreviewView visibility for an error
   TextView and post to the error callback.
4. stop(): cameraProvider.unbindAll(), lifecycleOwner.destroy(), torch off, reset
   views to clean state. Guard against double-stop.
5. Torch button: camera.cameraControl.enableTorch(on) with a pressed visual toggle;
   resets off in stop().
6. Public interface for the service: setCallbacks(onClose: () -> Unit,
   onError: (String) -> Unit); start(context)/stop(); the scanner's parent provides
   scan results wiring in Task 6 — design the view so an onBarcodeResult callback
   slot exists (added by Task 6, not now).
7. Service integration in BarcodeKeyboardService: onScanRequested → if
   ContextCompat.checkSelfPermission CAMERA is not granted, switch the container to a
   simple inline permission state (a small layout in input_view.xml with message +
   "Open setup" button → startActivity(Intent(this, MainActivity) with
   FLAG_ACTIVITY_NEW_TASK)); else add/swap to ScannerView, call start(this). CLOSE →
   stop + switch back to keyboard. onError → stop + switch back to keyboard (the error
   text also shows briefly in the scanner's error view before close, 1500 ms).
   - Lifecycle guards: onFinishInputView, onWindowHidden, onDestroy → scannerStop()
     if open, and mode reset to KEYBOARD (so the next show is always the keyboard).
     Add mode handling now as a private enum Mode in the service (KEYBOARD, SCANNER;
     HISTORY joins in Task 7).
8. Verification: ./gradlew test assembleDebug green; on small_phone: boot, install,
   enable/set IME, grant camera, open scanner via SCAN key, screenshot the live
   preview inside the IME window (report path); screenshot permission-denied inline
   state (revoke first via adb shell pm revoke ... android.permission.CAMERA, tap
   SCAN, report path); close returns to keyboard; verify privacy camera dot off
   after close (adb dumpsys media.camera | grep -i "no active"? or report from
   dumpsys camera | Devices/Clients — capture evidence in the report).
9. Commit.

## Task 6: ML Kit analyzer, auto-insert + auto-close, throttle, error fallback polish

Context: Task 5's ScannerView shows live preview with close/torch and an
onBarcodeResult callback slot reserved. This task adds detection and the full
detection flow.

1. scanner/ScanThrottle.kt: pure class, constructor takes nowMs: () -> Long (default
   System::currentTimeMillis). Method allow(): Boolean — first call true; subsequent
   calls false until 1000 ms elapsed since the last allowed call. Method reset().
2. scanner/BarcodeAnalyzer.kt: implements ImageAnalysis.Analyzer; holds the ML Kit
   BarcodeScanning client built with BarcodeScannerOptions.Builder()
   .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build(); analyze(imageProxy):
   mediaImage null → imageProxy.close() and return; else InputImage.fromMediaImage(
   mediaImage, imageProxy.imageInfo.rotationDegrees) → scanner.process(image)
   .addOnSuccessListener { barcodes → first barcode rawValue non-null → if
   throttle.allow() post result to main thread via the callback; else ignore }
   .addOnCompleteListener { imageProxy.close() }. Every code path closes the proxy
   exactly once. No isProcessing flag — STRATEGY_KEEP_ONLY_LATEST owns backpressure.
3. ScannerView.start: additionally build ImageAnalysis with
   setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST) and setAnalyzer on a single
   background executor (Executors.newSingleThreadExecutor, shutdown in stop()) with
   BarcodeAnalyzer { text → mainHandler.post { onBarcodeResult(text) } }; bind all
   three (preview + analysis) in one bindToLifecycle.
4. Full detection flow in the service: onBarcodeResult(text) → currentInputConnection
   null-checked commitText(text, 1) → performHapticFeedback(VIRTUAL_KEY) on the
   container view → (Task 7 will add history here; leave a marked single call point)
   → scannerStop + mode KEYBOARD.
5. Permission gate hardening: if the OS revokes camera mid-session the bind surfaces
   an error → onError path already returns to keyboard (Task 5); ensure the inline
   error text shows from res strings and never crashes.
6. Unit tests: ScanThrottleTest with a mutable fake clock — first allow true,
   second immediate false, true again after 1000 ms advance, reset() re-allows.
7. Verification: ./gradlew test assembleDebug green. On small_phone the default back
   camera is a virtual scene with no codes — attempt the camera route: check the AVD
   config (avdmanager or ~/.android/avd/small_phone.avd/config.ini) for
   hw.camera.back; if it can be set to webcam0 via emulator console (emu avd hostmicon
   is not it — use `adb emu` or stop emulator, edit config.ini hw.camera.back=webcam0,
   restart) then point the host webcam at a QR on screen and verify a full scan
   inserts text into a field; if webcam routing fails, verify the detection path on
   a physical device if one is attached (adb devices), else report the emulator
   limitation and rely on Task 9's verification — do not fake results.
8. Commit.

## Task 7: ScanHistoryStore, history panel, clipboard capture, HISTORY key

Context: Detection flow (Task 6) has a marked single call point for history.
This task adds persistence, the panel mode, and clipboard capture.

1. history/ScanHistoryStore.kt: constructor(context: Context); SharedPreferences
   "scan_history", key "entries", JSON array. add(entry: String): trim, ignore blank;
   move-to-front on duplicate; cap at 20 (oldest dropped); persist synchronously.
   entries(): List<String> read-back; clear(). Handle JSONException by resetting to
   an empty store (corrupt storage never crashes the IME).
2. Service: instantiate store onCreate; Task 6's marked call point → store.add(text)
   before closing. Add Mode.HISTORY and a HISTORY key to the KeyboardView bottom row
   between SCAN and SPACE (label "⌚"? no — use text label "HIST"; contentDescription
   from strings) wired to onHistoryRequested → switch container to HistoryPanelView.
3. history/HistoryPanelView.kt: custom FrameLayout inflating
   res/layout/history_panel_view.xml: header row (title, CLEAR, CLOSE buttons) +
   RecyclerView (ListAdapter with DiffUtil) of entries, each an expandable single
   item showing up to 2 lines ellipsized; tap → callback inserts entry. Empty state
   TextView "No scanned or copied items yet". Panel height: same taller treatment as
   scanner (~45% screen) for readable list.
   Interface: setCallbacks(onEntrySelected: (String) -> Unit, onClose: () -> Unit);
   fun refresh(store entries). Service: onEntrySelected → commitText + mode KEYBOARD;
   onClose/CLEAR → mode KEYBOARD / store.clear + refresh.
4. Clipboard capture: in onStartInputView (when shown, not restarting) —
   ClipboardManager primaryClip, coerceToText, toString; accept only if non-blank,
   length ≤ 500, differs from store's newest entry; wrap in try/catch
   SecurityException/DropBoxManager-not-allowed style failures → silently skip (this
   is expected when not the default IME). add() to store.
5. Mode resets: any mode switch away from KEYBOARD/panel closes scanner/panel state
   cleanly; IME re-show always lands on KEYBOARD (scanner/panel never persist across
   hide).
6. Unit tests (Robolectric where Android classes needed): ScanHistoryStoreTest —
   add/persist round-trip across two instances, dedup moves newest to front, cap 20
   drops oldest, clear works, corrupt JSON resets safely, blank entries ignored.
7. Verification: ./gradlew test assembleDebug green; on-device screenshots: history
   panel empty state and populated state (scan or clipboard first — adb shell input
   text into a field copies nothing; use adb shell am broadcast? simplest: copy via
   adb shell cmd clipboard? not on all APIs — use the store directly through a scan
   or set a clip via the host: report which path worked), tap-to-insert works.
8. Commit.

## Task 8: Long-press accent popups

Context: KeyboardView + KeyView from Tasks 2-3 are stable. This task adds accent
selection on long-press. No service changes.

1. keyboard/AccentMap.kt: pure object with variants(letter: Char): List<Char> covering
   a à á â ä æ ã å ā; e è é ê ë ė ē; i ì í î ï ī; o ò ó ô ö õ ø ō œ; u ù ú û ü ū; c ç
   ć č; n ñ ń; s ß ś š; y ÿ ý; z ź ž; d ď đ; l ł ľ; r ŕ ř; t ť ŧ; g ĝ ğ; h ĥ; j ĵ;
   empty for others.
2. Long-press on a letter KeyView (Handler postDelayed ~350 ms, canceled on up/move)
   → KeyboardView shows a PopupWindow anchored above the key: a horizontal strip of
   mini KeyViews with the base letter first then variants.
3. Touch behavior: opening on long-press; dragging within the popup highlights the
   candidate under the finger (follow both x and y); release over a candidate inserts
   it (apply KeyboardState casing — uppercase if ON/LOCKED); release over the origin
   key (or a tap without drag) inserts the base letter; release elsewhere cancels.
   A drag slop (~8 px) distinguishes tap vs drag before popup opens. KeyView press
   state visual stays until finger up.
4. Insert path uses the existing onText callback (KeyboardView applies casing before
   calling the listener, as it already does for taps).
5. Accessibility: PopupWindow candidates remain reachable via explore-by-touch with
   click actions; every candidate has a contentDescription (its own character).
6. Verification: ./gradlew test assembleDebug green; on-device: long-press "e" shows
   popup (screenshot), drag-select inserts "é" (verify in a text field via screenshot
   or adb shell dumpsys via uiautomator layout dump); plain tap still inserts "e";
   shift+long-press inserts uppercase variant.
7. Commit.

## Task 9: README + full verification pass

Context: All features are in. This task finalizes docs and runs the full acceptance
sweep, fixing small in-scope issues found (bigger issues → report, do not hack).

1. README.md at repo root: project overview, features, privacy statement (all
   on-device, no network permission, nothing leaves the device), setup steps (enable
   IME + grant camera via the wizard), build instructions (./gradlew assembleDebug,
   android CLI), usage (SCAN key, torch, history, accents), limitations + roadmap
   section, no license file (flag in report that the user must choose one).
2. Full suite: ./gradlew clean test assembleDebug green.
3. On-device acceptance sweep on small_phone (or physical device, report which):
   install, wizard flow (grant + enable), keyboard types in Chrome URL bar and a
   notes app, SCAN opens viewfinder, QR + EAN-13 (or any 1D) scan auto-inserts +
   auto-closes + buzz, airplane-mode scan works (adb shell cmd connectivity airplane-
   mode enable) proving offline, torch toggles, rotate mid-scanner lands on keyboard
   (by design), revoke camera mid-session → inline permission state + open setup
   button works, IME switch away/back → camera released (privacy dot), history
   records scans + clipboard, tap-to-insert works. Capture screenshots at each
   checkpoint; report paths.
4. Small in-scope fixes allowed directly (typos, missing strings, layout nits);
   anything structural goes in the report instead.
5. Commit.

## Post-plan (controller-owned, not a task)

Final whole-branch review → fix wave → scoped re-review → adjudicate → delete SDD
workspace → finishing-a-development-branch.
