package com.aura.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Slowly drifting colour washes over a calm base: the launcher's default wallpaper. */
@Composable
fun AuroraBackground() {
    val c = LocalAura.current
    val p = c.palette
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(31000, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    val d by t.animateFloat(0f, 1f, infiniteRepeatable(tween(37000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(c.base)
        val w = size.width
        val h = size.height
        val strength = if (c.dark) 0.50f else 0.60f
        fun blob(col: Color, cx: Float, cy: Float, r: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(col.copy(alpha = strength), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }
        blob(p.c1, w * (0.10f + 0.60f * a), h * (0.10f + 0.14f * b), w * 1.05f)
        blob(p.c2, w * (0.95f - 0.60f * b), h * (0.52f + 0.12f * d), w * 1.00f)
        blob(p.c3, w * (0.20f + 0.50f * d), h * (0.95f - 0.16f * a), w * 1.10f)
    }
}
