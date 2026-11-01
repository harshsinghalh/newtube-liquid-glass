# NewTube Liquid Glass

A reproducible build of NewTube 1.15.0 with a dual-engine Liquid Glass bottom navigation, based on the NewTube APK and the two Android Liquid Glass libraries supplied for this task.

## What changes

- Preserves NewTube's existing Material bottom navigation, original icons, labels, badges, accessibility and navigation behavior.
- Adds a live native refraction/dispersion dock using **AndroidLiquidGlassView 1.1.0** (Android 13+).
- Adds a spring-animated selected-tab lens using **Backdrop 2.0.0 / Shapes 1.2.0** from Compose Multiplatform.
- Allows the feed to scroll behind the translucent dock and adds enough trailing content padding to keep the last card reachable.
- Uses a translucent fallback surface on older Android versions where the native shader library does not render effects.

## Pinned source

The APK supplied for this task contains NewTube source revision `756e7e56acba61ccdf5b4d5a802963f0eb62616c`, NewTube 1.15.0. The build workflow checks out that exact source revision plus its submodules before applying the integration patch.

## Build and downloads

The workflow in `.github/workflows/build-newtube-apk.yml` builds `:smarttubetv:assembleStmobileDebug`, verifies the generated APK(s), creates SHA-256 checksums and publishes both an Actions artifact and a GitHub prerelease.

- [Download from GitHub Releases](https://github.com/harshsinghalh/newtube-liquid-glass/releases)
- [Build history and logs](https://github.com/harshsinghalh/newtube-liquid-glass/actions/workflows/build-newtube-apk.yml)

The build output is debug-signed for testing and is not signed with the upstream release key. Read `BUILD-INFO.txt` and verify `SHA256SUMS.txt` before installation.

## Integration layout

- `integration/LiquidGlassBottomBar.kt` — Compose selection lens.
- `integration/src/main/kotlin/com/newtube/liquidglass/LiquidGlassNavBridge.kt` — Android-to-Compose bridge and native refraction view setup.
- `integration/patch_newtube.py` — reproducible source integration script.
- `integration/README.md` — integration and licensing notes.
