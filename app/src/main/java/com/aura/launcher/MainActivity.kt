package com.aura.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.aura.launcher.data.WidgetItem
import com.aura.launcher.ui.AuraApp
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private var pendingWidgetId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AuraApp(vm, onPickWidget = ::beginAddWidget) }
    }

    override fun onStart() {
        super.onStart()
        try { vm.host.startListening() } catch (_: Exception) {}
    }

    override fun onStop() {
        try { vm.host.stopListening() } catch (_: Exception) {}
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.onHomePressed()
        }
    }

    // ---- widget binding flow ----

    private fun beginAddWidget(info: AppWidgetProviderInfo) {
        val id = vm.host.allocateAppWidgetId()
        pendingWidgetId = id
        if (vm.awm.bindAppWidgetIdIfAllowed(id, info.provider)) {
            configureOrFinish(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_BIND)
        }
    }

    private fun configureOrFinish(id: Int) {
        val info = vm.awm.getAppWidgetInfo(id)
        if (info?.configure != null) {
            try {
                vm.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIG, null)
                return
            } catch (_: Exception) {
            }
        }
        finishAdd(id)
    }

    private fun finishAdd(id: Int) {
        val info = vm.awm.getAppWidgetInfo(id)
        if (info == null) {
            vm.host.deleteAppWidgetId(id)
            return
        }
        val dp = (info.minHeight / resources.displayMetrics.density).toInt()
        val h = min(max(dp, 110), 420)
        val page = vm.home.add(WidgetItem(id, h))
        vm.requestPage = page
        pendingWidgetId = -1
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId) ?: pendingWidgetId
        if (id < 0) return
        when (requestCode) {
            REQ_BIND -> if (resultCode == RESULT_OK) configureOrFinish(id) else vm.host.deleteAppWidgetId(id)
            REQ_CONFIG -> if (resultCode == RESULT_OK) finishAdd(id) else vm.host.deleteAppWidgetId(id)
        }
    }

    companion object {
        private const val REQ_BIND = 7001
        private const val REQ_CONFIG = 7002
    }
}
