package com.aura.launcher.data

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateMapOf

/** Live notification counts per package, shown as dots on icons. */
object BadgeStore {
    val counts = mutableStateMapOf<String, Int>()

    fun set(m: Map<String, Int>) {
        counts.clear()
        counts.putAll(m)
    }
}

class BadgeService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        try {
            val m = HashMap<String, Int>()
            activeNotifications?.forEach { n ->
                val summary = (n.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                if (!n.isOngoing && !summary) m[n.packageName] = (m[n.packageName] ?: 0) + 1
            }
            Handler(Looper.getMainLooper()).post { BadgeStore.set(m) }
        } catch (_: Exception) {
        }
    }
}
