package com.aura.launcher.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil

class HomePage {
    val items = mutableStateListOf<HomeItem>()
}

/** Home pages + dock, persisted as JSON. Items auto-flow in a grid; widgets span the full width. */
class HomeStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("aura_home", Context.MODE_PRIVATE)

    val pages = mutableStateListOf<HomePage>()
    val dock = mutableStateListOf<String>()

    // Layout hints, kept up to date by the UI so we can decide when a page is "full".
    var capacityDp = 520
    var columns = 4
    var clockReserve = 210

    var seeded: Boolean = sp.getBoolean("seeded", false)
        private set

    init { load() }

    fun markSeeded() {
        seeded = true
        sp.edit().putBoolean("seeded", true).apply()
    }

    private fun load() {
        try {
            val root = JSONObject(sp.getString("layout", null) ?: "")
            val ps = root.getJSONArray("pages")
            for (i in 0 until ps.length()) {
                val page = HomePage()
                val arr = ps.getJSONArray(i)
                for (j in 0 until arr.length()) {
                    val o = arr.getJSONObject(j)
                    when (o.getString("t")) {
                        "a" -> page.items.add(AppItem(o.getString("k")))
                        "w" -> page.items.add(WidgetItem(o.getInt("id"), o.getInt("h")))
                        "f" -> {
                            val ks = o.getJSONArray("keys")
                            page.items.add(
                                FolderItem(o.getString("id"), o.getString("name"), List(ks.length()) { ks.getString(it) })
                            )
                        }
                    }
                }
                pages.add(page)
            }
            val d = root.getJSONArray("dock")
            for (i in 0 until d.length()) dock.add(d.getString(i))
        } catch (_: Exception) {
        }
        if (pages.isEmpty()) pages.add(HomePage())
    }

    fun save() {
        val ps = JSONArray()
        for (p in pages) {
            val arr = JSONArray()
            for (item in p.items) {
                val o = JSONObject()
                when (item) {
                    is AppItem -> o.put("t", "a").put("k", item.key)
                    is WidgetItem -> o.put("t", "w").put("id", item.widgetId).put("h", item.heightDp)
                    is FolderItem -> o.put("t", "f").put("id", item.id).put("name", item.name)
                        .put("keys", JSONArray(item.keys))
                }
                arr.put(o)
            }
            ps.put(arr)
        }
        val root = JSONObject().put("pages", ps).put("dock", JSONArray(dock.toList()))
        sp.edit().putString("layout", root.toString()).apply()
    }

    private fun usedDp(index: Int, page: HomePage): Int {
        var h = if (index == 0) clockReserve else 0
        var tiles = 0
        for (item in page.items) {
            if (item is WidgetItem) h += item.heightDp + 12 else tiles++
        }
        return h + ceil(tiles / columns.toDouble()).toInt() * ROW_DP
    }

    /** Adds an item to the first page with room. Returns the page index, or -1 if it's a duplicate app. */
    fun add(item: HomeItem): Int {
        if (item is AppItem && pages.any { p -> p.items.any { it is AppItem && it.key == item.key } }) return -1
        for (i in pages.indices) {
            val p = pages[i]
            p.items.add(item)
            if (usedDp(i, p) <= capacityDp) { save(); return i }
            p.items.removeAt(p.items.lastIndex)
        }
        val np = HomePage()
        np.items.add(item)
        pages.add(np)
        save()
        return pages.lastIndex
    }

    fun remove(pageIndex: Int, item: HomeItem) {
        pages.getOrNull(pageIndex)?.items?.remove(item)
        save()
    }

    fun move(pageIndex: Int, item: HomeItem, delta: Int) {
        val items = pages.getOrNull(pageIndex)?.items ?: return
        val i = items.indexOf(item)
        val j = i + delta
        if (i < 0 || j !in items.indices) return
        val t = items[i]; items[i] = items[j]; items[j] = t
        save()
    }

    fun replace(pageIndex: Int, old: HomeItem, new: HomeItem) {
        val items = pages.getOrNull(pageIndex)?.items ?: return
        val i = items.indexOf(old)
        if (i >= 0) { items[i] = new; save() }
    }

    fun addPage(): Int { pages.add(HomePage()); save(); return pages.lastIndex }

    fun removePageIfEmpty(index: Int): Boolean {
        if (pages.size > 1 && pages.getOrNull(index)?.items?.isEmpty() == true) {
            pages.removeAt(index); save(); return true
        }
        return false
    }

    // dock
    fun addDock(key: String) { if (key !in dock && dock.size < 4) { dock.add(key); save() } }
    fun removeDock(key: String) { dock.remove(key); save() }

    // folders
    fun findFolder(id: String): Pair<Int, FolderItem>? {
        for (i in pages.indices) for (item in pages[i].items) {
            if (item is FolderItem && item.id == id) return i to item
        }
        return null
    }

    fun updateFolder(id: String, f: (FolderItem) -> FolderItem) {
        val (pi, old) = findFolder(id) ?: return
        val items = pages[pi].items
        val idx = items.indexOf(old)
        if (idx >= 0) { items[idx] = f(old); save() }
    }

    fun removeFolder(id: String) {
        val (pi, old) = findFolder(id) ?: return
        pages[pi].items.remove(old)
        save()
    }

    fun removeFromFolder(id: String, key: String) = updateFolder(id) { it.copy(keys = it.keys - key) }

    /** Drops apps that were uninstalled. */
    fun prune(valid: Set<String>) {
        if (valid.isEmpty()) return
        var changed = false
        for (p in pages) {
            if (p.items.removeAll { it is AppItem && it.key !in valid }) changed = true
            for (i in p.items.indices) {
                val f = p.items[i]
                if (f is FolderItem && f.keys.any { it !in valid }) {
                    p.items[i] = f.copy(keys = f.keys.filter { it in valid })
                    changed = true
                }
            }
        }
        if (dock.removeAll { it !in valid }) changed = true
        if (changed) save()
    }

    companion object {
        const val ROW_DP = 100
    }
}
