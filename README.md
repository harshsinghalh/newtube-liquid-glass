# NewTube Liquid Glass

A reproducible integration workspace that patches the official NewTube mobile Activity with the AndroidLiquidGlass Backdrop library, preserves NewTube's existing BottomNavigationView as the navigation state machine, and layers a Compose glass bar over it.

## Source and implementation

- Upstream NewTube: https://github.com/aleixrodriala/newtube
- Activity: **smarttubetv/src/stmobile/java/com/newtube/mobile/ui/browse/MobileBrowseActivity.java**
- Layout: **smarttubetv/src/stmobile/res/layout/activity_mobile_browse.xml**
- Glass component: [LiquidGlassBottomBar.kt](integration/LiquidGlassBottomBar.kt)
- Android View-to-Compose snapshot bridge: [LiquidGlassNavBridge.kt](integration/src/main/kotlin/com/newtube/liquidglass/LiquidGlassNavBridge.kt)
- Repeatable source patch: [patch_newtube.py](integration/patch_newtube.py)

## How the integration works

The existing Material navigation view remains the source of truth for selected menu IDs and continues to own NewTube's tab selection and reselection callbacks. It is made invisible and a ComposeView overlay renders the liquid-glass surface. The bridge snapshots the real Android View hierarchy behind the bar into a cropped bitmap and supplies that image to Backdrop; it refreshes when the feed scrolls, the layout changes, or navigation items change. This is a snapshot bridge, not a claim that Backdrop directly samples Android View siblings.

The dependency versions are pinned to NewTube's Kotlin 2.3.21 toolchain: Backdrop 2.0.0, Shapes 1.2.0, and Compose BOM 2026.06.01.

## Build and download

The [GitHub Actions workflow](.github/workflows/build-newtube-apk.yml) checks out upstream NewTube and its submodules, applies the patch, runs the NewTube mobile debug build, checks APK ZIP integrity, records SHA-256 checksums, and uploads APK(s) as the **newtube-liquid-glass-debug-apk** artifact.

Open [Actions](https://github.com/harshsinghalh/newtube-liquid-glass/actions/workflows/build-newtube-apk.yml), select the latest successful run, and download its artifact. The artifact is only available after the workflow succeeds. Debug APKs are for testing and are not signed with the upstream release key.

## Verification

A successful CI run confirms source patch checks, Gradle compilation/package assembly, and APK archive integrity. It does not replace runtime testing on a physical device. If a run fails, its logs are linked from Actions.
