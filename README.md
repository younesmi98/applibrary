# NEXUS 0.3.0

<<<<<<< HEAD
## Get a debug APK without Android Studio
1. Create a GitHub repository and upload this project (zip contents, keep the `.github` folder).
2. Open the Actions tab, run "Build NEXUS debug APK" (it also runs on every push).
3. When it finishes, open the run and download the `nexus-debug-apk` artifact. Unzip it and install the APK.
4. If the run fails, copy the red error lines from the log and send them back.

## Fonts
The workflow downloads Vazirmatn and Inter (SIL OFL) with `tools/fetch_fonts.sh` before building. Run the same script locally if you build elsewhere (needs `gh`, `unzip`, network).
=======
Android game library and collection manager. See `docs/STATUS.md` for what is included and `docs/KNOWN_ISSUES.md` for release gates.

## Build and test

Requirements: JDK 17, Gradle 8.11.1, Android SDK Platform 36 and Build Tools 36.0.0.

```bash
./tools/fetch_fonts.sh
gradle :app:testDebugUnitTest
gradle :app:assembleDebug :app:assembleRelease
```

The project currently has no Gradle wrapper. Android Studio or a local Gradle 8.11.1 installation can open/build it. GitHub Actions also downloads official Vazirmatn/Inter fonts, runs unit tests and publishes debug/release APK artifacts.

## Release signing

An unsigned release APK can be built for verification. To create an installable distributable APK, configure `NEXUS_STORE_FILE`, `NEXUS_STORE_PASSWORD`, `NEXUS_KEY_ALIAS`, and `NEXUS_KEY_PASSWORD` as documented in `docs/BUILD_AND_RELEASE.md`. Never commit the signing keystore or passwords.

## Fonts

Official Vazirmatn and Inter font files are fetched by `tools/fetch_fonts.sh` during CI. Font binaries are not checked into this source archive.
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
