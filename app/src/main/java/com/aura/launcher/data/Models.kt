package com.aura.launcher.data

import android.os.UserHandle

data class AppEntry(
    val key: String,
    val pkg: String,
    val cls: String,
    val label: String,
    val user: UserHandle,
)

sealed interface HomeItem
data class AppItem(val key: String) : HomeItem
data class WidgetItem(val widgetId: Int, val heightDp: Int) : HomeItem
data class FolderItem(val id: String, val name: String, val keys: List<String>) : HomeItem

enum class IconShape(val label: String) {
    PUFFY("Puffy"),
    CIRCLE("Circle"),
    SQUIRCLE("Squircle"),
    ROUNDED("Rounded"),
    SQUARE("Square"),
    ORIGINAL("Original"),
}
