package com.aura.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.AppMenuState
import com.aura.launcher.LauncherViewModel
import com.aura.launcher.Source
import com.aura.launcher.data.AppEntry
import kotlinx.coroutines.launch
import java.text.Normalizer

private fun letterOf(label: String): String {
    val base = Normalizer.normalize(label, Normalizer.Form.NFD)
    val ch = base.firstOrNull { it.isLetterOrDigit() } ?: return "#"
    val u = ch.uppercaseChar()
    return if (u in 'A'..'Z') u.toString() else "#"
}

@Composable
fun AppDrawer(vm: LauncherViewModel) {
    val c = LocalAura.current
    val s = vm.settings
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val q = query.trim()

    val visible = remember(vm.repo.apps, s.hidden) { vm.repo.apps.filter { it.key !in s.hidden } }
    val filtered = remember(visible, q) {
        if (q.isEmpty()) {
            visible.sortedWith(
                compareBy<AppEntry>({ letterOf(it.label).let { l -> if (l == "#") "\u0000" else l } }, { it.label.lowercase() })
            )
        } else {
            visible.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { !it.label.startsWith(q, ignoreCase = true) }
        }
    }
    val rows: List<Any> = remember(filtered, q) {
        if (q.isNotEmpty()) filtered
        else buildList {
            var last = ""
            for (a in filtered) {
                val l = letterOf(a.label)
                if (l != last) { add(l); last = l }
                add(a)
            }
        }
    }
    val gridState = rememberLazyGridState()

    fun close() {
        focus.clearFocus()
        query = ""
        vm.drawerOpen = false
    }
    fun launchApp(a: AppEntry) {
        vm.repo.launch(a)
        close()
    }
    BackHandler { if (query.isNotEmpty()) query = "" else close() }

    Box(Modifier.fillMaxSize().background(c.base.copy(alpha = if (s.systemWallpaper) 0.80f else 0.93f))
        .pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            // grab handle: drag down to dismiss
            Box(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var t = 0f
                        detectVerticalDragGestures(onDragStart = { t = 0f }, onDragEnd = { if (t > 80f) close() }) { _, dy -> t += dy }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(width = 42.dp, height = 4.dp).clip(CircleShape).background(c.textDim.copy(alpha = 0.5f)))
            }

            // search
            Row(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val dim = c.textDim
                Canvas(Modifier.size(18.dp)) {
                    val w = this.size.width
                    val sw = 2.dp.toPx()
                    drawCircle(dim, radius = w * 0.34f, center = Offset(w * 0.42f, w * 0.42f), style = Stroke(sw))
                    drawLine(dim, Offset(w * 0.66f, w * 0.66f), Offset(w * 0.92f, w * 0.92f), strokeWidth = sw, cap = StrokeCap.Round)
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search apps or the web", color = c.textDim, fontSize = 16.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = c.text, fontSize = 16.sp),
                        cursorBrush = SolidColor(c.palette.c1),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            val first = filtered.firstOrNull()
                            if (first != null) launchApp(first) else if (q.isNotEmpty()) webSearch(ctx, q)
                        }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Text("✕", color = c.textDim, fontSize = 16.sp, modifier = Modifier.clickable { query = "" }.padding(start = 8.dp))
                }
            }

            Row(Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(s.drawerColumns),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = if (q.isEmpty()) 0.dp else 12.dp, top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        count = rows.size,
                        span = { i -> if (rows[i] is String) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                    ) { i ->
                        when (val r = rows[i]) {
                            is String -> Text(
                                r,
                                color = c.palette.c1,
                                fontFamily = FontFamily.Serif,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(start = 8.dp, top = 10.dp),
                            )
                            is AppEntry -> AppIcon(
                                vm, r,
                                onClick = { launchApp(r) },
                                onLong = { vm.appMenu = AppMenuState(r, Source.Drawer) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (q.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Search the web for “$q”",
                                color = c.palette.c1,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { webSearch(ctx, q); close() }
                                    .padding(16.dp),
                            )
                        }
                    }
                }
                if (q.isEmpty()) {
                    val letters = rows.filterIsInstance<String>()
                    IndexRail(letters) { letter ->
                        val idx = rows.indexOf(letter)
                        if (idx >= 0) scope.launch { gridState.scrollToItem(idx) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexRail(letters: List<String>, onSelect: (String) -> Unit) {
    val c = LocalAura.current
    var h by remember { mutableIntStateOf(0) }
    fun pick(y: Float) {
        if (letters.isEmpty() || h == 0) return
        val i = (y / h * letters.size).toInt().coerceIn(0, letters.size - 1)
        onSelect(letters[i])
    }
    Column(
        Modifier
            .fillMaxHeight()
            .width(30.dp)
            .onSizeChanged { h = it.height }
            .pointerInput(letters) { detectTapGestures { pick(it.y) } }
            .pointerInput(letters) {
                detectDragGestures(onDragStart = { pick(it.y) }) { change, _ ->
                    change.consume()
                    pick(change.position.y)
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { Text(it, fontSize = 10.sp, color = c.textDim) }
    }
}
