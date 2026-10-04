# Security Policy

Scanner Keyboard is an input method editor (IME). A keyboard is a high-trust
component. It sits between you and every app you type into. This page explains
what the app can see, and how to report a problem safely.

## Threat model

- An active IME can see every keystroke and clipboard copy by design. No
  keyboard app can avoid this while the system selects it.
- The app stores none of that data on its own. It records only the scan and
  copy entries shown in the History panel.
- The app declares no `INTERNET` permission. It cannot send data off the
  device. The Android platform enforces this, not just convention.
- Barcode decoding runs on-device with the bundled ML Kit model. Camera frames
  and decoded values never leave the phone.
- The camera is open only while the scanner view is on screen. It closes when
  the scanner closes, the keyboard hides, or the app moves to the background.
- History and clipboard captures live in the app's private storage. You can
  clear them with one tap (CLEAR). Uninstall removes them.
- A keyboard deserves the same scrutiny as a password manager. If you need
  the highest assurance, build the APK yourself from this source. Compare your
  build with the published release signature.

## Reporting a vulnerability

- Report vulnerabilities privately through GitHub. Open a security advisory at:
  <https://github.com/ubaierbhat/scanner-keyboard/security/advisories/new>
- The same entry point is the repository **Security** tab, then **Report a
  vulnerability**.
- Do not open a public issue for a vulnerability. Public issues expose users
  before a fix ships.
- Open a normal issue only when your report contains no sensitive detail.

## Disclosure timeline

- We follow coordinated disclosure.
- We aim to confirm, fix, and publish a security advisory within 90 days of a
  valid report.
- A fix ships in a release before any public disclosure of the problem.
- If 90 days pass without a fix, we coordinate the timeline with the reporter.
  Nothing becomes public without that agreement.
