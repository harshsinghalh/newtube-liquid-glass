# NewTube liquid-glass integration

The integration patch applies to a clean checkout of https://github.com/aleixrodriala/newtube.

## Files

- `LiquidGlassBottomBar.kt`: the Backdrop-based Compose glass surface copied into NewTube's mobile source set.
- `src/main/kotlin/com/newtube/liquidglass/LiquidGlassNavBridge.kt`: Android View snapshot bridge and state adapter.
- `patch_newtube.py`: idempotent source patch for the Activity, XML layout, Gradle plugin, and dependencies.

## Integration design

NewTube's existing Material BottomNavigationView stays in the Activity and continues to own selected menu IDs, selected/reselected listeners, deep-link destinations, and its existing navigation callbacks. It is hidden visually and a ComposeView overlays the same dock region. Compose taps set the legacy menu's selectedItemId, so the Activity's existing listeners remain responsible for routing.

Backdrop cannot directly sample arbitrary Android View siblings. The bridge therefore renders the actual NewTube root View into a cropped bitmap matching the ComposeView's bounds, with the Compose overlay transparent during capture. It supplies that ImageBitmap through `rememberCanvasBackdrop` and refreshes on feed scrolling, layout changes, and menu changes. This is a live-updated snapshot bridge rather than a direct GPU backdrop capture of Android Views.

## Dependency compatibility

NewTube's SharedModules constants declare Kotlin 2.3.21. The patch uses the matching Compose compiler plugin, Backdrop 2.0.0, Shapes 1.2.0, and Compose BOM 2026.06.01 rather than the newer 2.0.1/1.2.1 artifacts built with Kotlin 2.4.10.

## Build and artifact

The repository workflow checks out upstream NewTube with its submodules, runs this patch, builds `:smarttubetv:assembleStmobileDebug`, checks the APK archive, records SHA-256 checksums, and uploads the APK(s) as a GitHub Actions artifact. Download the artifact from the latest successful run at:

https://github.com/harshsinghalh/newtube-liquid-glass/actions/workflows/build-newtube-apk.yml

The artifact is not available until the build succeeds. CI archive verification is not a substitute for runtime/device testing.
