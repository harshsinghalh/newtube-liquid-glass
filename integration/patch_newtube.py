#!/usr/bin/env python3
"""Idempotently patch the supplied NewTube 1.15.0 source checkout."""
from pathlib import Path
import re
import shutil
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
activity = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/browse/MobileBrowseActivity.java"
layout = root / "smarttubetv/src/stmobile/res/layout/activity_mobile_browse.xml"
module = root / "smarttubetv/build.gradle"
top = root / "build.gradle"
for path in (activity, layout, module, top):
    if not path.is_file():
        raise SystemExit(f"Required NewTube file missing: {path}")

dest = root / "smarttubetv/src/stmobile/java/com/newtube/liquidglass"
dest.mkdir(parents=True, exist_ok=True)
shutil.copy2(Path(__file__).with_name("LiquidGlassBottomBar.kt"), dest / "LiquidGlassBottomBar.kt")
shutil.copy2(Path(__file__).parent / "src/main/kotlin/com/newtube/liquidglass/LiquidGlassNavBridge.kt",
             dest / "LiquidGlassNavBridge.kt")

a = activity.read_text()
if "LiquidGlassNavBridge" not in a:
    anchor = "import com.newtube.mobile.ui.playback.SystemPipBridge;\n"
    if anchor not in a:
        raise SystemExit("Activity import anchor not found")
    a = a.replace(anchor, anchor + """
import androidx.compose.ui.platform.ComposeView;
import com.qmdeve.liquidglass.widget.LiquidGlassView;
import com.newtube.liquidglass.LiquidGlassNavBridge;
""", 1)
    anchor = "    private BottomNavigationView mBottomNav;\n"
    if anchor not in a:
        raise SystemExit("BottomNavigationView field not found")
    a = a.replace(anchor, anchor + "    private LiquidGlassNavBridge mLiquidGlassNav;\n", 1)
    anchor = "        setupBottomNav();\n"
    if anchor not in a:
        raise SystemExit("setupBottomNav() call not found")
    a = a.replace(anchor, anchor + "        setupLiquidGlassNavigation();\n", 1)

    method = """    private void setupLiquidGlassNavigation() {
        LiquidGlassView glass = findViewById(R.id.mobile_liquid_glass_view);
        ComposeView overlay = findViewById(R.id.mobile_liquid_glass_overlay);
        mLiquidGlassNav = new LiquidGlassNavBridge(
                mContentSwipe, overlay, mBottomNav, mContentGrid, glass);
        mContentGrid.post(() -> {
            int bottom = mBottomNav.getHeight()
                    + getResources().getDimensionPixelSize(R.dimen.mobile_card_spacing);
            mContentGrid.setPadding(mContentGrid.getPaddingLeft(), mContentGrid.getPaddingTop(),
                    mContentGrid.getPaddingRight(), bottom);
        });
    }

"""
    anchor = "    private void setupBottomNav() {\n"
    if anchor not in a:
        raise SystemExit("setupBottomNav() method not found")
    a = a.replace(anchor, method + anchor, 1)

    selection = """            if (!mSuppressNavCallback) {
                onNavItemChosen(item.getItemId());
            }
            return true;"""
    if selection not in a:
        raise SystemExit("Nav selection listener not found")
    a = a.replace(selection, selection.replace(
        "            return true;",
        "            if (mLiquidGlassNav != null) mLiquidGlassNav.refreshFromMenu();\n            return true;"
    ), 1)

    reselection = """            } else {
                onNavItemChosen(item.getItemId());
            }
        });"""
    if reselection not in a:
        raise SystemExit("Nav reselection listener not found")
    a = a.replace(reselection, reselection.replace(
        "        });",
        "            if (mLiquidGlassNav != null) mLiquidGlassNav.refreshFromMenu();\n        });"
    ), 1)

    rebuild = """        rebuildBottomNav();
        if (mYouShowing) {"""
    if rebuild not in a:
        raise SystemExit("Navigation rebuild callback not found")
    a = a.replace(rebuild, """        rebuildBottomNav();
        if (mLiquidGlassNav != null) mLiquidGlassNav.refreshFromMenu();
        if (mYouShowing) {""", 1)

    teardown = "        mBottomNav.removeCallbacks(mAnnounceUpdateInstalled);\n"
    if teardown not in a:
        raise SystemExit("Activity teardown anchor not found")
    a = a.replace(teardown, teardown + "        if (mLiquidGlassNav != null) mLiquidGlassNav.dispose();\n", 1)
    activity.write_text(a)

x = layout.read_text()
if "mobile_liquid_glass_view" not in x:
    start = x.index('        android:id="@+id/mobile_content_swipe"')
    end = x.index("</com.newtube.mobile.ui.common.HapticSwipeRefreshLayout>", start)
    block = x[start:end]
    old_bottom = 'app:layout_constraintBottom_toTopOf="@id/mobile_bottom_nav"'
    if old_bottom not in block:
        raise SystemExit("Feed bottom constraint not found")
    block = block.replace(old_bottom, 'app:layout_constraintBottom_toBottomOf="parent"', 1)
    x = x[:start] + block + x[end:]

    glass_views = """    <!-- Native refraction samples the live View hierarchy below this dock. -->
    <com.qmdeve.liquidglass.widget.LiquidGlassView
        android:id="@+id/mobile_liquid_glass_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginHorizontal="8dp"
        android:layout_marginVertical="2dp"
        android:elevation="4dp"
        app:layout_constraintBottom_toBottomOf="@id/mobile_bottom_nav"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="@id/mobile_bottom_nav" />

    <!-- Compose Backdrop selection lens; original Material navigation remains above it. -->
    <androidx.compose.ui.platform.ComposeView
        android:id="@+id/mobile_liquid_glass_overlay"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@android:color/transparent"
        android:clickable="false"
        android:focusable="false"
        android:importantForAccessibility="no"
        android:elevation="6dp"
        app:layout_constraintBottom_toBottomOf="@id/mobile_bottom_nav"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="@id/mobile_bottom_nav" />

"""
    anchor = '    <com.google.android.material.bottomnavigation.BottomNavigationView'
    if anchor not in x:
        raise SystemExit("BottomNavigationView layout node not found")
    x = x.replace(anchor, glass_views + anchor, 1)
    x = x.replace('android:background="?attr/newtubeColorBackground"',
                  'android:background="@android:color/transparent"\n        android:elevation="8dp"', 1)
    layout.write_text(x)

g = top.read_text()
if "compose-compiler-gradle-plugin" not in g:
    anchor = "classpath 'com.android.tools.build:gradle:9.2.1'"
    if anchor not in g:
        raise SystemExit("Unexpected Android Gradle Plugin pin")
    top.write_text(g.replace(anchor, anchor + "\n        classpath 'org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.3.21'", 1))

m = module.read_text()
if "org.jetbrains.kotlin.plugin.compose" not in m:
    anchor = "apply plugin: 'com.android.application'"
    if anchor not in m:
        raise SystemExit("Android application plugin not found")
    m = m.replace(anchor, anchor + "\napply plugin: 'org.jetbrains.kotlin.plugin.compose'", 1)
if "compose = true" not in m:
    match = re.search(r"buildFeatures\s*\{(?P<body>[^}]*)\}", m, re.S)
    if not match:
        raise SystemExit("buildFeatures block not found")
    m = m[:match.start()] + "buildFeatures {" + match.group("body") + "\n        compose = true\n    }" + m[match.end():]
if "com.qmdeve.liquidglass:core:1.1.0" not in m:
    m += """
// NEWTUBE(liquid-glass): native Android refraction and Compose Multiplatform lens.
dependencies {
    implementation 'org.jetbrains.compose.ui:ui:1.11.0'
    implementation 'org.jetbrains.compose.foundation:foundation:1.11.0'
    implementation 'org.jetbrains.compose.animation:animation:1.11.0'
    implementation 'io.github.kyant0:backdrop:2.0.0'
    implementation 'io.github.kyant0:shapes:1.2.0'
    implementation 'com.qmdeve.liquidglass:core:1.1.0'
}
"""
    module.write_text(m)

print(f"Applied NewTube Liquid Glass integration to {root}")
