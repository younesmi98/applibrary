# Remaining issues and release gates

1. **Build validation is outstanding.** The current container did not have Gradle or Android SDK and could not resolve external hosts. The GitHub Actions workflow must complete `testDebugUnitTest`, `assembleDebug`, and `assembleRelease` successfully before distributing a build.
2. **Release signing is owner-controlled.** The archive has no keystore. Configure the four `NEXUS_*` signing secrets/variables to produce a signed release APK.
3. **Windows manual-game launch is not end-to-end.** The app stores manual game metadata; a Windows companion and secure PC relay are not implemented. Do not advertise `.exe` launch from the phone until this exists.
4. **Epic live sync is unavailable.** Epic games can be imported from supported Legendary/Heroic exports or CSV; no Epic credentials are requested or stored.
5. **Steam presence is polling, not push.** Presence checks run while Home is open and can lag. Public Game Details are required for playtime/presence.
6. **Steam metadata coverage is limited.** Developer, publisher, description, genre and official store artwork are not guaranteed for every title; absent values are intentionally omitted/fallbacked.
7. **Physical QA is outstanding.** Test RTL/LTR, font scaling, TalkBack, Android 8/API 26 behavior, Android 14 Galaxy A13 performance, and Android 16 enforced edge-to-edge behavior.
8. **Play Console compliance is not just targetSdk.** Complete the data safety form, privacy policy, app access instructions, content rating, screenshots, and pre-launch report before publishing.
9. **Backup does not bundle image bytes.** Game image URIs remain local references and may not be portable to another device.
10. **Official platform logos are not bundled.** Current platform labels use simplified marks.
