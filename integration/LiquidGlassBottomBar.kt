package com.newtube.liquidglass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

@Composable
fun LiquidGlassBottomBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    backdrop: Backdrop,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    darkAppearance: Boolean = true
) {
    if (items.isEmpty()) return
    val surface = if (darkAppearance) Color(0xC917191E) else Color(0xC9F8F9FC)
    val foreground = if (darkAppearance) Color(0xFFF6F7FB) else Color(0xFF17191E)
    val inactive = if (darkAppearance) Color(0xFFB5BAC5) else Color(0xFF626875)
    val accent = if (darkAppearance) Color(0xFF9CC7FF) else Color(0xFF174EA6)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .navigationBarsPadding()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(18.dp.toPx())
                    lens(22.dp.toPx(), 18.dp.toPx(), chromaticAberration = false)
                },
                highlight = { Highlight.Default },
                shadow = { Shadow(alpha = 0.20f, radius = 18.dp) },
                innerShadow = { InnerShadow(radius = 8.dp, alpha = 0.12f) },
                onDrawSurface = { drawRect(surface) }
            )
            .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            LiquidNavigationItem(
                item = item,
                selected = index == selectedIndex,
                foreground = foreground,
                inactive = inactive,
                accent = accent,
                backdrop = backdrop,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

@Composable
private fun RowScope.LiquidNavigationItem(
    item: NavigationItem,
    selected: Boolean,
    foreground: Color,
    inactive: Color,
    accent: Color,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.03f else 1f, label = "navScale")
    val labelColor by animateColorAsState(if (selected) foreground else inactive, label = "navLabelColor")
    Box(
        modifier = Modifier
            .weight(1f)
            .height(60.dp)
            .scale(scale)
            .semantics { contentDescription = item.label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 1.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(7.dp.toPx())
                            lens(12.dp.toPx(), 10.dp.toPx())
                        },
                        highlight = { Highlight.Default },
                        shadow = { Shadow(alpha = 0.18f, radius = 8.dp) },
                        onDrawSurface = { drawRect(accent.copy(alpha = 0.24f)) }
                    )
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (selected) 1f else 0.92f }
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            BasicText(
                text = item.glyph,
                style = androidx.compose.ui.text.TextStyle(
                    color = if (selected) accent else labelColor,
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            )
            BasicText(
                text = item.label,
                style = androidx.compose.ui.text.TextStyle(
                    color = labelColor,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

data class NavigationItem(val label: String, val glyph: String, val id: Int)
