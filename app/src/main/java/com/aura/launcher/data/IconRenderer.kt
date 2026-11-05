package com.aura.launcher.data

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** Draws any app icon (adaptive or legacy) into a bitmap masked with the user's chosen shape. */
object IconRenderer {
    fun render(src: Drawable, size: Int, shape: IconShape, masked: Boolean, seed: String = ""): Bitmap {
        val d = src.constantState?.newDrawable()?.mutate() ?: src
        if (masked && shape == IconShape.PUFFY) return puffy(d, size, seed)
        if (!masked || shape == IconShape.ORIGINAL) {
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, size, size)
            d.draw(Canvas(out))
            return out
        }
        val content = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cc = Canvas(content)
        if (d is AdaptiveIconDrawable) {
            // Layers are 108dp wide, the visible window is the central 72dp.
            val big = (size * 1.5f).toInt()
            val tmp = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888)
            val tc = Canvas(tmp)
            d.background?.let { it.setBounds(0, 0, big, big); it.draw(tc) }
            d.foreground?.let { it.setBounds(0, 0, big, big); it.draw(tc) }
            val off = (big - size) / 2f
            cc.drawBitmap(tmp, -off, -off, null)
            tmp.recycle()
        } else {
            cc.drawColor(0xFFF3EFEA.toInt())
            val inset = (size * 0.16f).toInt()
            d.setBounds(inset, inset, size - inset, size - inset)
            d.draw(cc)
        }
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(content, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val r = size * when (shape) {
            IconShape.CIRCLE -> 0.5f
            IconShape.SQUIRCLE -> 0.36f
            IconShape.ROUNDED -> 0.24f
            else -> 0.08f
        }
        Canvas(out).drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r, paint)
        content.recycle()
        return out
    }

    // ---------------------------------------------------------------- puffy / candy style

    private fun averageColor(dr: Drawable?): Int? {
        if (dr == null) return null
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        dr.setBounds(0, 0, 16, 16)
        dr.draw(Canvas(bmp))
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (y in 0 until 16) for (x in 0 until 16) {
            val p = bmp.getPixel(x, y)
            if (Color.alpha(p) > 128) { r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++ }
        }
        bmp.recycle()
        return if (n < 8) null else Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    /** Turns an icon's average colour into a sweet, saturated candy tone. Greys get a colour from the package name. */
    private fun candy(avg: Int?, seed: String): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(avg ?: Color.GRAY, hsv)
        if (avg == null || hsv[1] < 0.16f) {
            hsv[0] = ((seed.hashCode() and 0x7fffffff) % 360).toFloat()
            hsv[1] = 0.5f
        }
        hsv[1] = hsv[1].coerceIn(0.40f, 0.80f)
        hsv[2] = hsv[2].coerceIn(0.90f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun mix(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt(),
    )

    /** Glossy inflated bubble tile with the app's glyph on top. */
    private fun puffy(d: Drawable, size: Int, seed: String): Bitmap {
        val s = size.toFloat()
        val layer = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        val avg: Int?
        if (d is AdaptiveIconDrawable) {
            avg = averageColor(d.background) ?: averageColor(d)
            val big = (s * 1.5f).toInt()
            val tmp = Bitmap.createBitmap(big, big, Bitmap.Config.ARGB_8888)
            d.foreground?.let { it.setBounds(0, 0, big, big); it.draw(Canvas(tmp)) }
            val m = s * 0.94f
            val o = (s - m) / 2f
            lc.drawBitmap(
                tmp,
                Rect(big / 6, big / 6, big * 5 / 6, big * 5 / 6),
                RectF(o, o, o + m, o + m),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
            tmp.recycle()
        } else {
            val inset = (s * 0.17f).toInt()
            d.setBounds(inset, inset, size - inset, size - inset)
            d.draw(lc)
            avg = averageColor(d)
        }

        val base = candy(avg, seed)
        val light = mix(base, Color.WHITE, 0.50f)
        val deep = mix(base, Color.BLACK, 0.22f)

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val padX = s * 0.07f
        val rect = RectF(padX, s * 0.05f, s - padX, s - s * 0.10f)
        val r = rect.width() * 0.42f

        // soft coloured drop shadow
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = base
            setShadowLayer(s * 0.06f, 0f, s * 0.04f, Color.argb(120, Color.red(deep), Color.green(deep), Color.blue(deep)))
        })
        // body: light top, candy middle, deeper bottom
        c.drawRoundRect(rect, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                intArrayOf(light, base, deep), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
        })
        // bright rim
        val sw = s * 0.028f
        val rim = RectF(rect.left + sw / 2, rect.top + sw / 2, rect.right - sw / 2, rect.bottom - sw / 2)
        c.drawRoundRect(rim, r - sw / 2, r - sw / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = sw
            shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0xB3FFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        // glossy highlight
        val gl = RectF(
            rect.left + rect.width() * 0.16f, rect.top + rect.height() * 0.06f,
            rect.left + rect.width() * 0.60f, rect.top + rect.height() * 0.30f,
        )
        c.drawOval(gl, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, gl.top, 0f, gl.bottom, 0x99FFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        // glyph with a tiny shadow
        val dy = rect.centerY() - s / 2f
        c.drawBitmap(layer, 0f, dy + s * 0.018f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.argb(80, Color.red(deep), Color.green(deep), Color.blue(deep)), PorterDuff.Mode.SRC_IN)
        })
        c.drawBitmap(layer, 0f, dy, Paint(Paint.FILTER_BITMAP_FLAG))
        layer.recycle()
        return out
    }
}

/** Minimal ADW/Nova-style icon pack reader (appfilter.xml). */
class IconPack(ctx: Context, val pkg: String) {
    private val res: Resources = ctx.packageManager.getResourcesForApplication(pkg)
    private val map = HashMap<String, String>()

    init {
        try {
            val id = res.getIdentifier("appfilter", "xml", pkg)
            val parser: XmlPullParser = if (id != 0) {
                res.getXml(id)
            } else {
                XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(res.assets.open("appfilter.xml"), "utf-8")
                }
            }
            val rx = Regex("ComponentInfo\\{([^}]+)\\}")
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = parser.getAttributeValue(null, "component")
                    val dr = parser.getAttributeValue(null, "drawable")
                    if (comp != null && dr != null) {
                        rx.find(comp)?.groupValues?.get(1)?.let { map[it] = dr }
                    }
                }
                ev = parser.next()
            }
        } catch (_: Exception) {
        }
    }

    fun drawableFor(appPkg: String, appCls: String): Drawable? {
        val name = map["$appPkg/$appCls"] ?: return null
        val id = res.getIdentifier(name, "drawable", pkg)
        if (id == 0) return null
        return try { res.getDrawable(id, null) } catch (_: Exception) { null }
    }
}
