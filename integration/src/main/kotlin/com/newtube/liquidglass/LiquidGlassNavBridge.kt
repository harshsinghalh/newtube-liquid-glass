package com.newtube.liquidglass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.qmdeve.liquidglass.widget.LiquidGlassView

/**
 * Uses AndroidLiquidGlassView to refract the live Android feed and Backdrop to animate the
 * selected tab lens. The original BottomNavigationView remains the sole navigation model.
 */
class LiquidGlassNavBridge(
    private val source: ViewGroup,
    private val overlay: ComposeView,
    private val nav: BottomNavigationView,
    private val feed: RecyclerView,
    private val nativeGlass: LiquidGlassView
) {
    private val selected = mutableIntStateOf(0)
    private val count = mutableIntStateOf(0)
    private val image = mutableStateOf<ImageBitmap?>(null)
    private val inset = mutableIntStateOf(0)
    private var previousId = Int.MIN_VALUE
    private var previousCount = -1
    private var disposed = false

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = capture()
    }
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        overlay.post {
            refreshFromMenu()
            updateGlassSize()
            capture()
        }
    }

    init {
        overlay.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        overlay.setBackgroundColor(Color.TRANSPARENT)
        overlay.isClickable = false
        overlay.isFocusable = false
        overlay.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        val density = nativeGlass.resources.displayMetrics.density
        val dark = (nativeGlass.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        nativeGlass.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 36f * density
            setColor(if (dark) Color.argb(88, 25, 29, 38) else Color.argb(146, 245, 248, 253))
            setStroke((density).toInt().coerceAtLeast(1),
                if (dark) Color.argb(70, 255, 255, 255) else Color.argb(90, 255, 255, 255))
        }
        nativeGlass.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        nativeGlass.isClickable = false
        nativeGlass.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        nativeGlass.setCornerRadius(36f * density)
        nativeGlass.setRefractionHeight(20f * density)
        nativeGlass.setRefractionOffset(66f * density)
        nativeGlass.setDispersion(0.34f)
        nativeGlass.setBlurRadius(1.15f)
        nativeGlass.setTintAlpha(0.045f)
        nativeGlass.bind(source)

        // Preserve native icons, labels, badges and click targets; only remove the M3/M2 pill
        // so the Backdrop-rendered selection lens is the single highlighted state.
        nav.setBackgroundColor(Color.TRANSPARENT)
        nav.setItemActiveIndicatorEnabled(false)

        overlay.setContent {
            val currentImage = image.value
            val backdrop = rememberCanvasBackdrop {
                currentImage?.let { drawImage(it) }
            }
            LiquidGlassBottomBar(
                selectedIndex = selected.intValue,
                tabCount = count.intValue,
                backdrop = backdrop,
                bottomInset = with(androidx.compose.ui.platform.LocalDensity.current) {
                    inset.intValue.toDp()
                }
            )
        }

        feed.addOnScrollListener(scrollListener)
        source.addOnLayoutChangeListener(layoutListener)
        overlay.addOnLayoutChangeListener(layoutListener)
        nav.addOnLayoutChangeListener(layoutListener)
        nativeGlass.addOnLayoutChangeListener(layoutListener)
        overlay.post {
            refreshFromMenu()
            updateGlassSize()
            capture()
        }
    }

    /** Refresh after navigation callbacks or a dynamic BrowsePresenter menu rebuild. */
    fun refreshFromMenu() {
        if (disposed) return
        val size = nav.menu.size()
        val id = nav.selectedItemId
        val systemInset = if (android.os.Build.VERSION.SDK_INT >= 30) {
            nav.rootWindowInsets?.getInsets(
                android.view.WindowInsets.Type.navigationBars()
            )?.bottom ?: 0
        } else {
            @Suppress("DEPRECATION")
            nav.rootWindowInsets?.systemWindowInsetBottom ?: 0
        }
        if (inset.intValue != systemInset) inset.intValue = systemInset
        if (id == previousId && size == previousCount) return

        previousId = id
        previousCount = size
        count.intValue = size
        selected.intValue = (0 until size)
            .firstOrNull { nav.menu.getItem(it).itemId == id } ?: 0
    }

    private fun updateGlassSize() {
        if (disposed || nativeGlass.height <= 0) return
        nativeGlass.setCornerRadius(nativeGlass.height / 2f)
    }

    /** Capture only the bottom-dock-sized region rather than allocating a full-screen bitmap. */
    private fun capture() {
        if (disposed || !overlay.isAttachedToWindow ||
            overlay.width <= 0 || overlay.height <= 0 ||
            source.width <= 0 || source.height <= 0) return

        val sourceAt = IntArray(2)
        val overlayAt = IntArray(2)
        source.getLocationInWindow(sourceAt)
        overlay.getLocationInWindow(overlayAt)
        val bitmap = try {
            Bitmap.createBitmap(overlay.width, overlay.height, Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) {
            return
        }
        try {
            Canvas(bitmap).apply {
                translate(
                    -(overlayAt[0] - sourceAt[0]).toFloat(),
                    -(overlayAt[1] - sourceAt[1]).toFloat()
                )
            }.also(source::draw)
            image.value = bitmap.asImageBitmap()
        } catch (_: RuntimeException) {
            bitmap.recycle()
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        feed.removeOnScrollListener(scrollListener)
        source.removeOnLayoutChangeListener(layoutListener)
        overlay.removeOnLayoutChangeListener(layoutListener)
        nav.removeOnLayoutChangeListener(layoutListener)
        nativeGlass.removeOnLayoutChangeListener(layoutListener)
    }
}
