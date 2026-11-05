package com.aura.launcher.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** All user-tweakable options. Fields are Compose state; call [save] after changing them. */
class LauncherSettings(ctx: Context) {
    private val sp = ctx.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)

    var themeMode by mutableIntStateOf(sp.getInt("themeMode", 0)) // 0 system, 1 dark, 2 light
    var palette by mutableIntStateOf(sp.getInt("palette", 0))
    var systemWallpaper by mutableStateOf(sp.getBoolean("systemWallpaper", false))

    var shape by mutableStateOf(
        runCatching { IconShape.valueOf(sp.getString("shape", "PUFFY") ?: "PUFFY") }
            .getOrDefault(IconShape.PUFFY)
    )
    var iconSize by mutableIntStateOf(sp.getInt("iconSize", 54))
    var showLabels by mutableStateOf(sp.getBoolean("showLabels", true))
    var iconPack by mutableStateOf(sp.getString("iconPack", "") ?: "")

    var homeColumns by mutableIntStateOf(sp.getInt("homeColumns", 4))
    var drawerColumns by mutableIntStateOf(sp.getInt("drawerColumns", 4))
    var showClock by mutableStateOf(sp.getBoolean("showClock", true))
    var clockStyle by mutableIntStateOf(sp.getInt("clockStyle", 0)) // 0 serif, 1 sans, 2 stacked
    var swipeDownShade by mutableStateOf(sp.getBoolean("swipeDownShade", true))
    var badges by mutableStateOf(sp.getBoolean("badges", true))

    var hidden by mutableStateOf<Set<String>>(sp.getStringSet("hidden", emptySet())?.toSet() ?: emptySet())

    fun save() {
        sp.edit()
            .putInt("themeMode", themeMode)
            .putInt("palette", palette)
            .putBoolean("systemWallpaper", systemWallpaper)
            .putString("shape", shape.name)
            .putInt("iconSize", iconSize)
            .putBoolean("showLabels", showLabels)
            .putString("iconPack", iconPack)
            .putInt("homeColumns", homeColumns)
            .putInt("drawerColumns", drawerColumns)
            .putBoolean("showClock", showClock)
            .putInt("clockStyle", clockStyle)
            .putBoolean("swipeDownShade", swipeDownShade)
            .putBoolean("badges", badges)
            .putStringSet("hidden", hidden.toSet())
            .apply()
    }
}
