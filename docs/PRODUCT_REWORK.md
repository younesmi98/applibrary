> Historical design/product notes from the 0.2.0 rework. The current source status and remaining release gates are documented in `docs/STATUS.md` and `docs/KNOWN_ISSUES.md`.

# NEXUS 0.2.0 product rework

Status of this package: **not compiled, not run, no tests executed.** The environment that produced it has no Android SDK, no Kotlin compiler and no network.
Checks that were run: brace balance, duplicate function names, a scan for leftover references to removed code. Nothing else.
(A GitHub Actions run of an earlier version of this project, after the owner's own fixes, built an APK and ran the unit tests. The owner's fixed sources were not available here, so this package was built from the last version produced here and may re-introduce errors the owner already fixed. Please compare against your repository before replacing it.)

## What NEXUS is now
A library of games from your platforms. It reads and shows; it does not ask you to log play time.

## Removed
- Session timer, Start/Pause/Finish, manual playtime adjustments, session history, session recovery dialog, foreground service, notification, notification permission.
- Wrapped (it depended on dated sessions), PC devices screen and Windows tracking rule (the Windows companion was never built).
- Database: the three session entities were removed from Room. Their tables are **left in the database file untouched** (no DROP), so nothing is lost. A game that was set to Playing by hand became Backlog (migration 6 to 7). "Playing" is now shown only when Steam itself reports a running game in the last 10 minutes.

## Added
- Platform data on each source: total playtime, last two weeks, last played, last sync (nullable: no figure is not the same as 0). Steam snapshots are kept per sync, so a per-game history grows with each sync.
- Steam provider using the documented Web API (GetOwnedGames, GetRecentlyPlayedGames, GetPlayerSummaries, ResolveVanityURL) directly from the phone with **your own Web API key**. The key is typed in, stored encrypted with an Android Keystore key, never in the APK and never logged. States: not connected, connected, syncing, failed, needs access. Private profile / private game details are reported as such, not as an empty library. Offline keeps the saved library and shows it.
- Epic: not available. Epic has no official way for a third-party app to read a library or playtime; unofficial login would put the account at risk. The Connections screen says so and offers the file import. Adding a platform later means adding another provider next to `PlatformSync`.
- Home dashboard: local profile header, search, sync; Now Playing (only when Steam reports it); Recently played; Library overview (per platform, total hours as reported); Favorites; Continue exploring. Real Statistics from platform data only.
- Library: platform filter, Recently played / Backlog / Completed / Favorites, sort by name, playtime, last played, added, rating; default view and sort in settings; platform badges and hours on cards.
- Add game: Sync from Steam / Epic (explained) / Import from file / Add manually (cover, title, developer, publisher, year, genre, description, platform).
- More: Account (Profile, Connected platforms), Library, Appearance (language, accent colour, background, effects, motion, fonts), Data (backup, restore, library CSV, clear image cache), About (version, changelog, licenses, build information).
- Emerald graphite palette, readable default text colour (the black-text bug came from text drawn outside a Surface), glow without a hard edge, cover cache (200 MB disk, 15% memory), Steam cover falls back to the store header image, then to a tone block.
- Back stack so Back returns to where you came from; screen state survives rotation (configChanges).

## Limits that remain
- Steam shows hours and the running game only if the profile's Game details are public. The running game is as fresh as the last check (every minute while Home is open); there is no push.
- Steam gives totals, not days, so there is no day-by-day chart. History comes from your own syncs.
- Developer, publisher, description and genre are not fetched for Steam games (the Steam store details endpoint is undocumented, so it was not used). They show only when you entered them.
- Platform marks are simplified letter marks, not official logos. Persian/English font files are downloaded by the CI workflow, not stored in this zip.
- Official Steam sign-in (OpenID) would need a backend; the backend folder from an earlier phase is not wired.
- Accent colours other than emerald, and dark-only theme: there is no light theme.
- No unit tests were run. New tests written but not executed: SteamJsonTest (parser: real zero vs missing, private profile, broken JSON, presence, vanity).

## Build
Upload the project to GitHub and run the "Build NEXUS debug APK" workflow (it downloads the fonts, builds, runs unit tests and uploads the APK and test reports). Locally: `gradle :app:assembleDebug` and `gradle :app:testDebugUnitTest` with Gradle 8.7, JDK 17 and Android SDK 34, after `tools/fetch_fonts.sh`.

## Steam setup
Steam privacy: Profile and Game details must be Public. Get a key at steamcommunity.com/dev/apikey. In NEXUS: More > Connected platforms, enter your SteamID64, profile link or profile name, paste the key, Connect and sync.
