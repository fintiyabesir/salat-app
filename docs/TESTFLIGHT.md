# TestFlight release guide

Awqat's first internal TestFlight candidate was version **0.1.0 (2)** for iPhone and iPad, validated by App Store Connect and installed from TestFlight on a physical iPhone. The most recent verified upload was workflow run **#38** (manual `workflow_dispatch`). Apple Watch is included from the next beta after its identifiers are configured; Wear OS is shipped through the Android release path.

## Current Apple identifiers

- iOS app bundle ID: `app.salat.mobile`
- Widget extension bundle ID: `app.salat.mobile.widget`
- Apple Watch app bundle ID: `app.salat.mobile.watchapp`
- Apple Watch complication bundle ID: `app.salat.mobile.watchapp.complicationextension`
- App Group: `group.app.salat.mobile`
- Marketing version: `0.1.0`
- Build number source: `github.run_number` for automatic runs, the `build_number` input for `workflow_dispatch`
- Latest verified upload: workflow run `#38`

The committed app, widget and watch property lists are the release metadata source of truth. XcodeGen references them without regenerating them, so `MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`, export-compliance metadata, extension metadata and App Group entitlements survive project generation.

## App Store Connect / Developer account setup

Before the first upload, the Apple Developer account must contain:

1. An explicit App ID for `app.salat.mobile` with App Groups enabled.
2. An explicit App ID for `app.salat.mobile.widget` with App Groups enabled.
3. The App Group `group.app.salat.mobile`, assigned to both phone/widget identifiers.
4. An App Store Connect app record associated with `app.salat.mobile`.
5. A valid Apple Distribution certificate whose private key is available to GitHub Actions.
6. An App Store Connect API key with sufficient access for signing/provisioning and build upload.

Before uploading the first Apple Watch-enabled build, also create explicit App IDs for `app.salat.mobile.watchapp` and `app.salat.mobile.watchapp.complicationextension`, enable App Groups on both, and assign the existing `group.app.salat.mobile` group to both identifiers.

## GitHub repository secrets

The TestFlight workflow expects these secret names:

- `APPLE_TEAM_ID`
- `APPLE_DISTRIBUTION_CERTIFICATE_BASE64`
- `APPLE_DISTRIBUTION_CERTIFICATE_PASSWORD`
- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_API_PRIVATE_KEY_BASE64`

The certificate secret is the base64 representation of a `.p12` containing the Apple Distribution certificate and its private key. The API private-key secret is the base64 representation of the `.p8` key downloaded from App Store Connect.

Automatic uploads also use the repository variable `AUTO_TESTFLIGHT_UPLOAD`. Keep it unset or set to `false` until every embedded target has a registered App ID and provisioning is ready. Set it to `true` to enable automatic uploads after successful `main` builds.

## Workflow

`.github/workflows/testflight.yml` has three modes:

- On a release pull request, `release-preflight` selects Xcode 26.3, builds the KMP device framework, generates the Xcode project and creates a real unsigned Release `.xcarchive` for a generic iOS device. It also validates the app icon, privacy manifests, widget embedding, version/build values, export-compliance flag and bundled offline city catalog.
- After `Core CI` succeeds for a push to `main`, the same preflight runs against the exact tested commit. When `AUTO_TESTFLIGHT_UPLOAD` is `true`, the workflow then signs, validates and uploads that commit to TestFlight. Runs are queued instead of cancelled, so every successful `main` push gets its own build.
- From **Actions → TestFlight Release → Run workflow**, set a build number and enable **Upload to TestFlight**. The workflow installs signing material temporarily, performs automatic provisioning using the App Store Connect API key, exports an App Store Connect IPA, validates it and uploads it.

Signing material is stored only in the runner's temporary keychain and deleted at the end of the job.

## Privacy / export compliance assumptions for build 1

The current app has no account, no analytics, no advertising and no Awqat backend. Prayer calculation, settings, manual city data and current-location handling are device-local. The privacy manifests declare no tracking and no collected data. `UserDefaults` required-reason API usage is declared for app-local settings (`CA92.1`) and the App Group used by the widget (`1C8F.1`).

If a future official-source verification implementation transmits location or another user-related value off-device, this declaration and the App Store Connect App Privacy answers must be reviewed before that build is uploaded.

`ITSAppUsesNonExemptEncryption` is `false`; Awqat does not implement non-exempt encryption. Revisit this if cryptographic functionality beyond Apple's exempt/system-provided mechanisms is introduced.

## Build-number policy

Keep marketing version `0.1.0` during the initial internal beta. Automatic `main` uploads use the monotonically increasing GitHub Actions workflow run number as `CFBundleVersion`; manual uploads use the build number entered when dispatching the workflow. This makes rapid internal iterations easy to identify without creating unnecessary App Store versions. A manually supplied build number must not duplicate an existing App Store Connect build.


## Automatic uploads

Superseded. Releases are now tag-driven and documented in [RELEASING.md](RELEASING.md);
a merge to `main` reaches the internal track only. The `AUTO_TESTFLIGHT_UPLOAD`
repository variable is no longer read by any workflow and can be deleted.
