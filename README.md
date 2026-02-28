# Opsec Android Client

Offline-first Android client for signed `catalog.json` feeds.

## Modules

- `:app` Compose UI, WorkManager sync, install flows
- `:data` Room + FTS, repository implementation
- `:network` OkHttp client + remote catalog fetch
- `:domain` models + use cases
- `:security` Ed25519 signature and integrity verification

## Security invariants

- Only HTTPS network requests are allowed.
- Catalog updates must pass Ed25519 signature verification and pinned public-key fingerprint check.
- Invalid/untrusted catalogs are rejected.
- APK install path uses Android Package Installer (no silent installs).

## Build defaults

- `minSdk = 26`
- `targetSdk = 35`

## GitHub Actions

- CI workflow: `.github/workflows/android-ci.yml`
  - Runs on push to `main` and pull requests.
  - Builds debug APK and runs unit tests.
- Release workflow: `.github/workflows/android-release.yml`
  - Runs on tags matching `v*.*.*`.
  - Builds signed release APK and publishes a GitHub Release with:
    - `OpsecApp-<tag>.apk`
    - `OpsecApp-<tag>.apk.sha256`

### Required repository secrets

- `ANDROID_KEYSTORE_BASE64`: base64 of your `.jks`/`.keystore`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

### Trigger a release

```bash
git tag v1.0.1
git push origin v1.0.1
```
