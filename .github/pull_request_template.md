## What changes

<!-- One or two sentences. What is different after this PR? -->

## Why

<!-- The problem this solves. Link the issue if there is one. -->

## Verification

<!-- What you actually ran, and the result. Not what should pass. -->

- [ ] `python3 scripts/validate_localizations.py`
- [ ] `gradle :shared:jvmTest :androidApp:assembleDebug :wearApp:assembleDebug :wearApp:testDebugUnitTest`
- [ ] `gradle :androidApp:validateDebugScreenshotTest`
- [ ] iOS build, if Apple sources changed

## User-visible surfaces

<!-- Tick every surface this touches; each needs its own check. -->

- [ ] Phone app  - [ ] Widget  - [ ] Apple Watch  - [ ] Wear OS
- [ ] Notifications  - [ ] All 9 locales, including RTL
