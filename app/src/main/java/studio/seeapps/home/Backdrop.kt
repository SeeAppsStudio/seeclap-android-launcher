package studio.seeapps.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import android.view.View
import kotlin.math.max

class Layer(val key: String, val sharp: Bitmap, val blur: Bitmap) {
    val isAmbient = key.startsWith("wall:ambient:")
    val blurShader = BitmapShader(blur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
}

object Layers {
    private val cache = LruCache<String, Layer>(4)

    private val waiting = mutableMapOf<String, MutableList<(Layer?) -> Unit>>()

    private fun load(key: String, build: () -> Layer?, done: (Layer?) -> Unit) {
        cache.get(key)?.let { done(it); return }
        waiting[key]?.let { it.add(done); return }
        waiting[key] = mutableListOf(done)
        Bg.run {
            val layer = runCatching(build).getOrNull()
            Bg.main.post {
                if (layer != null) cache.put(key, layer)
                waiting.remove(key)?.forEach { it(layer) }
            }
        }
    }

    val ambientKey get() = "wall:ambient:${Appearance.ambientResource(Ui.isDay)}"

    fun homeShade(height: Float): LinearGradient {
        val h = height.coerceAtLeast(1f)
        if (!Ui.isDay) return LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Ui.black(0.38f), Ui.black(0f), Ui.black(0f), Ui.black(0.5f)),
            floatArrayOf(0f, 0.17f, 0.52f, 1f), Shader.TileMode.CLAMP)
        return LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Ui.scrim(0.87f), Ui.scrim(0.76f), Ui.scrim(0f), Ui.scrim(0f), Ui.scrim(0.65f)),
            floatArrayOf(0f, 0.085f, 0.24f, 0.55f, 1f), Shader.TileMode.CLAMP)
    }

    fun ambient(context: Context, done: (Layer) -> Unit) {
        val key = ambientKey
        val resources = context.applicationContext.resources
        val resource = Appearance.ambientResource(Ui.isDay)
        load(key, {
            val soft = BitmapFactory.decodeResource(resources, resource)
            Layer(key, soft, soft)
        }) { layer -> if (layer != null) done(layer) }
    }

    fun image(context: Context, url: String, done: (Layer?) -> Unit) {
        val key = "img:$url"
        val app = context.applicationContext
        load(key, {
            Images.load(app, url, Images.maxWidth())?.let { sharp -> Layer(key, sharp, Blur.make(sharp)) }
        }, done)
    }
}

object Blur {

    fun make(src: Bitmap): Bitmap {
        val w = 240
        val h = max(1, w * src.height / max(1, src.width))
        val small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(small).drawBitmap(src, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        repeat(3) { boxBlur(px, w, h, 4) }
        small.setPixels(px, 0, w, 0, 0, w, h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val sat = ColorMatrix().apply { setSaturation(1.7f) }
        Canvas(out).drawBitmap(small, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(sat) })
        small.recycle()
        return out
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(px.size)
        pass(px, tmp, w, h, r, 1, w)
        pass(tmp, px, h, w, r, w, 1)
    }

    private fun pass(src: IntArray, dst: IntArray, lines: Int, len: Int, r: Int, lineStep: Int, step: Int) {
        val div = 2 * r + 1
        for (line in 0 until lines) {
            val base = line * lineStep
            var a = 0; var red = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = src[base + i.coerceIn(0, len - 1) * step]
                a += c ushr 24; red += (c shr 16) and 255; g += (c shr 8) and 255; b += c and 255
            }
            for (i in 0 until len) {
                dst[base + i * step] = ((a / div) shl 24) or ((red / div) shl 16) or ((g / div) shl 8) or (b / div)
                val out = src[base + (i - r).coerceIn(0, len - 1) * step]
                val inn = src[base + (i + r + 1).coerceIn(0, len - 1) * step]
                a += (inn ushr 24) - (out ushr 24)
                red += ((inn shr 16) and 255) - ((out shr 16) and 255)
                g += ((inn shr 8) and 255) - ((out shr 8) and 255)
                b += (inn and 255) - (out and 255)
            }
        }
    }
}

class Backdrop {
    var current: Layer? = null
        private set
    val showsProgram: Boolean get() = current?.key?.startsWith("img:") == true
    private var previous: Layer? = null
    private var t = 1f

    val ambientFraction: Float get() {
        val from = if (previous?.isAmbient == true) 1f - t else 0f
        return from + if (current?.isAmbient == true) t else 0f
    }
    private var fader: ValueAnimator? = null
    private val watchers = LinkedHashSet<View>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val m = Matrix()
    private val loc = IntArray(2)

    fun watch(v: View) { watchers += v }
    fun unwatch(v: View) { watchers -= v }
    private fun refresh() = watchers.forEach { it.invalidate() }

    fun show(layer: Layer, fade: Boolean = true) {
        if (current?.key == layer.key) return
        fader?.cancel()
        previous = if (current == null || !fade) null else current
        current = layer
        val ms = Ui.ms(650)
        if (previous == null || ms == 0L) {
            t = 1f
            previous = null
            refresh()
            return
        }
        t = 0f
        fader = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ms
            interpolator = Ui.decel
            addUpdateListener { t = it.animatedValue as Float; refresh() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { previous = null; t = 1f; refresh() }
            })
            start()
        }
    }

    fun drawSharp(canvas: Canvas, w: Int, h: Int, alpha: Float = 1f) {
        previous?.let { drawCover(canvas, it.sharp, w, h, alpha) }
        current?.let { drawCover(canvas, it.sharp, w, h, alpha * if (previous != null) t else 1f) }
    }

    fun drawBlurFull(canvas: Canvas, w: Int, h: Int, alpha: Float) {
        if (alpha <= 0f) return
        previous?.let { drawCover(canvas, it.blur, w, h, alpha) }
        current?.let { drawCover(canvas, it.blur, w, h, alpha * if (previous != null) t else 1f) }
    }

    private fun drawCover(canvas: Canvas, bmp: Bitmap, w: Int, h: Int, alpha: Float) {
        if (alpha <= 0f) return
        val scale = max(w / bmp.width.toFloat(), h / bmp.height.toFloat())
        val dw = bmp.width * scale
        val dh = bmp.height * scale

        val left = (w - dw) / 2f
        val top = (h - dh) * 0.3f
        paint.shader = null
        paint.alpha = (alpha * 255).toInt()
        canvas.drawBitmap(bmp, null, RectF(left, top, left + dw, top + dh), paint)
    }

    fun drawBehind(canvas: Canvas, view: View, shape: Path) {
        val root = view.rootView ?: return
        view.getLocationInWindow(loc)
        val rw = root.width.toFloat()
        val rh = root.height.toFloat()
        if (rw <= 0f) return
        previous?.let { slice(canvas, it, loc, rw, rh, shape, 1f) }
        current?.let { slice(canvas, it, loc, rw, rh, shape, if (previous != null) t else 1f) }
    }

    private fun slice(canvas: Canvas, layer: Layer, loc: IntArray, rw: Float, rh: Float, shape: Path, alpha: Float) {
        if (alpha <= 0f) return
        val bmp = layer.blur
        val scale = max(rw / bmp.width, rh / bmp.height)
        val left = (rw - bmp.width * scale) / 2f
        val top = (rh - bmp.height * scale) * 0.3f
        m.setScale(scale, scale)
        m.postTranslate(left - loc[0], top - loc[1])
        layer.blurShader.setLocalMatrix(m)
        paint.shader = layer.blurShader
        paint.alpha = (alpha * 255).toInt()
        canvas.drawPath(shape, paint)
        paint.shader = null
    }
}

class BackdropView(context: Context, private val backdrop: Backdrop) : View(context), ThemeAware {

    var blurMix = 0f
        set(v) { field = v; invalidate() }

    var dim = 0f
        set(v) { field = v; invalidate() }

    var bottomShade = 1f
        set(v) { field = v; invalidate() }

    var leftShade = 0f
        set(v) { field = v; invalidate() }

    private val shadePaint = Paint()
    private var anim: ValueAnimator? = null
    private var vertical: LinearGradient? = null
    private var horizontal: LinearGradient? = null

    init { applyColors() }

    override fun applyColors() {
        setBackgroundColor(Ui.surface)
        updateShades(width, height)
        invalidate()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); backdrop.watch(this) }
    override fun onDetachedFromWindow() { backdrop.unwatch(this); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = updateShades(w, h)

    private fun updateShades(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        vertical = Layers.homeShade(h.toFloat())

        horizontal = if (!Ui.isDay) LinearGradient(0f, 0f, w.toFloat(), 0f,
            intArrayOf(Ui.black(0.62f), Ui.black(0.2f), Ui.black(0f)),
            floatArrayOf(0f, 0.45f, 0.7f), Shader.TileMode.CLAMP)
        else LinearGradient(0f, 0f, w.toFloat(), 0f,
            intArrayOf(Ui.black(0.92f), Ui.black(0.88f), Ui.black(0.84f), Ui.black(0.64f), Ui.black(0f)),
            floatArrayOf(0f, 0.36f, 0.52f, 0.64f, 0.90f), Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        if (blurMix < 1f) backdrop.drawSharp(canvas, w, h, 1f)
        backdrop.drawBlurFull(canvas, w, h, blurMix)
        if (dim > 0f) {
            val alpha = if (Ui.isDay) 1f - (1f - dim) * (1f - dim) else dim
            canvas.drawColor(Ui.scrim(alpha))
        }
        val programShade = bottomShade * (1f - backdrop.ambientFraction)
        if (programShade > 0f) {
            shadePaint.shader = vertical
            shadePaint.alpha = (programShade * 255).toInt()
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), shadePaint)
        }
        if (leftShade > 0f) {
            shadePaint.shader = horizontal
            shadePaint.alpha = (leftShade * 255).toInt()
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), shadePaint)
        }
    }

    private val target = floatArrayOf(blurMix, dim, bottomShade, leftShade)

    fun animateTo(blur: Float? = null, dim: Float? = null, bottom: Float? = null, left: Float? = null, duration: Long = 320) {
        blur?.let { target[0] = it }
        dim?.let { target[1] = it }
        bottom?.let { target[2] = it }
        left?.let { target[3] = it }
        val from = floatArrayOf(blurMix, this.dim, bottomShade, leftShade)
        val to = target.copyOf()
        val ms = Ui.ms(duration)
        anim?.cancel()
        if (ms == 0L) {
            blurMix = to[0]; this.dim = to[1]; bottomShade = to[2]; leftShade = to[3]
            return
        }
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = ms
            interpolator = Ui.ease
            addUpdateListener {
                val f = it.animatedValue as Float
                blurMix = from[0] + (to[0] - from[0]) * f
                this@BackdropView.dim = from[1] + (to[1] - from[1]) * f
                bottomShade = from[2] + (to[2] - from[2]) * f
                leftShade = from[3] + (to[3] - from[3]) * f
            }
            start()
        }
    }
}
