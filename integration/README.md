# Compose-side integration notes

The reusable component lives at `src/main/kotlin/com/newtube/liquidglass/LiquidGlassBottomBar.kt`.

## Required dependencies

For a host Android module that already has Compose enabled, add the same library used by the uploaded project:

```kotlin
dependencies {
    implementation("io.github.kyant0:backdrop:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
    implementation("androidx.activity:activity-compose:<version-compatible-with-host>")
    implementation("androidx.compose.foundation:foundation:<version-compatible-with-host>")
    implementation("androidx.compose.ui:ui:<version-compatible-with-host>")
    implementation("androidx.compose.animation:animation:<version-compatible-with-host>")
}
```

Use the Compose and Activity versions compatible with the host NewTube Gradle toolchain rather than blindly copying versions from the standalone demo project. The Kotlin Compose compiler plugin must also be enabled in the host module.

## Host contract

- The host owns the selected destination and passes `selectedIndex`.
- `onItemSelected(index)` must call the existing NewTube navigation handler.
- Preserve the existing curated destination order and the synthetic You tab.
- The `Backdrop` passed to the component must record content in the same Compose hierarchy. Backdrop's shader cannot directly sample arbitrary Android View siblings. In the current NewTube View-based screen, this is the main integration step still required; the component alone does not replace `BottomNavigationView`.

## Verification status

The component source has been committed to this repository, but no Android build has been run in this environment. No APK is produced by this source-only integration workspace yet.
