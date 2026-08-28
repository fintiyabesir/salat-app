# TestFlight release guide

Salat's first internal TestFlight candidate is version **0.1.0 (1)** for iPhone and iPad. Apple Watch and Wear OS work continues separately and does not block this first phone/tablet beta.

## Current Apple identifiers

- iOS app bundle ID: `app.salat.mobile`
- Widget extension bundle ID: `app.salat.mobile.widget`
- App Group: `group.app.salat.mobile`
- Marketing version: `0.1.0`
- Initial TestFlight build: `1`

The app and widget explicitly bind `CFBundleShortVersionString` to `MARKETING_VERSION` and `CFBundleVersion` to `CURRENT_PROJECT_VERSION`, so the archive metadata is deterministic for App Store validation.

## App Store Connect / Developer account setup

Before the first upload, the Apple Developer account must contain:

1. An explicit App ID for `app.salat.mobile` with App Groups enabled.
2. An explicit App ID for `app.salat.mobile.widget` with App Groups enabled.
3. The App Group `group.app.salat.mobile`, assigned to both identifiers.
4. An App Store Connect app record associated with `app.salat.mobile`.
5. A valid Apple Distribution certificate whose private key is available to GitHub Actions.
6. An App Store Connect API key with sufficient access for signing/provisioning and build upload.

## GitHub repository secrets

The TestFlight workflow expects these secret names:

- `APPLE_TEAM_ID`
- `APPLE_DISTRIBUTION_CERTIFICATE_BASE64`
- `APPLE_DISTRIBUTION_CERTIFICATE_PASSWORD`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_API_PRIVATE_KEY_BASE64`

The certificate secret is the base64 representation of a `.p12` containing the Apple Distribution certificate and its private key. The API private-key secret is the base64 representation of the `.p8` key downloaded from App Store Connect.

## Workflow

`.github/workflows/testflight.yml` has two modes:

- On a release pull request, `release-preflight` selects Xcode 26.3, builds the KMP device framework, generates the Xcode project and creates a real unsigned Release `.xcarchive` for a generic iOS device. It also validates the app icon, privacy manifests, widget embedding, version/build values, export-compliance flag and bundled offline city catalog.
- From **Actions → TestFlight Release → Run workflow**, set a build number and enable **Upload to TestFlight**. The workflow installs signing material temporarily, performs automatic provisioning using the App Store Connect API key, exports an App Store Connect IPA, validates it and uploads it.

Signing material is stored only in the runner's temporary keychain and deleted at the end of the job.

## Privacy / export compliance assumptions for build 1

The current app has no account, no analytics, no advertising and no Salat backend. Prayer calculation, settings, manual city data and current-location handling are device-local. The privacy manifests declare no tracking and no collected data. `UserDefaults` required-reason API usage is declared for app-local settings (`CA92.1`) and the App Group used by the widget (`1C8F.1`).

If a future official-source verification implementation transmits location or another user-related value off-device, this declaration and the App Store Connect App Privacy answers must be reviewed before that build is uploaded.

`ITSAppUsesNonExemptEncryption` is `false`; Salat does not implement non-exempt encryption. Revisit this if cryptographic functionality beyond Apple's exempt/system-provided mechanisms is introduced.

## Build-number policy

Keep marketing version `0.1.0` during the initial internal beta and increment only the TestFlight build number (`1`, `2`, `3`, ...). This makes rapid internal iterations easy to identify without creating unnecessary App Store versions.
