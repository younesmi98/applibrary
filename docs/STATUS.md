# NEXUS implementation status

Nothing here has been compiled or run. The build environment used to write it has no Android SDK, no Kotlin compiler and no network.
Only a brace/parenthesis balance check was run on the Kotlin files.

## What changed in this merge
- All phase folders now form one Gradle project (`:app`). Phase folders are compiled together as source sets of the app module.
- NexusDatabase now lists every entity from phases 2-5 (before this, playtime, Steam and PC entities were never registered). Version 4, migrations 1->2, 2->3, 3->4 registered.
- Added: app module, manifest, theme, launcher icon, Application class, MainActivity.
- Real screens: Library (paged, search, source/status filters, sort), Add Game (one screen, cover picker), Game Detail (status, favorite, session timer, notes, playtime by source, adjustments with undo, session history, delete library entry), About (honest feature list).
- SessionActionReceiver written (notification Pause/Resume/Finish). Recovery dialog for unfinished sessions on app start.
- Fixed: kotlin-test dependency for the playtime test; ServiceCompat.startForeground for older API levels.
- GitHub Actions workflow builds the debug APK and runs unit tests: `.github/workflows/build.yml`.

## Added in the second pass (all uncompiled)
- Screens: More hub, Stats, Wrapped (share as PNG), Play Next, Backlog assistant, Collections, Wishlist, Journal, Import from file (Epic JSON, GOG/other CSV, with review step), Backup and export (JSON, sessions CSV, restore that only adds), PC devices, Settings with effects level and reduce motion.
- Detail page now has collections, journal and Windows tracking rule panels. Library has a collection filter.
- Database v5: wishlist and journal tables, InsightsDao, migration 4->5.
- insights and polish modules now read/write Room through InsightsLoader and BackupData.
- Wrapped image export through FileProvider.

## Third pass: design and Persian (all uncompiled)
- Whole UI rewritten around the uiSKILL rules: artwork-first library grid (adaptive columns, list toggle), hero detail page, Home dashboard, bottom navigation (Home, Library, Stats, More), sentence-case headings, sections instead of boxes (one emphasized block per screen), explicit type scale, 48dp touch targets, generated cover colours for games without artwork.
- Persian everywhere in the app UI: `tr("English", "فارسی")` at every string, RTL layout, Jalali dates and Persian digits (ICU), language switch in Settings (Automatic / English / فارسی), notification text localized, Wrapped image drawn right-to-left in Persian. Back arrows mirror in RTL.
- Notification now opens the app on tap and uses the app icon.

## Known gaps in this pass
- No Vazirmatn or Inter font files are bundled (no network here); the system font is used. Persian shaping depends on the phone font.
- Error text from the file parsers and backup decoder stays English.
- Wishlist dates are typed in Gregorian (YYYY-MM-DD).
- Contrast of the colour pairs was chosen by eye, not measured. No screen reader, zoom or tablet testing.

## Not done
- PcRelay (Supabase or LAN) and the Windows tray app. Pairing codes are only generated locally.
- Steam sign-in, network client and sync screen.
- Metadata search, artwork download, duplicate merge, smart collections, game comparison, achievements UI.
- Not stored, so related charts and modes stay empty: genres, hours to beat, achievement percent, completion dates.
- No new unit tests for the Room-backed code.
