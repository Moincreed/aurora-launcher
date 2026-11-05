package com.aura.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private class WidgetGroup(val pkg: String, val name: String, val list: List<AppWidgetProviderInfo>)

@Composable
fun WidgetPicker(vm: com.aura.launcher.LauncherViewModel, onPick: (AppWidgetProviderInfo) -> Unit) {
    val c = LocalAura.current
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val pm = ctx.packageManager
    BackHandler { vm.widgetPickerOpen = false }

    val groups = remember {
        vm.awm.installedProviders
            .groupBy { it.provider.packageName }
            .map { (pkg, list) ->
                val name = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                    .getOrElse { runCatching { list.first().loadLabel(pm).toString() }.getOrDefault(pkg) }
                WidgetGroup(pkg, name, list)
            }
            .sortedBy { it.name.lowercase() }
    }
    var expanded by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(c.base.copy(alpha = 0.97f))
        .pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Widgets", color = c.text, fontSize = 30.sp, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
                PillButton("Close") { vm.widgetPickerOpen = false }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { g ->
                    item(key = g.pkg) {
                        val open = expanded == g.pkg
                        Row(
                            Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .glass(RoundedCornerShape(20.dp))
                                .clickable { expanded = if (open) null else g.pkg }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(g.name, color = c.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text("${g.list.size} ${if (open) "▴" else "▾"}", color = c.textDim, fontSize = 13.sp)
                        }
                    }
                    if (expanded == g.pkg) {
                        g.list.forEachIndexed { n, info ->
                            item(key = "${g.pkg}#$n") {
                                WidgetCard(info, density) { onPick(info); vm.widgetPickerOpen = false }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.size(24.dp)) }
            }
        }
    }
}

@Composable
private fun WidgetCard(info: AppWidgetProviderInfo, density: Float, onClick: () -> Unit) {
    val c = LocalAura.current
    val ctx = LocalContext.current
    val preview = remember(info) {
        runCatching {
            val d = info.loadPreviewImage(ctx, 0) ?: info.loadIcon(ctx, 0)
            val w = d.intrinsicWidth.coerceAtLeast(1)
            val h = d.intrinsicHeight.coerceAtLeast(1)
            val sc = min(1f, 520f / max(w, h))
            d.toBitmap(max(1, (w * sc).toInt()), max(1, (h * sc).toInt())).asImageBitmap()
        }.getOrNull()
    }
    val label = remember(info) { runCatching { info.loadLabel(ctx.packageManager).toString() }.getOrDefault("Widget") }
    val cols = ceil((info.minWidth / density + 30) / 70.0).toInt().coerceAtLeast(1)
    val rows = ceil((info.minHeight / density + 30) / 70.0).toInt().coerceAtLeast(1)
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.chip)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 190.dp), contentAlignment = Alignment.Center) {
            preview?.let { Image(it, null, Modifier.heightIn(max = 190.dp), contentScale = ContentScale.Fit) }
        }
        Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text("$cols × $rows", color = c.textDim, fontSize = 12.sp)
        }
    }
}
