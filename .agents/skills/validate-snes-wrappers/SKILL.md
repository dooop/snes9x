---
name: validate-snes-wrappers
description: Validates the Snes9x Apple and Android wrappers, triages build and lint output, and checks release hygiene and Snes9x license obligations. Use after code or build changes, before handoff or release, or when asked to test, verify, inspect CI readiness, or audit generated files. Do not use as the primary implementation workflow.
---

# Validate SNES wrappers

Run from the repository root. Do not clean or delete the user's existing outputs merely to obtain a clean run.

## Preflight

1. Record `git status --short` and preserve unrelated changes.
2. Confirm `git submodule status` shows an initialized `snes9x` commit.
3. Confirm `swift/Sources/Snes9xCore` and `LICENSE` still resolve to the intended submodule paths.
4. Ensure no tracked modification exists below `snes9x/`.

## Execute checks

Choose the full affected matrix; do not claim a platform passed from a different host build.

```sh
swift package dump-package >/dev/null
swift build
swift test
xcodebuild -scheme snes -destination 'generic/platform=iOS' build
./gradlew :snes:assembleDebug :app:assembleLocalDebug
./gradlew :snes:lintDebug :app:lintLocalDebug
```

Use `./scripts/validate.sh` for the repository's shorter combined entry point, but add `swift test`, iOS, and Android lint when their toolchains are available.

## Interpret results

- Inspect the generated library and local-sample lint reports and treat every error as a failed validation.
- Distinguish test count from XCTest's compatibility summary when Swift Testing is in use.
- Report skipped checks with the missing toolchain or environment requirement.
- Do not dismiss warnings that affect correctness, packaging, ABI support, accessibility, or lifecycle behavior.

## Check repository and release hygiene

1. Run `git status --short` after validation and distinguish intended edits from ignored generated output.
2. Confirm no generated build product is tracked.
3. For distributed binaries, verify the Snes9x non-commercial terms, complete copyright notice, reproducible build instructions, and notices for bundled components are included; commercial distribution requires upstream permission.
4. Report results as a compact pass/fail/skipped matrix followed by actionable findings ordered by severity.
