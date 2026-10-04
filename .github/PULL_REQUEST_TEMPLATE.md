## What does this change?

Describe the change in one or two sentences. Say why it is needed. Link the
issue it fixes, if there is one.

## Checklist

Put an `x` in every box that applies.

- [ ] Unit tests pass: `./gradlew testDebugUnitTest`
- [ ] Device QA done for keyboard, IME, scanner, or input changes. Re-enable
      and re-select Scanner Keyboard after each reinstall before testing.
- [ ] Docs and UI strings follow ASD-STE100 style: short sentences, active
      voice, one term per idea.
- [ ] No new code comments. Strings, dimensions, and colors stay in XML
      resources. UI stays on the XML View system.
- [ ] Commit messages use the repo prefixes (`feat:`, `fix:`, `build:`,
      `docs:`, `chore:`).

## Screens

Attach screenshots for UI or keyboard changes. A screenshot of the live IME
helps the reviewer a lot.

## Notes for reviewers

- This repo has no Android CI. Check the test output in the PR description.
- Security fixes: do not disclose details before the fix ships. See
  [SECURITY.md](../SECURITY.md).
