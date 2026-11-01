package com.newtube.liquidglass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/** Animated refractive lens. NewTube's Material view remains the actual navigation control. */
@Composable
fun LiquidGlassBottomBar(
    selectedIndex: Int,
    tabCount: Int,
    backdrop: Backdrop,
    bottomInset: Dp
) {
    if (tabCount <= 0) return
    val index by animateFloatAsState(
        selectedIndex.coerceIn(0, tabCount - 1).toFloat(),
        spring(dampingRatio = 0.78f, stiffness = 520f),
        label = "newtubeGlassSelectedTab"
    )
    val tint = Color(0xFF8ABEFF).copy(alpha = 0.15f)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tabWidth = maxWidth / tabCount
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = tabWidth * index, y = -(bottomInset / 2f))
                .width(tabWidth)
                .height(50.dp)
                .padding(horizontal = 4.dp, vertical = 3.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(9.dp.toPx(), 15.dp.toPx(), chromaticAberration = false)
                    },
                    highlight = { Highlight.Default },
                    shadow = { Shadow(alpha = 0.15f, radius = 10.dp) },
                    innerShadow = { InnerShadow(radius = 7.dp, alpha = 0.10f) },
                    onDrawSurface = { drawRect(tint) }
                )
        )
    }
}
