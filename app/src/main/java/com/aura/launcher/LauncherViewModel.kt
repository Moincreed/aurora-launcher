package com.aura.launcher

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.aura.launcher.data.AppEntry
import com.aura.launcher.data.AppItem
import com.aura.launcher.data.AppRepository
import com.aura.launcher.data.HomeStore
import com.aura.launcher.data.LauncherSettings
import com.aura.launcher.data.WidgetItem

sealed interface Source {
    data object Drawer : Source
    data class Page(val index: Int) : Source
    data object Dock : Source
    data class Folder(val id: String) : Source
}

data class AppMenuState(val app: AppEntry, val source: Source)
data class WidgetMenuState(val page: Int, val item: WidgetItem)
data class PickerRequest(
    val title: String,
    val initial: Set<String>,
    val includeHidden: Boolean = false,
    val onDone: (Set<String>) -> Unit,
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val settings = LauncherSettings(app)
    val repo = AppRepository(app)
    val home = HomeStore(app)
    val host = AppWidgetHost(app, 1024)
    val awm: AppWidgetManager = AppWidgetManager.getInstance(app)

    var drawerOpen by mutableStateOf(false)
    var settingsOpen by mutableStateOf(false)
    var widgetPickerOpen by mutableStateOf(false)
    var homeMenuOpen by mutableStateOf(false)
    var appMenu by mutableStateOf<AppMenuState?>(null)
    var widgetMenu by mutableStateOf<WidgetMenuState?>(null)
    var openFolderId by mutableStateOf<String?>(null)
    var picker by mutableStateOf<PickerRequest?>(null)
    var requestPage by mutableIntStateOf(-1)
    var currentPage by mutableIntStateOf(0)

    init {
        repo.setIconPack(settings.iconPack)
        repo.onLoaded = { onAppsLoaded() }
        repo.start()
    }

    private fun onAppsLoaded() {
        if (repo.apps.isEmpty()) return
        home.prune(repo.byKey.keys)
        if (!home.seeded) seed()
    }

    /** First run: fill the dock with the default phone/messages/browser/camera apps and a few favourites. */
    private fun seed() {
        val pm = getApplication<Application>().packageManager
        fun pkgFor(i: Intent): String? =
            pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName

        val dockPkgs = listOfNotNull(
            pkgFor(Intent(Intent.ACTION_DIAL)),
            pkgFor(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))),
            pkgFor(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))),
            pkgFor(Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
        ).distinct()
        for (p in dockPkgs) repo.apps.firstOrNull { it.pkg == p }?.let { home.addDock(it.key) }

        val favourites = listOf(
            "com.android.vending", "com.google.android.apps.photos", "com.google.android.youtube",
            "com.instagram.android", "com.whatsapp", "com.google.android.apps.maps",
            "com.google.android.gm", "com.android.settings",
        )
        for (p in favourites) {
            val a = repo.apps.firstOrNull { it.pkg == p && it.key !in home.dock } ?: continue
            home.add(AppItem(a.key))
        }
        home.markSeeded()
    }

    fun hide(key: String) {
        settings.hidden = settings.hidden + key
        settings.save()
    }

    fun removeWidget(page: Int, item: WidgetItem) {
        try { host.deleteAppWidgetId(item.widgetId) } catch (_: Exception) {}
        home.remove(page, item)
    }

    fun onHomePressed() {
        drawerOpen = false
        settingsOpen = false
        widgetPickerOpen = false
        homeMenuOpen = false
        appMenu = null
        widgetMenu = null
        openFolderId = null
        picker = null
        requestPage = 0
    }

    override fun onCleared() {
        repo.stop()
    }
}
