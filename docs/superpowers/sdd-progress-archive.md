# SDD ledger — plan: docs/superpowers/plans/2026-09-27-scanner-keyboard-plan.md
Task 1: fix round 1/5 (1 addressed, 0 open; commits 45a01a2..d5d7718)
Task 1: minor (deferred): template launcher icons (stock robot mipmap/drawable) retained
Task 1: minor (deferred): android:windowSoftInputMode=adjustResize on <application> is inert (activity-level attr)
Task 1: minor (deferred): duplicate local.properties entry in .gitignore
Task 1: complete (commits cb647eb..d5d7718, review clean)
Task 2: minor (deferred): KeyView extends TextView (custom View, not AppCompatButton — intent satisfied; confirm at final review)
Task 2: minor (deferred): KEYCODE_DEL fallback 2-arg KeyEvent — UP downTime mismatch; fix with Task 3 work
Task 2: minor (deferred): letter contentDescription stale after shift toggle — fix with KeyboardState in Task 3
Task 2: minor (deferred): getSelectedText(0) IPC per backspace — consider caching in Task 3 refactor
Task 2: minor (deferred): ./gradlew test vacuously green (NO-SOURCE) — informational
Task 2: complete (commits d5d7718..860a320, review clean, 5 minors deferred)
Task 3: minor (deferred): KeyView multi-pointer — repeat not stopped on ACTION_POINTER_UP; track gesture id
Task 3: minor (deferred): showLayer ordinal-couples LAYER_IDS to KeyboardLayer enum order — use Map
Task 3: minor (deferred): caps-lock lacks accessibility long-press equivalent; stale Shift contentDescription on disabled symbol-layer shift
Task 3: minor (deferred): duplicate '(' string resource (key_alt_paren_open vs key_sym_open_paren)
Task 3: minor (deferred): keyboard_view.xml ~1200 lines of per-key boilerplate; consider per-layer includes when Task 7 edits bottom rows
Task 3: complete (commits 860a320..2303652, review clean, 22 unit tests, 5 minors deferred)
Task 4: minor (deferred): onRequestPermissionsResult ignores requestCode (safe today)
Task 4: minor (deferred): no permanent-denial guidance path in setup wizard (brief silent)
Task 4: minor (deferred): mixed inline-dp vs named dimens convention in activity_setup.xml
Task 4: complete (commits 2303652..4065cb4, review clean, physical-device verified, 3 minors deferred)
Task 5: fix round 1/5 (1 addressed, 0 open; commits 035eb11..586f70e)
Task 5: minor (deferred): caught camera errors not logged (generic string to callback only) — fold diagnostic into Task 6
Task 5: minor (deferred): permission-denied prompt does not self-refresh if grant happens while shown
Task 5: minor (deferred): landscape rotation while scanner open does not recompute height
Task 5: minor (deferred): providerFuture.get() on main thread inside addListener (immediate since resolved; note only)
Task 5: complete (commits 4065cb4..586f70e, review clean after 1 fix round, physical-device verified, 4 minors deferred)
Task 6: fix round 0 (review clean first pass)
Task 6: minor (deferred): InputImage.fromMediaImage outside try — unreachable throw path could skip proxy close
Task 6: minor (deferred): unreachable '?: return' guards in bindPreview silently skip bind instead of fail()
Task 6: minor (deferred): ScanThrottle.lastAllowedAt non-volatile — safe by executor serialization; make @Synchronized if executor changes
Task 6: parked-for-Task-9: live aim-at-QR insert + back-to-back throttle acceptance (physical phone camera-down on desk; needs human aim ~10s)
Task 6: complete (commits 586f70e..9ab75e4, review clean, 27 unit tests, offline proven on device, 3 minors + 1 acceptance deferred)
Task 7: fix round 1/5 (1 addressed, 0 open; commits 8138571..e2a85f3; long-payload 2-line cap proven task7-09, symbols-layer HIST shot task7-10 also closes Task-6-era evidence gap)
Task 7: minor (deferred): closeHistory() hides panel only when mode==HISTORY — make unconditional for self-contained cleanup
Task 7: minor (deferred): clipboard coerceToText is main-thread IPC every fresh onStartInputView — plan-sanctioned, on record
Task 7: complete (commits 9ab75e4..e2a85f3, review clean after 1 fix round, 34 unit tests, clipboard capture proven on physical device, 2 minors deferred)
Task 7: note — onClear 3rd setCallbacks param judged acceptable design completion by reviewer
Task 8: minor (deferred): slop uses scaledTouchSlop (~8dp) vs brief literal ~8px — idiomatic, wording deviation only
Task 8: minor (deferred): candidate y-band extends one slop above popup; below-flip makes thin strip of origin key resolve to candidate — guard with origin exclusion
Task 8: minor (deferred): touchable PopupWindow lets second pointer fire candidate onPress (double-insert vector inside declared multitouch gap) — touchable=false cheap
Task 8: minor (deferred): showAccentPopup ~60-line builder in KeyboardView (+177 lines, now ~370) — extract AccentPopup collaborator
Task 8: minor (deferred): popup width clamp uses displayMetrics.widthPixels vs window width — benign portrait
Task 8: complete (commits e2a85f3..d2fb52b, review clean first pass, 39 unit tests, 6/6 device checks, 5 minors deferred)

## Task 9 started (README + full verification pass)
Task 9: minor (deferred): README says 'open-source' while license pending — reword or license
Task 9: minor (deferred): README clipboard wording should say picked-up-on-next-show, not at-copy-time
Task 9: minor (deferred): sweep used Settings-search+Messages instead of Chrome+Notes (functionally equivalent; substitution undisclosed in report)
Task 9: minor (deferred): README 'phone buzzes' — performHapticFeedback, silent if haptics disabled
Task 9: controller-action: qr/ acceptance assets + screenshots live in SDD workspace — persist before workspace deletion
Task 9: complete (commits d2fb52b..c5bdbee, review clean, README-only, 33/33 automatable sweep PASS, 3 aim-checks deferred to human checklist)
Final review: With fixes — 0 Critical, 3 Important (allowBackup privacy leak, permanent-denial dead-end, README open-source/license contradiction), 6 minors triaged fix-before-merge: false
Final review triage: fix-before-merge = allowBackup, denial path, README x2, fromMediaImage try-move, origin-first release order, mode-guard in handleBarcodeResult; resolved-in-HEAD already: DEL downTime, contentDescription refresh, camera error logging, prompt self-refresh
Final fix wave: complete (commits c5bdbee..5fcfa70; 8/8 addressed; re-review clean)
Final review: parked — in-memory cameraPermissionRequested can mislabel button after process death / back-cancel — ruling: real, self-heals via onRequestPermissionsResult, settings path always works; roadmap polish, not load-bearing
Branch done: 15 commits cb647eb..5fcfa70; awaiting human aim-checks via docs/acceptance/manual-checklist.md; license choice outstanding (user)
