package com.aura.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class AppRepository(private val ctx: Context) {
    private val la = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val um = ctx.getSystemService(Context.USER_SERVICE) as UserManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val exec = Executors.newSingleThreadExecutor()
    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    var apps by mutableStateOf<List<AppEntry>>(emptyList()); private set
    var byKey by mutableStateOf<Map<String, AppEntry>>(emptyMap()); private set
    var iconVersion by mutableIntStateOf(0); private set
    var onLoaded: (() -> Unit)? = null

    @Volatile var packId: String = ""; private set
    @Volatile private var pack: IconPack? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
    }

    fun start() {
        la.registerCallback(callback)
        reload()
    }

    fun stop() = la.unregisterCallback(callback)

    fun reload() {
        exec.execute {
            val list = ArrayList<AppEntry>()
            try {
                for (u in um.userProfiles) {
                    val serial = um.getSerialNumberForUser(u)
                    for (i in la.getActivityList(null, u)) {
                        val cn = i.componentName
                        if (cn.packageName == ctx.packageName) continue
                        val key = cn.flattenToShortString() + "@" + serial
                        infos[key] = i
                        list.add(AppEntry(key, cn.packageName, cn.className, i.label.toString(), u))
                    }
                }
            } catch (_: Exception) {
            }
            list.sortBy { it.label.lowercase() }
            mainHandler.post {
                apps = list
                byKey = list.associateBy { it.key }
                cache.clear()
                iconVersion++
                onLoaded?.invoke()
            }
        }
    }

    fun launch(app: AppEntry) {
        try {
            la.startMainActivity(ComponentName(app.pkg, app.cls), app.user, null, null)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shortcuts(app: AppEntry): List<ShortcutInfo> = try {
        val q = LauncherApps.ShortcutQuery()
            .setPackage(app.pkg)
            .setActivity(ComponentName(app.pkg, app.cls))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        la.getShortcuts(q, app.user) ?: emptyList()
    } catch (_: Exception) {
        emptyList() // only works while Aura is the default launcher
    }

    fun startShortcut(s: ShortcutInfo) {
        try { la.startShortcut(s, null, null) } catch (_: Exception) {}
    }

    // ---- icons ----

    private fun cacheKey(app: AppEntry, shape: IconShape, px: Int) = "${app.key}|$shape|$px|$packId"

    fun cached(app: AppEntry, shape: IconShape, px: Int): ImageBitmap? = cache[cacheKey(app, shape, px)]

    fun iconBitmap(app: AppEntry, shape: IconShape, px: Int): ImageBitmap {
        val k = cacheKey(app, shape, px)
        cache[k]?.let { return it }
        val fromPack: Drawable? = pack?.drawableFor(app.pkg, app.cls)
        val d: Drawable = fromPack
            ?: infos[app.key]?.getBadgedIcon(0)
            ?: ctx.packageManager.defaultActivityIcon
        val bmp = IconRenderer.render(d, px, shape, masked = fromPack == null, seed = app.pkg).asImageBitmap()
        cache[k] = bmp
        return bmp
    }

    fun setIconPack(pkg: String) {
        packId = pkg
        pack = if (pkg.isBlank()) null else runCatching { IconPack(ctx, pkg) }.getOrNull()
        cache.clear()
        iconVersion++
    }

    fun iconPacks(): List<Pair<String, String>> {
        val pm = ctx.packageManager
        val found = LinkedHashMap<String, String>()
        val actions = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
        )
        for (a in actions) {
            try {
                for (ri in pm.queryIntentActivities(Intent(a), 0)) {
                    found[ri.activityInfo.packageName] = ri.loadLabel(pm).toString()
                }
            } catch (_: Exception) {
            }
        }
        return found.map { it.key to it.value }.sortedBy { it.second.lowercase() }
    }
}
