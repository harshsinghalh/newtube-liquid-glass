#!/usr/bin/env python3
"""Apply the liquid-glass nav integration to a clean NewTube checkout. Idempotent by design."""
from pathlib import Path
import re, shutil, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
activity = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/browse/MobileBrowseActivity.java"
layout = root / "smarttubetv/src/stmobile/res/layout/activity_mobile_browse.xml"
module_gradle = root / "smarttubetv/build.gradle"
root_gradle = root / "build.gradle"
shared_constants = root / "SharedModules/constants.gradle"
for p in (activity, layout, module_gradle, root_gradle):
    if not p.exists():
        raise SystemExit(f"Required NewTube source file missing: {p}")

# The upstream branch declares compileSdk 37, which is not yet published in the
# Android SDK repository used by hosted runners. Compile this debug verification build
# against API 36 while retaining upstream targetSdk behavior; do not commit this override
# back to the upstream project.
if shared_constants.exists():
    constants = shared_constants.read_text()
    constants = constants.replace("compileSdkVersion = 37", "compileSdkVersion = 36", 1)
    shared_constants.write_text(constants)

# Copy bridge + component into the actual NewTube mobile source set.
dest = root / "smarttubetv/src/stmobile/java/com/newtube/liquidglass"
dest.mkdir(parents=True, exist_ok=True)
shutil.copy2(Path(__file__).parent / "LiquidGlassBottomBar.kt", dest / "LiquidGlassBottomBar.kt")
shutil.copy2(Path(__file__).parent / "src/main/kotlin/com/newtube/liquidglass/LiquidGlassNavBridge.kt", dest / "LiquidGlassNavBridge.kt")

s = activity.read_text()
if "LiquidGlassNavBridge" not in s:
    s = s.replace("import androidx.annotation.Nullable;\n", "import androidx.annotation.Nullable;\nimport androidx.compose.ui.platform.ComposeView;\n")
    s = s.replace("import com.newtube.mobile.ui.playback.SystemPipBridge;\n",
                  "import com.newtube.mobile.ui.playback.SystemPipBridge;\nimport com.newtube.liquidglass.LiquidGlassNavBridge;\n")
    s = s.replace("    private BottomNavigationView mBottomNav;\n",
                  "    private BottomNavigationView mBottomNav;\n    private LiquidGlassNavBridge mLiquidGlassNav;\n")
    s = s.replace("        setupBottomNav();\n", "        setupBottomNav();\n        setupLiquidGlassNav();\n", 1)
    s = s.replace("""            if (!mSuppressNavCallback) {
                onNavItemChosen(item.getItemId());
            }
            return true;""", """            if (!mSuppressNavCallback) {
                onNavItemChosen(item.getItemId());
            }
            if (mLiquidGlassNav != null) mLiquidGlassNav.refreshFromMenu();
            return true;""", 1)
    s = s.replace("""            } else {
                onNavItemChosen(item.getItemId());
            }
        });""", """            } else {
                onNavItemChosen(item.getItemId());
            }
            if (mLiquidGlassNav != null) mLiquidGlassNav.refreshFromMenu();
        });""", 1)
    anchor = "    private void setupBottomNav() {\n"
    method = """    private void setupLiquidGlassNav() {
        mLiquidGlassNav = new LiquidGlassNavBridge(
                findViewById(R.id.mobile_browse_root),
                findViewById(R.id.mobile_liquid_glass_nav),
                mBottomNav,
                mContentGrid);
    }

"""
    if anchor not in s: raise SystemExit("Could not locate setupBottomNav() anchor")
    s = s.replace(anchor, method + anchor, 1)
    old = """            mSuppressNavCallback = false;
            return;
        }
        mNavSignature = newSignature;"""
    new = """            mSuppressNavCallback = false;
            if (mLiquidGlassNav != null) mLiquidGlassNav.refreshFromMenu();
            return;
        }
        mNavSignature = newSignature;"""
    if old not in s: raise SystemExit("Could not locate unchanged-navigation branch")
    s = s.replace(old, new, 1)
    old = """        mSuppressNavCallback = false;

        refreshUpdateBadge();"""
    new = """        mSuppressNavCallback = false;
        if (mLiquidGlassNav != null) mLiquidGlassNav.refreshFromMenu();

        refreshUpdateBadge();"""
    if old not in s: raise SystemExit("Could not locate navigation rebuild completion")
    s = s.replace(old, new, 1)
    activity.write_text(s)

x = layout.read_text()
if "mobile_liquid_glass_nav" not in x:
    x = x.replace('        android:layout_height="wrap_content"\n        android:background="?attr/newtubeColorBackground"\n        app:itemIconSize="24dp"',
                  '        android:layout_height="wrap_content"\n        android:visibility="invisible"\n        android:background="?attr/newtubeColorBackground"\n        app:itemIconSize="24dp"', 1)
    marker = "</androidx.constraintlayout.widget.ConstraintLayout>"
    overlay = """
    <!-- Compose overlay: legacy BottomNavigationView remains the source of truth for selection,
         reselect, deep links, badges, and long-press actions. -->
    <androidx.compose.ui.platform.ComposeView
        android:id="@+id/mobile_liquid_glass_nav"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:elevation="16dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent" />

"""
    if marker not in x: raise SystemExit("Could not locate root layout closing tag")
    x = x.replace(marker, overlay + marker, 1)
    layout.write_text(x)

# Compose compiler plugin + libraries. AGP 9.x provides built-in Kotlin; use matching compiler plugin.
g = root_gradle.read_text()
if "compose-compiler-gradle-plugin" not in g:
    g = g.replace("classpath 'com.android.tools.build:gradle:9.2.1'",
                  "classpath 'com.android.tools.build:gradle:9.2.1'\n        classpath 'org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.21'", 1)
    root_gradle.write_text(g)
m = module_gradle.read_text()
if "org.jetbrains.kotlin.plugin.compose" not in m:
    m = m.replace("apply plugin: 'com.android.application'",
                  "apply plugin: 'com.android.application'\napply plugin: 'org.jetbrains.kotlin.plugin.compose'", 1)
if "compose = true" not in m:
    m = m.replace("    buildFeatures {\n        buildConfig = true\n    }",
                  "    buildFeatures {\n        buildConfig = true\n        compose = true\n    }", 1)
if "io.github.kyant0:backdrop:2.0.0" not in m:
    # Add to the existing module dependency block at the end, avoiding assumptions about formatting.
    m += """

// NEWTUBE(liquid-glass): Compose overlay for the mobile bottom navigation.
dependencies {
    implementation platform('androidx.compose:compose-bom:2026.06.01')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.foundation:foundation'
    implementation 'androidx.compose.animation:animation'
    implementation 'io.github.kyant0:backdrop:2.0.0'
    implementation 'io.github.kyant0:shapes:1.2.0'
}
"""
module_gradle.write_text(m)
print("Liquid-glass integration patch applied to", root)
