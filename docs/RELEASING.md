# Releasing Awqat

Releases are triggered by **tags**, not by merges. A merge to `main` publishes to the
internal track only. A tag is the immutable record of what a version actually contained —
a CI run counter is not, because it resets when a workflow is renamed or recreated.

## Tracks

| Track | Trigger | Apple | Google Play | Gate |
|---|---|---|---|---|
| CI | any pull request | build + test | build + test | branch protection |
| `internal` | merge to `main` | TestFlight internal | Play internal testing | none |
| `beta` | tag `v1.2.3-beta.1` | TestFlight external | Play closed testing | none |
| `production` | tag `v1.2.3` | App Store Connect | Play production (draft) | **required reviewer** |

The `production` GitHub Environment carries a required reviewer. A production tag builds,
verifies and then **waits** in the Actions UI until a human approves. Nothing reaches users
before that click.

Neither store publishes automatically even after approval. Apple uploads the build to App
Store Connect, where you submit it for review; Play creates a **draft** release you review
and roll out in the Play Console. Both are deliberate — the pipeline gets the artifact to
the door, a person opens it.

## Versioning

- **Marketing version** (`0.1.0`) lives in `version.properties` — the only place. Android,
  Wear and the Apple build all read it.
- **Build number** is `git rev-list --count HEAD`, the commit count. It is monotonic and
  stored nowhere. App Store Connect permanently rejects a build number that is not greater
  than the previous one, so a counter that can reset is a trap.
- The release workflow **fails** if a tag disagrees with `version.properties`, so
  `v0.2.0` cannot be cut while the file still says `0.1.0`.

Because the build number is the commit count, every release checkout uses `fetch-depth: 0`.
A shallow clone would report `1`.

## Cutting a release

```bash
# 1. Bump the marketing version and commit it through a pull request.
#    Patch: fixes only. Minor: new user-visible capability. Major: a redesign.
vim version.properties

# 2. After it merges, tag the merge commit.
git checkout main && git pull
git tag -a v0.2.0 -m "Awqat 0.2.0"
git push origin v0.2.0
```

A beta first is the normal path: tag `v0.2.0-beta.1`, let testers use it, then tag `v0.2.0`
on the same commit when it holds up.

Then approve the `production` deployment in the Actions run, submit for review in App Store
Connect, and roll out the draft in the Play Console.

## Required secrets

Apple secrets currently sit at **repository** level. Move them into the three Environments
so that no workflow outside a release can read them:

```bash
gh secret set APPLE_TEAM_ID --env production      # repeat for internal, beta
```

Add to each environment, verify a release still works, and only then delete the
repository-level copies with `gh secret delete <NAME>`.

| Secret | Purpose |
|---|---|
| `APPLE_TEAM_ID` | Apple Developer team |
| `APPLE_DISTRIBUTION_CERTIFICATE_BASE64` | Distribution certificate, base64 `.p12` |
| `APPLE_DISTRIBUTION_CERTIFICATE_PASSWORD` | Password for that `.p12` |
| `APPLE_DEVELOPMENT_CERTIFICATE_BASE64` | Development certificate, base64 `.p12` — see below |
| `APPLE_DEVELOPMENT_CERTIFICATE_PASSWORD` | Password for that `.p12` |
| `APP_STORE_CONNECT_API_KEY_ID` | ASC API key id |
| `APP_STORE_CONNECT_ISSUER_ID` | ASC issuer id |
| `APP_STORE_CONNECT_API_PRIVATE_KEY_BASE64` | ASC `.p8`, base64 |
| `ANDROID_KEYSTORE_BASE64` | Upload keystore, base64 `.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Upload key alias |
| `ANDROID_KEY_PASSWORD` | Upload key password |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Play Developer API service account, raw JSON |

Every upload step is skipped with a warning when its secrets are absent, so the pipeline
stays useful before they all exist.

## Why a development certificate is needed

`xcodebuild archive` signs with a **development** identity and `-exportArchive` then
re-signs for distribution. That is Apple's flow, not a misconfiguration.

A certificate's private key never leaves the machine that generated it, and every CI
runner is a fresh machine. Without a development identity to import, automatic signing
asks Apple for a **new certificate on every run** — which silently consumed the
account's certificate limit until every release failed with "Your account has reached
the maximum number of certificates".

Supplying one development `.p12` fixes it permanently: Xcode finds an identity and
reuses it. Export it from Keychain Access under **My Certificates** (that category only
lists certificates whose private key you hold), as Personal Information Exchange.

The release job prints `security find-identity` after import, so a future signing
failure shows what the runner actually had.

## Google Play setup

The Play side is not yet live. In order:

1. Create the Play Console app record for `app.salat.mobile` and enable **Play App Signing**.
   The keystore in CI is then only the *upload* key; Google holds the signing key.
2. **Upload the first bundle by hand.** The Play Developer API cannot create an app's first
   release — the automated upload only works from the second one onward.
3. Create the service account, grant it release permissions, and store its JSON as
   `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
4. Start closed testing. A personal Play Console account created on or after
   13 November 2023 must run a closed test with **12 testers opted in for 14 continuous
   days** before it can apply for production access, per app. That is calendar time that
   cannot be compressed, so start it before the code is finished.

## Open items

These are known gaps, deliberately left rather than guessed at:

- **R8 is off.** `isMinifyEnabled = false` in both Android modules. A shrunk build has
  never run on a device. Enable it, smoke-test every surface — notifications, widget,
  Wear tile, all locales — and only then ship it.
- **Wear OS is built but not published.** The release workflow archives
  `wearApp-release.aab` but uploads only the phone bundle, because `wearApp` currently
  shares `applicationId` with the phone app and a shared Play listing needs a decided
  version-code scheme. Resolve this when the Play listing is created.
- **The automatic path has never run.** Before this workflow existed, every TestFlight
  upload was a manual `workflow_dispatch`. The first tag will be the first real exercise
  of the whole chain.

## Emergency

Branch protection applies to administrators. To push a hotfix directly:

```bash
gh api -X DELETE repos/fintiyabesir/salat-app/branches/main/protection/enforce_admins
# ... push the fix, then immediately restore it:
gh api -X POST repos/fintiyabesir/salat-app/branches/main/protection/enforce_admins
```

Prefer a normal pull request. CI takes minutes; a broken release lasts until the next one.
