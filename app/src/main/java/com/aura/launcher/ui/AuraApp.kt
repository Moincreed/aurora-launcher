package com.aura.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.aura.launcher.LauncherViewModel

@Composable
fun AuraApp(vm: LauncherViewModel, onPickWidget: (AppWidgetProviderInfo) -> Unit) {
    AuraTheme(vm.settings) {
        val c = LocalAura.current
        val activity = LocalContext.current as ComponentActivity

        // Keep status/navigation bar icon colours in step with the chosen theme.
        LaunchedEffect(c.dark) {
            val style = if (c.dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }

        Box(Modifier.fillMaxSize()) {
            if (vm.settings.systemWallpaper) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
            } else {
                AuroraBackground()
            }

            HomeScreen(vm)

            AnimatedVisibility(
                visible = vm.drawerOpen,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut(),
            ) { AppDrawer(vm) }

            AnimatedVisibility(
                visible = vm.widgetPickerOpen,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut(),
            ) { WidgetPicker(vm, onPickWidget) }

            AnimatedVisibility(
                visible = vm.settingsOpen,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
            ) { SettingsScreen(vm) }

            DialogHost(vm)
        }
    }
}
