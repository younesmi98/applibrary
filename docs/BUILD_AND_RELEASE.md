# NEXUS 0.3.0 — build and release

## Toolchain

- JDK 17
- Gradle 8.11.1
- Android Gradle Plugin 8.9.1
- Android SDK Platform 36 / Build Tools 36.0.0
- Android Studio Meerkat (or newer) for API 36 projects

The app keeps `minSdk 26` and targets `targetSdk 36`. The target is set for the current Google Play submission requirement for new apps and app updates (Android 16 / API 36 from 31 August 2026).

## Build locally

1. Open the `nexus/` project folder in Android Studio and install Android SDK Platform 36 and Build Tools 36.0.0 when prompted.
2. Download the official fonts, or let the GitHub Actions workflow do it:

   ```bash
   ./tools/fetch_fonts.sh
   ```

3. Run tests and builds:

   ```bash
   gradle :app:testDebugUnitTest
   gradle :app:assembleDebug :app:assembleRelease
   ```

Debug APK: `app/build/outputs/apk/debug/`

Release APK: `app/build/outputs/apk/release/`

## Signing a distributable release

A release APK built without a keystore is **unsigned** and cannot be installed as a normal release update or submitted to an app store. Never commit the keystore or its passwords to Git.

Create a private upload/release keystore once and keep a secure backup:

```bash
keytool -genkeypair -v \
  -keystore nexus-release.jks \
  -alias nexus \
  -keyalg RSA -keysize 4096 -validity 10000
```

Set these environment variables before building:

```bash
export NEXUS_STORE_FILE="/absolute/path/to/nexus-release.jks"
export NEXUS_STORE_PASSWORD="..."
export NEXUS_KEY_ALIAS="nexus"
export NEXUS_KEY_PASSWORD="..."
gradle :app:assembleRelease
```

For GitHub Actions, add repository Actions secrets with these exact names:

- `NEXUS_KEYSTORE_BASE64` — base64-encoded contents of `nexus-release.jks`
- `NEXUS_STORE_PASSWORD`
- `NEXUS_KEY_ALIAS`
- `NEXUS_KEY_PASSWORD`

Without those secrets, CI still produces an unsigned release APK for build verification. With all four configured, it signs the release APK. Keep a second secure copy of the keystore; losing it can prevent updates to the same Android application ID.

## Current release caveats

- This source package has not been built in the current execution environment: it has no Android SDK, Gradle distribution, or outbound DNS/network. CI must run successfully before calling the APK verified.
- A signed APK needs the owner's private keystore; no signing key is included.
- Epic has no live account integration here; supported import is file-based.
- Manual game entry is available, but launching Windows `.exe` games from Android needs a separate Windows companion/relay, which is not implemented in this package.
- Steam integration currently uses the user's own Steam Web API key stored encrypted on-device; it is not Steam OpenID sign-in. Steam privacy settings must permit game details.
- Physical-device verification on Galaxy A13, Play Console pre-launch checks, privacy disclosures, and final store assets remain release gates.
