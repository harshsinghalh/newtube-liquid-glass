package com.newtube.liquidglass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

/**
 * Adapts the existing View-based NewTube navigation to Compose without moving navigation logic.
 *
 * The backdrop is a cropped bitmap rendered from the real Android View hierarchy beneath the bar.
 * It is refreshed on feed scroll/layout and tab-menu changes; this is intentionally a snapshot
 * bridge, because Backdrop cannot directly sample Android View siblings.
 */
class LiquidGlassNavBridge(
    private val root: View,
    private val composeView: ComposeView,
    private val legacyNav: BottomNavigationView,
    private val content: RecyclerView
) {
    private val destinations = mutableStateOf<List<NavigationItem>>(emptyList())
    private val selectedIndex = mutableStateOf(0)
    private val background = mutableStateOf<ImageBitmap?>(null)

    init {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            val image = background.value
            val backdrop = rememberCanvasBackdrop {
                if (image != null) drawImage(image)
            }
            val items = destinations.value
            LiquidGlassBottomBar(
                items = items,
                selectedIndex = selectedIndex.value.coerceIn(0, maxOf(0, items.lastIndex)),
                backdrop = backdrop,
                onItemSelected = { index ->
                    items.getOrNull(index)?.let { legacyNav.selectedItemId = it.id }
                }
            )
        }
        legacyNav.visibility = View.INVISIBLE
        content.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                captureBackground()
            }
        })
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> captureBackground() }
        composeView.post { refreshFromMenu() }
    }

    fun refreshFromMenu() {
        val menu = legacyNav.menu
        val items = (0 until menu.size()).map { index ->
            val item = menu.getItem(index)
            NavigationItem(item.title?.toString().orEmpty(), glyphFor(item.title?.toString().orEmpty()), item.itemId)
        }
        destinations.value = items
        val selectedId = legacyNav.selectedItemId
        selectedIndex.value = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        composeView.post { captureBackground() }
    }

    private fun glyphFor(label: String): String {
        val lower = label.lowercase()
        return when {
            "home" in lower -> "⌂"
            "subscription" in lower -> "▤"
            "history" in lower -> "◷"
            "download" in lower -> "↓"
            "you" in lower -> "●"
            else -> "•"
        }
    }

    private fun captureBackground() {
        if (!composeView.isAttachedToWindow || composeView.width <= 0 || composeView.height <= 0 ||
            root.width <= 0 || root.height <= 0) return
        val bitmap = try {
            Bitmap.createBitmap(composeView.width, composeView.height, Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) {
            return
        }
        val canvas = Canvas(bitmap)
        val previousAlpha = composeView.alpha
        composeView.alpha = 0f
        canvas.translate(-composeView.left.toFloat(), -composeView.top.toFloat())
        root.draw(canvas)
        composeView.alpha = previousAlpha
        background.value = bitmap.asImageBitmap()
    }
}
