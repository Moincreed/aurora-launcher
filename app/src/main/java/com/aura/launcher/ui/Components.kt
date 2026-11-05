@file:OptIn(ExperimentalFoundationApi::class)

package com.aura.launcher.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aura.launcher.LauncherViewModel
import com.aura.launcher.data.AppEntry
import com.aura.launcher.data.BadgeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Frosted-glass surface. */
@Composable
fun Modifier.glass(shape: Shape): Modifier {
    val c = LocalAura.current
    return this.clip(shape).background(c.glass).border(1.dp, c.glassBorder, shape)
}

@Composable
fun rememberIcon(vm: LauncherViewModel, app: AppEntry, size: Dp): State<ImageBitmap?> {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val shape = vm.settings.shape
    return produceState<ImageBitmap?>(vm.repo.cached(app, shape, px), app.key, shape, px, vm.repo.iconVersion) {
        value = vm.repo.cached(app, shape, px)
            ?: withContext(Dispatchers.Default) { vm.repo.iconBitmap(app, shape, px) }
    }
}

@Composable
fun AppIcon(
    vm: LauncherViewModel,
    app: AppEntry,
    onClick: () -> Unit,
    onLong: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: Dp = vm.settings.iconSize.dp,
    showLabel: Boolean = vm.settings.showLabels,
) {
    val c = LocalAura.current
    val s = vm.settings
    val bmp by rememberIcon(vm, app, size)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.90f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val count = BadgeStore.counts[app.pkg] ?: 0
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(interactionSource = src, indication = null, onLongClick = onLong, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size)) {
            val b = bmp
            if (b != null) Image(b, null, Modifier.size(size))
            if (s.badges && count > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(c.accent)
                        .border(1.5.dp, c.base, CircleShape)
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(5.dp))
            Text(
                app.label,
                color = c.text,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MenuRow(glyph: String, title: String, onClick: () -> Unit) {
    val c = LocalAura.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(c.accent),
            contentAlignment = Alignment.Center,
        ) { Text(glyph, color = Color.White, fontSize = 14.sp) }
        Spacer(Modifier.width(14.dp))
        Text(title, color = c.text, fontSize = 16.sp)
    }
}

@Composable
fun PillButton(text: String, primary: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalAura.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(if (primary) Modifier.background(c.accent) else Modifier.background(c.chip))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (primary) Color.White else c.text, fontSize = 14.sp)
    }
}

@Composable
fun AuraDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val c = LocalAura.current
        val shape = RoundedCornerShape(30.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.surface)
                .border(1.dp, c.glassBorder, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

// ---- small intent helpers ----

fun expandShade(ctx: Context) {
    try {
        val sb = ctx.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
    } catch (_: Throwable) {
    }
}

fun openAppInfo(ctx: Context, pkg: String) {
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

fun uninstallApp(ctx: Context, pkg: String) {
    try {
        ctx.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

fun webSearch(ctx: Context, q: String) {
    try {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

fun openSystem(ctx: Context, action: String) {
    try {
        ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}
