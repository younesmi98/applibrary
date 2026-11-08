# NEXUS 0.3.0 — release candidate notes

## Startup and platform presentation

- Added a NEXUS launch screen with the eight-spoke iOS-style fading activity indicator inspired by the loader on `younes.bond`.
- The splash checks that the local Room database opens before showing the app, uses a short minimum display duration to avoid flicker, and presents a retry state if local database initialization fails.
- The spinner is drawn with Compose Canvas; it adds no third-party animation dependency and becomes static when the app's Reduce Motion preference is enabled.

## Android compatibility and release configuration

- Prepared for Android 16 / API 36 and edge-to-edge system bars, with safe-drawing insets applied to app content and splash content.
- Updated the Android Gradle Plugin / Gradle pairing to AGP 8.9.1 and Gradle 8.11.1.
- Set app version to `0.3.0` / version code `3`.
- Added optional release signing from environment variables; signing credentials and the keystore are intentionally excluded from source control.
- CI now targets SDK 36, runs unit tests, and builds debug and release APK artifacts.

## Source fixes

- Removed a duplicate `FontFamily` import.
- Removed `ColorScheme.surfaceContainer*` properties that are not available in this project's pinned Material 3 dependency.
- Updated the launcher mark to use the NEXUS emerald accent instead of the previous cyan tint.
- Corrected the game-removal message so it no longer implies active session-tracking data is attached to the library entry.

## Not included / release gates

This is not a verified store release. The current environment could not run Gradle, Android SDK or the GitHub workflow. A signed APK requires the owner's own keystore. The Windows companion for launching manual PC executables is not implemented, and Epic live account sync is not supported; see `docs/KNOWN_ISSUES.md`.
