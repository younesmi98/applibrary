<<<<<<< HEAD
=======
> Partly superseded by docs/PRODUCT_REWORK.md: the session timer, Wrapped and PC items mentioned below were removed in 0.2.0, and the palette is now emerald.

>>>>>>> 7d946ca (NEXUS 0.3.0 redesign and release preparation)
# Design pass (iOS-style, glass) — what is real and what is not

Nothing in this pass has been compiled or run. The build environment that produced it has no Android SDK, no Kotlin compiler and no network.
Only brace/parenthesis balance and duplicate function names were checked by script.

## Libraries
| Library | Used | Why |
|---|---|---|
| Haze `dev.chrisbanes.haze:haze:0.7.3` | Yes | Real backdrop blur for the floating tab bar. 0.7.x was built with Kotlin 1.9.23 and Compose 1.6, which matches this project (Kotlin 1.9.24, Compose BOM 2024.06). Haze 0.9 and later need Compose 1.7, so they were not used. Only `Modifier.haze(state)` and `Modifier.hazeChild(state, shape)` are called; these two calls were checked against Haze documentation, not against a compiler. Blur works on Android 12+ at effects level High; below that a denser frosted tint is drawn. |
| Compose Shimmer | No | A 20-line `Modifier.shimmer()` in Components.kt does the same job without another dependency. |
| compose-cupertino | No | It replaces Material components wholesale. Segmented control, grouped rows, large titles and spring press feedback were written directly. |
| LiquidGlass (shayann07), PrismalAGSL | No | PrismalAGSL needs Kotlin 2.2 and compileSdk 37. LiquidGlass duplicates what Haze gives for this use. |
| Lottie | No | No animation here needed it. |
| BlurView | No | It is for the View system; this app is Compose. |

## Fonts
Vazirmatn (Persian) and Inter (English), weights 400/500/600/700, SIL OFL 1.1.
The font files are NOT inside this zip: the environment could not download them. Instead `tools/fetch_fonts.sh` downloads the official releases
(Vazirmatn v33.003, latest Inter) into `app/src/main/assets/fonts/` together with the OFL texts, and the GitHub workflow runs it before each build,
so the built APK contains the fonts. The workflow also uploads the fonts as an artifact named `nexus-fonts`; commit those files if you want them in the repository.
`Fonts.kt` only uses files that exist, so a failed download falls back to the system font instead of crashing. Settings shows whether each font is bundled.

## Done in code
- Library screen after the reference image: search, title with Add button, segmented filter (All / Playing / Done / Favorites), grid/list switch, Filters and Sort, 3-column cards with platform mark, working heart, status dot and recorded hours. The avatar and bell from the reference image were left out because they would be buttons that do nothing.
- Platform marks on cards, list rows and the game page. They are simplified letter marks, not official logos.
- Game page with Overview / Statistics / Media tabs (tab state survives; the ViewModel is not recreated). Media: replace/remove cover and background, gallery stored in a new Room table `game_images` (migration 5 to 6, picker URIs kept with persisted read grants).
- Statistics tab: four playtime sources side by side (never summed), bars per source and per recent session, history, adjustments, Windows rule.
- Floating tab bar with real blur (when allowed), sliding indicator and icon scale on a spring, RTL-aware.
- Skeleton loading with a shimmer for Home, Library, Stats, Detail and list screens. Static when motion is reduced.
- Typography per language (Inter / Vazirmatn) with taller Persian lines. Settings: grouped rows, segmented controls, effects level and Reduce motion now apply immediately.
- Press feedback (spring shrink) on glass buttons.

## Not done
- Blur on dialogs and bottom sheets: they are separate windows, so Haze cannot blur what is behind them.
- Blur on Home panels: they scroll with the content they would blur, so they use a frosted tint.
- Screenshots in either language. Compile, unit-test run, emulator checks, Galaxy A13 performance check.
- New unit tests for the gallery table and migration.
- Official platform logos.
