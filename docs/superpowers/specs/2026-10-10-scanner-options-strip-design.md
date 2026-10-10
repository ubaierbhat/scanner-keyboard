# Scanner Options Strip (Form Toggle In-Scanner) — Design

Date: 2026-10-10
Status: approved by product owner ("go", 2026-10-10)
Builds on: `ed5ca48` (continuous-scan/actions wave, unreleased, version 3 / 1.0.2)

## Problem

Form mode (`translate_scan_actions`) is toggled only from the app's setup screen —
the only way to leave the keyboard mid-workflow to change scan behavior. With
Continuous already living on the scanner preview, a second scanner-relevant
setting buried elsewhere is inconsistent and cumbersome. Naive solution (stack a
second pill) would fragment the preview chrome; needs a pattern that scales as
more scan options arrive.

## Decisions (product owner)

| Question | Decision |
|---|---|
| Layout | Option A: one top pill as a two-cell options strip; container designed to grow |
| Label | "Form" (short; form-filling language) |
| Setup-screen Scanning card | **Removed** — one control per setting, in context |

UX rationale: settings (top strip) stay separated from exits (✕/Torch bottom
bar). Labeled switches chosen over icon keys because they are self-explanatory
today; migration to icon keys (Option B) is deferred until cells exceed ~4 —
YAGNI.

## Design

### 1. `scanner_view.xml` — options strip

The existing Continuous pill container becomes a generic horizontal strip:

```
[ "Continuous" (sw) │ divider │ "Form" (sw) ]
```

- Same rounded scrim background (`scanner_pill_background`), same position
  (top|center_horizontal), same margins.
- Cell = `TextView` label + `MaterialSwitch`, label marginEnd 6dp.
- Divider = `View` 1dp wide, vertical margins ~10dp, color `#33FFFFFF`
  (new color token `scanner_divider`).
- Switch ids: `scanner_continuous_switch` (unchanged), `scanner_form_switch`
  (new).
- New string `scanner_form_label` = "Form".

### 2. `ScannerView.kt` — binding

- Add `formSwitch: MaterialSwitch` field bound to `settings.translateScanActions`:
  init sets checked from prefs; change listener persists.
- Extract the duplicated switch-listener wiring (deferred Task-5 minor) into one
  private `bindSwitches()` used by `init` and `start()` (start re-syncs silently:
  clear listeners, set checked from prefs, re-attach).
- Continuous behavior unchanged, including `onContinuousChanged` → suppressor
  reset. Form switch needs no callback: `handleBarcodeResult` re-reads
  `translateScanActions` per scan (already implemented).

### 3. Setup screen — card removal

- `activity_setup.xml`: delete the entire `scan_card` MaterialCardView.
- `MainActivity.kt`: remove `scanActionsSwitch`, `scanSettings` fields, binding
  block, and now-unused imports (`MaterialSwitch`, `ScanSettings`).
- `strings.xml`: remove `setup_step_scan_title`, `setup_step_scan_description`,
  `setup_step_scan_switch_label`.
- No sync logic needed anywhere (single source of truth restored by removal).

### 4. Untouched by design

Service commit/translate path, `ScanSettings` keys/values (existing installs keep
their state), `ScanTranslator`, `DuplicateSuppressor`, keyboard geometry,
nav-bar insets, permission flow.

## Testing

- Extend `ScannerViewContinuousTest` (rename to `ScannerViewSwitchesTest`):
  - form switch initialized from `translateScanActions`, toggling persists it;
  - continuous tests preserved;
  - both switches restored in a freshly constructed view.
- Full suite green; `assembleDebug`.
- Device QA (Fire 6; XCover5 when back on USB):
  1. Strip renders both labeled switches, no overlap with hint/toolbar.
  2. Form ON in scanner → scan `A\nB` into single-line field → focus jump +
     segments land per settled routing rules (re-verifies the whole path via
     the NEW control).
  3. Form state persists across scanner close/reopen; service honors it on the
     very next scan without restart.
  4. Continuous regression: dedupe still works with Form ON simultaneously.
  5. Setup screen: no Scanning card; remaining flow intact (screenshot).
- Docs: CHANGELOG Unreleased wording ("on the setup screen" → scanner toggle),
  README Scan-actions bullet, architecture.html setup-screen mentions if any.

## Constraints

- NO version bump (stays 3 / 1.0.2) — same unreleased wave.
- No new dependencies; minSdk 24; work on main; push at wave end per SDD.

## Out of scope

- Icon-key strip migration.
- Additional options (beep/auto-copy/keep-awake) — strip structure accommodates
  them, none built here.
- Any change to Form-mode translation semantics.
