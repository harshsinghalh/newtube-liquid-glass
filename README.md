# NewTube Liquid Glass

Experimental integration workspace for applying the [AndroidLiquidGlass-kmp](https://github.com/kyant0/Backdrop) Backdrop effect to NewTube's phone navigation.

## Upstream
- App source: https://github.com/aleixrodriala/newtube
- Existing navigation: `smarttubetv/src/stmobile/java/com/newtube/mobile/ui/browse/MobileBrowseActivity.java` and `smarttubetv/src/stmobile/res/layout/activity_mobile_browse.xml`
- Library: `io.github.kyant0:backdrop:2.0.1`

## Important integration constraint
NewTube currently implements its navigation with Android Views/Material Components. Backdrop's lens/blur pipeline captures Compose layers, not arbitrary sibling Android Views. A correct full-app integration must put the relevant background and glass bar in a shared Compose layer (or explicitly bridge a background snapshot); simply replacing the XML widget with a ComposeView does not make the effect refract the RecyclerView behind it.

The component in this repository is the Compose-side navigation surface. It is not yet wired into NewTube's Activity, and this repository does not yet contain a built APK. The build must be run against the upstream NewTube checkout before treating the integration as complete.
