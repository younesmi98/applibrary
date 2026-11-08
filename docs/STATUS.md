# NEXUS 0.3.0 source status

This is a release-candidate source update, not a verified store release. The current package includes the eight-spoke iOS-style startup loader, Android 16 target preparation, edge-to-edge safe insets, and release signing configuration via environment variables.

## Included

- Graphite/emerald Compose theme and shared components.
- Home, Library, game details, add/import, platform connections, statistics, More and Settings screens.
- Grid/List, search, platform/status filters, sorting, favorites and collections.
- Steam Web API client for owned games, recent playtime, player summary and current-game presence; Steam key is stored through Android Keystore-backed encrypted preferences.
- Offline Room library and Coil artwork caching.
- Manual game metadata entry and file-based importers.
- Eight-spoke splash animation that draws the rotating/fading spokes directly on Canvas; respects the in-app Reduce Motion setting.
- Android 16/API 36 compile/target configuration and edge-to-edge safe content layout.
- Optional release signing configured from environment variables; signing key is not stored in this source archive.

## Known limitations / not release-verified

<<<<<<< HEAD
## Fourth pass: match the design sheet, iOS feel (uncompiled)
- First on-device check (screenshots from the user): the debug APK builds and runs, Persian and English both show, RTL works, bottom bar works.
- Restyled after `nexus-screens.html`: graphite #141415, one teal accent #3CC8A8, soft teal/blue glow at the top of each page, glass blocks (translucent, no real blur), floating rounded bottom bar with thin line icons, 3-column posters with a small mono caption, tone-block covers that carry the game title (no big letter), hairline-separated value rows, text filters with a teal underline.
- iOS-style behaviour: large titles, centered back chevron (mirrors in RTL), grouped rounded rows with chevrons in More, 12-tick spinner for loading, spring slide transitions for pushed screens (direction follows RTL), fade between tabs.
- Add Game reduced to what is explained: cover, title, developer, year, status. Source is always Manual. Removed the unexplained source list, original title, tags and executable fields from that screen (executable rule is still in the game page).
- Library: "+" button in the title row replaces the floating button.

## Fifth pass: iOS-style redesign with real blur, fonts, tabs (uncompiled)
See docs/DESIGN_PASS.md for the library decisions, fonts and the exact list of what is done and not done. Database is now version 6.

## Known gaps in this pass
- No Vazirmatn or Inter font files are bundled (no network here); the system font is used. Persian shaping depends on the phone font.
- Error text from the file parsers and backup decoder stays English.
- Wishlist dates are typed in Gregorian (YYYY-MM-DD).
- Contrast of the colour pairs was chosen by eye, not measured. No screen reader, zoom or tablet testing.
=======
- No build or unit tests could be executed in this environment because Gradle and Android SDK are absent and outbound DNS is unavailable. Run the CI workflow before distributing an APK.
- This archive does not include a signed APK. A private keystore is required.
- Epic is file-import only (Legendary/Heroic/CSV); no supported live Epic account sync.
- The PC relay and Windows companion do not exist in this package. Manual entries can be organized, but Windows executable launch from the Android phone is not end-to-end implemented.
- No full physical-device test on Galaxy A13 or Play Console review has been run.
- Steam current-game presence is polling-based, requires public Game Details, and can be up to about one minute behind.
- Steam provides total and recent playtime, not full historical day-by-day activity; charts only reflect snapshots collected by NEXUS syncs.
- Artwork files are not included in the JSON backup.
- Platform badges are generic marks rather than official licensed logos.
>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)

See `docs/BUILD_AND_RELEASE.md` for exact toolchain and signing steps, and `docs/KNOWN_ISSUES.md` for the remaining work.
