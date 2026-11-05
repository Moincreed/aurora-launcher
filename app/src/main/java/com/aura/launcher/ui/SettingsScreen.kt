package com.aura.launcher.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.aura.launcher.LauncherViewModel
import com.aura.launcher.PickerRequest
import com.aura.launcher.data.IconShape

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val c = LocalAura.current
    val s = vm.settings
    val ctx = LocalContext.current
    BackHandler { vm.settingsOpen = false }

    Box(Modifier.fillMaxSize().background(c.base.copy(alpha = 0.97f))
        .pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Settings", color = c.text, fontSize = 30.sp, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
                PillButton("Done", primary = true) { vm.settingsOpen = false }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Section("Appearance") {
                    Label("Theme")
                    Choices(listOf("System", "Dark", "Light"), s.themeMode) { s.themeMode = it; s.save() }
                    Label("Colour palette")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Palettes.forEachIndexed { i, p ->
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(p.c1, p.c2, p.c3)))
                                    .then(if (s.palette == i) Modifier.border(3.dp, c.text, CircleShape) else Modifier)
                                    .clickable { s.palette = i; s.save() }
                            )
                        }
                    }
                    Label("Wallpaper")
                    Choices(listOf("Aurora", "My wallpaper"), if (s.systemWallpaper) 1 else 0) {
                        s.systemWallpaper = it == 1; s.save()
                    }
                    if (s.systemWallpaper) {
                        ActionRow("Change system wallpaper", "Opens the wallpaper picker") {
                            openSystem(ctx, android.content.Intent.ACTION_SET_WALLPAPER)
                        }
                    }
                }

                Section("Icons") {
                    Label("Shape")
                    Choices(IconShape.entries.map { it.label }, s.shape.ordinal) {
                        s.shape = IconShape.entries[it]; s.save()
                    }
                    Label("Size  ·  ${s.iconSize} dp")
                    Slider(
                        value = s.iconSize.toFloat(),
                        onValueChange = { s.iconSize = it.toInt() },
                        onValueChangeFinished = { s.save() },
                        valueRange = 44f..68f,
                        colors = SliderDefaults.colors(
                            thumbColor = c.palette.c1,
                            activeTrackColor = c.palette.c1,
                            inactiveTrackColor = c.chip,
                        ),
                    )
                    ToggleRow("Show labels", null, s.showLabels) { s.showLabels = it; s.save() }
                    Label("Icon pack")
                    val packs = remember { vm.repo.iconPacks() }
                    val names = listOf("Default") + packs.map { it.second }
                    val sel = if (s.iconPack.isBlank()) 0 else packs.indexOfFirst { it.first == s.iconPack } + 1
                    Choices(names, sel.coerceAtLeast(0)) { i ->
                        s.iconPack = if (i == 0) "" else packs[i - 1].first
                        vm.repo.setIconPack(s.iconPack)
                        s.save()
                    }
                    if (packs.isEmpty()) Text("No icon packs installed. Any ADW / Nova compatible pack will appear here.", color = c.textDim, fontSize = 12.sp)
                }

                Section("Home") {
                    Label("Columns")
                    Choices(listOf("4", "5"), s.homeColumns - 4) { s.homeColumns = it + 4; s.save() }
                    ToggleRow("Clock & greeting", null, s.showClock) { s.showClock = it; s.save() }
                    if (s.showClock) {
                        Label("Clock style")
                        Choices(listOf("Serif", "Light sans", "Stacked"), s.clockStyle) { s.clockStyle = it; s.save() }
                    }
                    ToggleRow("Swipe down for notifications", null, s.swipeDownShade) { s.swipeDownShade = it; s.save() }
                }

                Section("App drawer") {
                    Label("Columns")
                    Choices(listOf("3", "4", "5"), s.drawerColumns - 3) { s.drawerColumns = it + 3; s.save() }
                    ActionRow("Hidden apps", "${s.hidden.size} hidden") {
                        vm.picker = PickerRequest("Hidden apps", s.hidden, includeHidden = true) { set ->
                            s.hidden = set; s.save()
                        }
                    }
                }

                Section("Notifications") {
                    ToggleRow("Notification dots", "Shows a dot on apps with unread notifications", s.badges) { on ->
                        s.badges = on; s.save()
                        val enabled = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
                        if (on && !enabled) openSystem(ctx, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    }
                }

                Section("System") {
                    ActionRow("Set Aura as default launcher", "Opens Android's home app settings") {
                        openSystem(ctx, Settings.ACTION_HOME_SETTINGS)
                    }
                }

                Text(
                    "Aura Launcher 1.0",
                    color = c.textDim,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val c = LocalAura.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            color = c.palette.c1,
            fontSize = 12.sp,
            letterSpacing = 2.5.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Column(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = LocalAura.current.textDim, fontSize = 13.sp)
}

@Composable
private fun Choices(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = LocalAura.current
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, name ->
            val on = i == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .then(if (on) Modifier.background(c.accent) else Modifier.background(c.chip))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(name, color = if (on) Color.White else c.text, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalAura.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 16.sp)
            if (sub != null) Text(sub, color = c.textDim, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.palette.c1,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = c.chip,
                uncheckedThumbColor = c.textDim,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ActionRow(title: String, sub: String, onClick: () -> Unit) {
    val c = LocalAura.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        Text(title, color = c.text, fontSize = 16.sp)
        Text(sub, color = c.textDim, fontSize = 12.sp)
    }
}
