# Contributing to Scanner Keyboard

Thank you for your interest. This guide explains how to set up your
development environment. It also lists the standards this project uses.

Found a security problem? Read [SECURITY.md](SECURITY.md) first. Do not open
a public issue for a vulnerability.

## Development setup

You need these tools:

- JDK 17. The app compiles and builds with it.
- Android SDK 36. The app uses compileSdk and targetSdk 36, minSdk 24.
- Git.

Steps:

1. Clone the repository:

   ```bash
   git clone https://github.com/ubaierbhat/scanner-keyboard.git
   ```

2. Create `local.properties` in the project root. Point it at your SDK:

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

   `ANDROID_HOME` also works. The file `local.properties` is git-ignored.

3. Run the tests and build the debug APK:

   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```

   Gradle 9.1 downloads the rest for you.

   Note on JDK versions: the app build needs JDK 17. The unit-test tasks
   fork a separate JDK 21 JVM, because Robolectric needs it for the API 36
   sandbox. Gradle provisions that JDK automatically through the foojay
   resolver. You do not need to install JDK 21 yourself.

## Tests and CI

This repository has no Android CI job. The only GitHub Actions workflow is
`Pages`, and it just publishes the site in `public/`. Unit tests run on your
machine. Run `./gradlew testDebugUnitTest` before you push. All 58 tests must
pass.

## Project rules

- Do not add code comments. Keep new and changed source free of comments.
- Put strings, dimensions, and colors in XML resources, not in Kotlin code.
- Build the UI with the XML View system. Do not add Jetpack Compose.
- The app declares no `INTERNET` permission. Never add a network permission.
- The camera stays open only while the scanner view is on screen. Keep that
  lifecycle rule in any scanner change.

## Documentation style

Write docs in ASD-STE100 (Simplified Technical English) style:

- Maximum 20 words per sentence.
- One sentence, one idea.
- Active voice. Address the reader as "you".
- One term, one meaning. Do not invent synonyms.

## Device QA for keyboard and input changes

Keyboard and input changes need a physical device. Robolectric unit tests do
not prove real IME behavior.

- The reference device is a Samsung Galaxy XCover5. The required checks are
  listed in `docs/acceptance/manual-checklist.md`.
- After every reinstall, Android may reset or disable the active IME. Re-enable
  Scanner Keyboard in system settings and re-select it before you test.
- Cover at least: typing, shift and caps lock, accent popups, and dialpad
  auto-switch on phone and number fields. Also cover smart Enter, the scanner
  open and close with camera release, and history insert and clear.

## Commits and branches

Use the commit style already in this repository: a conventional prefix, a
lowercase summary, no trailing period.

```
feat: <what the change adds>
fix:  <what the change repairs>
build: <Gradle, signing, manifest, or packaging change>
docs: <documentation change>
chore: <housekeeping change>
```

Examples from the history:

```
feat: dialpad grid for phone and number layouts
fix: dialpad 4x4 grid matching Samsung reference, verified on device
```

Rules:

- Make one logical change per commit.
- Keep the build and tests green in every commit.
- Feature branches use `feature/<short-name>`, for example
  `feature/scanner-keyboard-v1`. Work merges to `main`.

## Pull requests

- Fill in the pull request template. Tick every checklist item.
- Link the issue you fix, if there is one.
- For keyboard or input changes, write your device QA results in the PR.
- Keep the PR summary short. Say what changed and why.
