# Build, APK and security checklist
No APK is included. It cannot be built in this chat (no Android SDK here). To build one:
1. Open the whole project in Android Studio, let Gradle sync, fix any compile errors (none of phases 2-8 was compiled yet).
2. Add plugins/deps used by the new modules: kotlinx-serialization (plugin + json), JUnit4. Add the `polish`, `insights`, `pc`, `importers` modules to settings.gradle.
3. Copy app-res/values and values-fa into app/src/main/res. Set android:supportsRtl="true" in the manifest.
4. Run all unit tests: ./gradlew test
5. Debug APK: ./gradlew assembleDebug  (app/build/outputs/apk/debug)
6. Release APK: create a keystore (keep it OUT of git), configure signingConfigs from environment variables, then ./gradlew assembleRelease.

Security check before release (each item is still to be verified by you, none was verified here):
- [ ] No API keys or secrets in the APK (search the build output for the Steam key and IGDB/RAWG keys).
- [ ] Steam calls go through the backend; backend env vars are not in git.
- [ ] Device tokens and Steam id stored with Android Keystore / EncryptedSharedPreferences.
- [ ] android:allowBackup considered, so tokens do not end up in cloud backups.
- [ ] Supabase row-level security enabled and tested with a second account.
- [ ] Notification permission is requested only after explaining why.

Known limits: see KNOWN_ISSUES.md.
