@file:OptIn(ExperimentalFoundationApi::class)

package com.aura.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aura.launcher.AppMenuState
import com.aura.launcher.LauncherViewModel
import com.aura.launcher.Source
import com.aura.launcher.WidgetMenuState
import com.aura.launcher.data.AppItem
import com.aura.launcher.data.FolderItem
import com.aura.launcher.data.WidgetItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val s = vm.settings
    val home = vm.home
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { home.pages.size })
    val dragThreshold = with(LocalDensity.current) { 90.dp.toPx() }

    home.columns = s.homeColumns
    home.clockReserve = if (s.showClock) 210 else 0

    LaunchedEffect(vm.requestPage) {
        if (vm.requestPage >= 0) {
            pagerState.animateScrollToPage(vm.requestPage.coerceIn(0, home.pages.size - 1))
            vm.requestPage = -1
        }
    }
    LaunchedEffect(pagerState.currentPage) { vm.currentPage = pagerState.currentPage }
    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            // swipe up = app drawer, swipe down = notification shade
            .pointerInput(s.swipeDownShade) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragCancel = { total = 0f },
                    onDragEnd = {
                        if (total < -dragThreshold) vm.drawerOpen = true
                        else if (total > dragThreshold * 1.6f && s.swipeDownShade) expandShade(ctx)
                    },
                ) { _, dy -> total += dy }
            }
    ) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onLongPress = { vm.homeMenuOpen = true }) }
        ) {
            SideEffect { home.capacityDp = maxHeight.value.toInt() }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { idx ->
                HomePageContent(vm, idx)
            }
        }
        PageDots(pagerState.currentPage, home.pages.size)
        Dock(vm)
    }
}

@Composable
private fun HomePageContent(vm: LauncherViewModel, idx: Int) {
    val s = vm.settings
    val page = vm.home.pages.getOrNull(idx) ?: return
    LazyVerticalGrid(
        columns = GridCells.Fixed(s.homeColumns),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (idx == 0 && s.showClock) {
            item(span = { GridItemSpan(maxLineSpan) }) { ClockHeader(vm) }
        }
        items(
            count = page.items.size,
            span = { i -> if (page.items.getOrNull(i) is WidgetItem) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
        ) { i ->
            when (val item = page.items.getOrNull(i)) {
                is AppItem -> {
                    val app = vm.repo.byKey[item.key]
                    if (app != null) {
                        AppIcon(
                            vm, app,
                            onClick = { vm.repo.launch(app) },
                            onLong = { vm.appMenu = AppMenuState(app, Source.Page(idx)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is FolderItem -> FolderTile(vm, item)
                is WidgetItem -> WidgetView(vm, idx, item)
                null -> {}
            }
        }
        if (page.items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Touch and hold to add widgets, folders and pages",
                    color = LocalAura.current.textDim,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- clock

@Composable
fun ClockHeader(vm: LauncherViewModel) {
    val c = LocalAura.current
    val ctx = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - value % 60_000L + 30L)
        }
    }
    val is24 = remember { DateFormat.is24HourFormat(ctx) }
    val time = remember(now) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(Date(now)) }
    val date = remember(now) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)).uppercase() }
    val greeting = remember(now) {
        when (Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }
    val tap = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 26.dp, bottom = 14.dp)
            .clickable(interactionSource = tap, indication = null) {
                try {
                    ctx.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                }
            }
    ) {
        Text(
            greeting,
            color = c.textDim,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 17.sp,
        )
        when (vm.settings.clockStyle) {
            1 -> Text(
                time,
                color = c.text,
                fontSize = 74.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
            )
            2 -> {
                val brush = Brush.linearGradient(listOf(c.palette.c1, c.palette.c2, c.palette.c3))
                Text(
                    time.substringBefore(':'),
                    style = TextStyle(brush = brush, fontSize = 84.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light),
                    lineHeight = 78.sp,
                )
                Text(
                    time.substringAfter(':'),
                    color = c.text,
                    fontSize = 84.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                    lineHeight = 78.sp,
                )
            }
            else -> Text(
                time,
                color = c.text,
                fontSize = 84.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                letterSpacing = (-3).sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            date,
            color = c.textDim,
            fontSize = 12.sp,
            letterSpacing = 3.sp,
            modifier = Modifier.clickable(interactionSource = tap, indication = null) {
                try {
                    ctx.startActivity(
                        Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_APP_CALENDAR)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
            },
        )
    }
}

// ---------------------------------------------------------------- folder

@Composable
private fun FolderTile(vm: LauncherViewModel, f: FolderItem) {
    val c = LocalAura.current
    val size = vm.settings.iconSize.dp
    val apps = f.keys.mapNotNull { vm.repo.byKey[it] }
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = { vm.openFolderId = f.id },
                onClick = { vm.openFolderId = f.id },
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(size).glass(RoundedCornerShape(size * 0.32f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (row in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (col in 0..1) {
                            val app = apps.getOrNull(row * 2 + col)
                            val mini = size * 0.34f
                            if (app != null) {
                                val bmp by rememberIcon(vm, app, mini)
                                val b = bmp
                                if (b != null) Image(b, null, Modifier.size(mini)) else Spacer(Modifier.size(mini))
                            } else {
                                Spacer(Modifier.size(mini))
                            }
                        }
                    }
                }
            }
        }
        if (vm.settings.showLabels) {
            Spacer(Modifier.height(5.dp))
            Text(
                f.name,
                color = c.text,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------- widget

@Composable
private fun WidgetView(vm: LauncherViewModel, pageIdx: Int, item: WidgetItem) {
    val c = LocalAura.current
    val info: AppWidgetProviderInfo? = remember(item.widgetId) { vm.awm.getAppWidgetInfo(item.widgetId) }
    val shape = RoundedCornerShape(26.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().height(item.heightDp.dp).clip(shape)) {
        if (info == null) {
            Box(Modifier.fillMaxSize().glass(shape), contentAlignment = Alignment.Center) {
                Text("Widget unavailable", color = c.textDim, fontSize = 13.sp)
            }
        } else {
            val w = maxWidth.value.toInt()
            val h = item.heightDp
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    vm.host.createView(ctx, item.widgetId, info).apply {
                        setPadding(0, 0, 0, 0)
                        updateAppWidgetSize(Bundle(), w, h, w, h)
                    }
                },
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { vm.widgetMenu = WidgetMenuState(pageIdx, item) },
            contentAlignment = Alignment.Center,
        ) { Text("•••", color = Color.White, fontSize = 9.sp) }
    }
}

// ---------------------------------------------------------------- dots + dock

@Composable
private fun PageDots(current: Int, count: Int) {
    if (count <= 1) return
    val c = LocalAura.current
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            val sel = i == current
            val w by animateDpAsState(if (sel) 22.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .padding(3.dp)
                    .height(6.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(if (sel) c.accent else SolidColor(c.textDim.copy(alpha = 0.45f)))
            )
        }
    }
}

@Composable
private fun Dock(vm: LauncherViewModel) {
    val s = vm.settings
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(36.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (key in vm.home.dock) {
            val app = vm.repo.byKey[key] ?: continue
            AppIcon(
                vm, app,
                onClick = { vm.repo.launch(app) },
                onLong = { vm.appMenu = AppMenuState(app, Source.Dock) },
                showLabel = false,
            )
        }
        DrawerButton(s.iconSize) { vm.drawerOpen = true }
    }
}

@Composable
private fun DrawerButton(sizeDp: Int, onClick: () -> Unit) {
    val c = LocalAura.current
    val size = sizeDp.dp
    Box(
        Modifier
            .padding(4.dp)
            .size(size)
            .clip(RoundedCornerShape(size * 0.36f))
            .background(c.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.46f)) {
            val d = this.size.minDimension
            val r = d / 11f
            val step = (d - 2f * r) / 2f
            for (i in 0..2) for (j in 0..2) {
                drawCircle(Color.White.copy(alpha = 0.95f), r, Offset(r + i * step, r + j * step))
            }
        }
    }
}
