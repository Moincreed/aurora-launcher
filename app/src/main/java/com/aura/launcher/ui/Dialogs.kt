package com.aura.launcher.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.AppMenuState
import com.aura.launcher.LauncherViewModel
import com.aura.launcher.PickerRequest
import com.aura.launcher.Source
import com.aura.launcher.WidgetMenuState
import com.aura.launcher.data.AppItem
import com.aura.launcher.data.FolderItem
import java.util.UUID

/** All floating menus, stacked so later ones sit on top. */
@Composable
fun DialogHost(vm: LauncherViewModel) {
    if (vm.homeMenuOpen) HomeMenuDialog(vm)
    vm.widgetMenu?.let { WidgetMenuDialog(vm, it) }
    vm.openFolderId?.let { FolderDialog(vm, it) }
    vm.picker?.let { AppPickerDialog(vm, it) }
    vm.appMenu?.let { AppMenuDialog(vm, it) }
}

// ------------------------------------------------------------ app menu

@Composable
private fun AppMenuDialog(vm: LauncherViewModel, st: AppMenuState) {
    val ctx = LocalContext.current
    val c = LocalAura.current
    val app = st.app
    val home = vm.home
    val dismiss = { vm.appMenu = null }
    val shortcuts = remember(app.key) { vm.repo.shortcuts(app) }
    val icon by rememberIcon(vm, app, 44.dp)

    AuraDialog(dismiss) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Image(it, null, Modifier.size(44.dp)) }
            Spacer(Modifier.width(14.dp))
            Text(app.label, color = c.text, fontSize = 20.sp, fontFamily = FontFamily.Serif)
        }
        shortcuts.take(4).forEach { sc ->
            MenuRow("↗", sc.shortLabel?.toString() ?: sc.id) {
                vm.repo.startShortcut(sc); dismiss(); vm.drawerOpen = false
            }
        }
        when (val src = st.source) {
            is Source.Page -> {
                MenuRow("↑", "Move earlier") { home.move(src.index, AppItem(app.key), -1); dismiss() }
                MenuRow("↓", "Move later") { home.move(src.index, AppItem(app.key), 1); dismiss() }
                MenuRow("–", "Remove from Home") { home.remove(src.index, AppItem(app.key)); dismiss() }
            }
            is Source.Dock -> MenuRow("–", "Remove from Dock") { home.removeDock(app.key); dismiss() }
            is Source.Folder -> MenuRow("–", "Remove from folder") { home.removeFromFolder(src.id, app.key); dismiss() }
            else -> {}
        }
        if (st.source !is Source.Page) {
            MenuRow("+", "Add to Home") {
                val p = home.add(AppItem(app.key))
                if (p < 0) Toast.makeText(ctx, "Already on Home", Toast.LENGTH_SHORT).show()
                else { vm.requestPage = p; vm.drawerOpen = false; vm.openFolderId = null }
                dismiss()
            }
        }
        if (app.key !in home.dock && home.dock.size < 4) {
            MenuRow("▾", "Add to Dock") { home.addDock(app.key); dismiss(); vm.drawerOpen = false }
        }
        MenuRow("◌", "Hide app") { vm.hide(app.key); dismiss() }
        MenuRow("i", "App info") { openAppInfo(ctx, app.pkg); dismiss() }
        MenuRow("✕", "Uninstall") { uninstallApp(ctx, app.pkg); dismiss() }
    }
}

// ------------------------------------------------------------ home menu

@Composable
private fun HomeMenuDialog(vm: LauncherViewModel) {
    val ctx = LocalContext.current
    val home = vm.home
    val dismiss = { vm.homeMenuOpen = false }
    AuraDialog(dismiss) {
        MenuRow("▦", "Widgets") { dismiss(); vm.widgetPickerOpen = true }
        MenuRow("❏", "New folder") {
            dismiss()
            vm.picker = PickerRequest("Choose apps for the folder", emptySet()) { keys ->
                if (keys.isNotEmpty()) {
                    val p = home.add(FolderItem(UUID.randomUUID().toString(), "Folder", keys.toList()))
                    vm.requestPage = p
                }
            }
        }
        MenuRow("+", "Add page") { vm.requestPage = home.addPage(); dismiss() }
        if (home.pages.size > 1 && home.pages.getOrNull(vm.currentPage)?.items?.isEmpty() == true) {
            MenuRow("–", "Remove this empty page") { home.removePageIfEmpty(vm.currentPage); dismiss() }
        }
        MenuRow("✦", "Wallpaper") {
            dismiss()
            try {
                ctx.startActivity(
                    Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
        }
        MenuRow("⚙", "Aura settings") { dismiss(); vm.settingsOpen = true }
    }
}

// ------------------------------------------------------------ widget menu

@Composable
private fun WidgetMenuDialog(vm: LauncherViewModel, st: WidgetMenuState) {
    val c = LocalAura.current
    val dismiss = { vm.widgetMenu = null }
    AuraDialog(dismiss) {
        Text("Widget size", color = c.textDim, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("S" to 120, "M" to 180, "L" to 260, "XL" to 380).forEach { (label, h) ->
                PillButton(label, primary = st.item.heightDp == h) {
                    vm.home.replace(st.page, st.item, st.item.copy(heightDp = h)); dismiss()
                }
            }
        }
        Spacer(Modifier.size(6.dp))
        MenuRow("↑", "Move up") { vm.home.move(st.page, st.item, -1); dismiss() }
        MenuRow("↓", "Move down") { vm.home.move(st.page, st.item, 1); dismiss() }
        MenuRow("✕", "Remove widget") { vm.removeWidget(st.page, st.item); dismiss() }
    }
}

// ------------------------------------------------------------ folder

@Composable
private fun FolderDialog(vm: LauncherViewModel, id: String) {
    val c = LocalAura.current
    val found = vm.home.findFolder(id)
    if (found == null) {
        SideEffect { vm.openFolderId = null }
        return
    }
    val folder = found.second
    val apps = folder.keys.mapNotNull { vm.repo.byKey[it] }
    AuraDialog({ vm.openFolderId = null }) {
        BasicTextField(
            value = folder.name,
            onValueChange = { v -> vm.home.updateFolder(id) { it.copy(name = v) } },
            singleLine = true,
            textStyle = TextStyle(color = c.text, fontSize = 24.sp, fontFamily = FontFamily.Serif),
            cursorBrush = SolidColor(c.palette.c1),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gridItems(apps, key = { it.key }) { app ->
                AppIcon(
                    vm, app,
                    onClick = { vm.repo.launch(app); vm.openFolderId = null },
                    onLong = { vm.appMenu = AppMenuState(app, Source.Folder(id)) },
                    size = 46.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Add apps", primary = true) {
                vm.picker = PickerRequest("Apps in “${folder.name}”", folder.keys.toSet()) { keys ->
                    vm.home.updateFolder(id) { it.copy(keys = keys.toList()) }
                }
            }
            PillButton("Delete") { vm.home.removeFolder(id); vm.openFolderId = null }
        }
    }
}

// ------------------------------------------------------------ app picker

@Composable
private fun AppPickerDialog(vm: LauncherViewModel, req: PickerRequest) {
    val c = LocalAura.current
    var sel by remember(req) { mutableStateOf(req.initial) }
    val list = remember(vm.repo.apps, vm.settings.hidden) {
        vm.repo.apps.filter { req.includeHidden || it.key !in vm.settings.hidden || it.key in req.initial }
    }
    AuraDialog({ vm.picker = null }) {
        Text(req.title, color = c.text, fontSize = 18.sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(8.dp))
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(list, key = { it.key }) { app ->
                val on = app.key in sel
                val icon by rememberIcon(vm, app, 36.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { sel = if (on) sel - app.key else sel + app.key }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon?.let { Image(it, null, Modifier.size(36.dp)) } ?: Spacer(Modifier.size(36.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(app.label, color = c.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .then(if (on) Modifier.background(c.accent) else Modifier.border(1.5.dp, c.textDim, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Text("✓", color = Color.White, fontSize = 12.sp) }
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Done", primary = true) { req.onDone(sel); vm.picker = null }
            PillButton("Cancel") { vm.picker = null }
        }
    }
}
