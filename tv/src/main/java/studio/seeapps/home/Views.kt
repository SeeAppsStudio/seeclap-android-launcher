package studio.seeapps.home

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

interface BackdropHost {
    val backdrop: Backdrop
}

interface ThemeAware {
    fun applyColors()
}

fun roundOutline(radius: () -> Float) = object : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius())
}

object Squircle {

    const val ROUND = 0.20f
    private const val K = 1.52866483f

    private val C = floatArrayOf(
        1.08849323f, 0f, 0.86840689f, 0f, 0.63149399f, 0.07491100f,
        0.37282392f, 0.16905899f, 0.16905899f, 0.37282392f, 0.07491100f, 0.63149399f,
        0f, 0.86840689f, 0f, 1.08849323f, 0f, K)

    fun into(path: Path, l: Float, t: Float, r: Float, b: Float, radius: Float): Path {
        path.reset()
        val rad = minOf(radius, (r - l) / (2 * K), (b - t) / (2 * K))
        if (rad < 0.5f) {
            path.addRect(l, t, r, b, Path.Direction.CW)
            return path
        }
        val s = K * rad
        path.moveTo(l + s, t)
        path.lineTo(r - s, t)
        corner(path, r, t, -1f, 0f, 0f, 1f, rad)
        path.lineTo(r, b - s)
        corner(path, r, b, 0f, -1f, -1f, 0f, rad)
        path.lineTo(l + s, b)
        corner(path, l, b, 1f, 0f, 0f, -1f, rad)
        path.lineTo(l, t + s)
        corner(path, l, t, 0f, 1f, 1f, 0f, rad)
        path.close()
        return path
    }

    private fun corner(p: Path, cx: Float, cy: Float, ux: Float, uy: Float, vx: Float, vy: Float, rad: Float) {
        for (i in 0 until 18 step 6) p.cubicTo(
            cx + (C[i] * ux + C[i + 1] * vx) * rad, cy + (C[i] * uy + C[i + 1] * vy) * rad,
            cx + (C[i + 2] * ux + C[i + 3] * vx) * rad, cy + (C[i + 2] * uy + C[i + 3] * vy) * rad,
            cx + (C[i + 4] * ux + C[i + 5] * vx) * rad, cy + (C[i + 4] * uy + C[i + 5] * vy) * rad)
    }
}

fun squircleOutline(radius: () -> Float) = object : ViewOutlineProvider() {
    private val path = Path()

    @Suppress("DEPRECATION")
    override fun getOutline(view: View, outline: Outline) {
        Squircle.into(path, 0f, 0f, view.width.toFloat(), view.height.toFloat(), radius())
        if (Build.VERSION.SDK_INT >= 30) outline.setPath(path)
        else if (path.isConvex) outline.setConvexPath(path)
        else outline.setRoundRect(0, 0, view.width, view.height, radius())
    }
}

class FocusFraction(private val view: View, private val onChange: (Float) -> Unit) {
    var value = 0f
        private set
    private var anim: ValueAnimator? = null

    fun to(target: Float, duration: Long = 200) {
        anim?.cancel()
        val ms = Ui.ms(duration)
        if (ms == 0L) { value = target; onChange(value); view.invalidate(); return }
        anim = ValueAnimator.ofFloat(value, target).apply {
            this.duration = ms
            interpolator = Ui.ease
            addUpdateListener { value = it.animatedValue as Float; onChange(value); view.invalidate() }
            start()
        }
    }
}

private val argb = ArgbEvaluator()
fun mix(a: Int, b: Int, f: Float): Int = argb.evaluate(f.coerceIn(0f, 1f), a, b) as Int

open class GlassView(
    context: Context,
    private val radius: Float,
    elevated: Boolean = true,

    private val continuous: Boolean = false,
    private val role: Role = Role.PANEL,
) : FrameLayout(context), ThemeAware {
    enum class Role { PANEL, FAVORITES, TABS }

    companion object {

        fun bandTintAlpha(ambientFraction: Float): Int =
            if (Ui.isDay) (255 * (1f - 0.65f * ambientFraction)).toInt() else 255

        fun tint(top: Float, bottom: Float): LinearGradient {

            val start = if (Ui.isDay) Ui.scrim(0.72f) else Ui.white(0.18f)
            val end = if (Ui.isDay) Ui.scrim(0.62f) else Ui.white(0.07f)
            return LinearGradient(0f, top, 0f, bottom, start, end, Shader.TileMode.CLAMP)
        }
    }

    private val backdrop = (context as? BackdropHost)?.backdrop
    private val shape = Path()
    private val edgePath = Path()
    private val plain = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val menuShade = if (role == Role.TABS) Paint(Paint.ANTI_ALIAS_FLAG) else null
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private fun r() = if (continuous) Ui.pxf(radius) else minOf(Ui.pxf(radius), height / 2f, width / 2f)

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        if (elevated && !Ui.light) {
            outlineProvider = if (continuous) squircleOutline { r() } else roundOutline { r() }
            elevation = Ui.pxf(16)
            if (Build.VERSION.SDK_INT >= 28) {
                outlineAmbientShadowColor = Ui.black(0.5f)
                outlineSpotShadowColor = Ui.black(0.6f)
            }
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); backdrop?.watch(this) }
    override fun onDetachedFromWindow() { backdrop?.unwatch(this); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val sw = Ui.pxf(1.5f)
        edge.strokeWidth = sw
        val r = r()
        val i = sw / 2
        if (continuous) {
            Squircle.into(shape, 0f, 0f, w.toFloat(), h.toFloat(), r)
            Squircle.into(edgePath, i, i, w - i, h - i, r - i)
        } else {
            shape.reset()
            shape.addRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, Path.Direction.CW)
            edgePath.reset()
            edgePath.addRoundRect(i, i, w - i, h - i, r - i, r - i, Path.Direction.CW)
        }
        applyColors()
    }

    override fun applyColors() {
        plain.color = if (Ui.isDay) Ui.surface else 0xCC1C1C1E.toInt()
        if (height > 0) {
            fill.shader = tint(0f, height.toFloat())
            edge.shader = LinearGradient(0f, 0f, 0f, height.toFloat(),
                intArrayOf(Ui.white(0.45f), Ui.white(0.1f), Ui.white(0.05f)),
                floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (backdrop != null) backdrop.drawBehind(canvas, this, shape)
        else canvas.drawPath(shape, plain)
        val ambient = backdrop?.ambientFraction ?: 0f
        if (role == Role.FAVORITES) fill.alpha = bandTintAlpha(ambient)
        canvas.drawPath(shape, fill)
        if (menuShade != null && !Ui.isDay && ambient > 0f) {

            menuShade.color = Ui.black(0.48f * ambient)
            canvas.drawPath(shape, menuShade)
        }
        canvas.drawPath(edgePath, edge)
    }
}

class TileView(context: Context, val designW: Int, val designH: Int, private val cornerRadius: Float? = null) : View(context), ThemeAware {
    var pkg = ""
    var bitmap: Bitmap? = null
        set(v) { field = v; shader = v?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }; invalidate() }

    var badge: String? = null
        set(v) { field = v; invalidate() }
    var lifted = 1.12f

    private var shader: BitmapShader? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Ui.white(0.94f) }
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Ui.ink() }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = Path()
    private val rimPath = Path()
    private val m = Matrix()
    private val focus = FocusFraction(this) { updateBannerShape(it) }

    init {
        isFocusable = true
        isClickable = true
        if (!Ui.light) {
            outlineProvider = squircleOutline { radius() }
            elevation = Ui.pxf(4)
            if (Build.VERSION.SDK_INT >= 28) outlineSpotShadowColor = Ui.black(0.7f)
        }
    }

    private fun radius() = cornerRadius?.let { Ui.pxf(it) } ?: height * Squircle.ROUND

    override fun applyColors() {
        rim.color = Ui.ink()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = setMeasuredDimension(Ui.px(designW), Ui.px(designH))

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val r = radius()
        rim.strokeWidth = Ui.pxf(2.5f)
        val i = rim.strokeWidth / 2
        Squircle.into(rimPath, i, i, w - i, h - i, r - i)
        updateBannerShape(focus.value)
    }

    private fun updateBannerShape(f: Float) {

        val inset = f * (rim.strokeWidth + Ui.pxf(2))
        Squircle.into(shape, inset, inset, width - inset, height - inset, radius() - inset)
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap
        val sh = shader
        if (bmp != null && sh != null) {
            val scale = max(width / bmp.width.toFloat(), height / bmp.height.toFloat())
            m.setScale(scale, scale)
            m.postTranslate((width - bmp.width * scale) / 2f, (height - bmp.height * scale) / 2f)
            sh.setLocalMatrix(m)

            paint.color = Color.WHITE
            paint.shader = sh
            canvas.drawPath(shape, paint)
        } else {
            paint.shader = null
            paint.color = Ui.white(0.1f)
            canvas.drawPath(shape, paint)
        }
        val f = focus.value
        if (f > 0f) {
            rim.alpha = (f * 230).toInt()
            canvas.drawPath(rimPath, rim)
        }
        badge?.let { drawBadge(canvas, it) }
    }

    private fun drawBadge(canvas: Canvas, label: String) {
        text.typeface = Ui.semibold
        text.textSize = height * 0.1f
        val padX = height * 0.07f
        val bh = height * 0.17f
        val bw = text.measureText(label) + padX * 2
        val left = width - bw - height * 0.06f
        val top = height - bh - height * 0.06f
        canvas.drawRoundRect(left, top, left + bw, top + bh, bh / 2, bh / 2, badgeFill)
        text.color = Ui.INK
        canvas.drawText(label, left + padX, top + bh / 2 - (text.descent() + text.ascent()) / 2, text)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val scale = if (gainFocus) lifted else 1f
        animate().scaleX(scale).scaleY(scale).translationZ(if (gainFocus && !Ui.light) Ui.pxf(26) else 0f)
            .setInterpolator(Ui.ease).setDuration(Ui.ms(220)).start()
        focus.to(if (gainFocus) 1f else 0f, 200)
    }
}

class Pill(context: Context, private val tab: Boolean) : TextView(context), ThemeAware {
    var selectedTab = false
        set(v) { field = v; recolor(); invalidate() }
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val focus = FocusFraction(this) { recolor() }

    init {
        isFocusable = true
        isClickable = true
        gravity = Gravity.CENTER
        typeface = Ui.semibold
        includeFontPadding = false
        maxLines = 1
        letterSpacing = -0.005f
        if (tab) {
            textSize(24)
            setPadding(Ui.px(30), 0, Ui.px(30), 0)
        } else {
            textSize(28)
            setPadding(Ui.px(48), 0, Ui.px(48), 0)
        }
        if (!Ui.light) outlineProvider = roundOutline { height / 2f }
        recolor()
    }

    private fun textSize(v: Int) = setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.pxf(v))

    private fun recolor() {
        val rest = Ui.ink(if (tab && !selectedTab) 0.84f else 1f)
        setTextColor(mix(rest, Ui.INK, focus.value))
    }

    override fun applyColors() {
        recolor()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val f = focus.value
        val r = height / 2f
        val rest = when {
            tab && selectedTab -> Ui.ink(0.22f)
            tab -> Color.TRANSPARENT
            else -> if (Ui.isDay) Ui.scrim(0.80f) else Ui.white(0.16f)
        }
        bg.color = mix(rest, Color.WHITE, f)
        if (Color.alpha(bg.color) > 0) canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, bg)
        if (!tab && f < 1f) {
            edge.strokeWidth = Ui.pxf(1.5f)
            edge.color = Ui.ink(0.3f * (1 - f))
            val i = edge.strokeWidth / 2
            canvas.drawRoundRect(i, i, width - i, height - i, r - i, r - i, edge)
        }
        super.onDraw(canvas)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)

        val scale = if (!gainFocus || tab) 1f else 1.06f
        animate().scaleX(scale).scaleY(scale).translationZ(if (gainFocus && !Ui.light) Ui.pxf(if (tab) 6 else 14) else 0f)
            .setInterpolator(Ui.ease).setDuration(Ui.ms(200)).start()
        focus.to(if (gainFocus) 1f else 0f)
    }
}

class RowView(context: Context, icon: String?, label: String, chevron: Boolean) : LinearLayout(context), ThemeAware {
    private val iconView = ImageView(context)
    private val iconDrawable = icon?.let { IconDrawable(it) }
    private val chevronDrawable = if (chevron) IconDrawable("chevron") else null
    val labelView: TextView = context.label(28, Ui.medium, Ui.ink())
    val valueView: TextView = context.label(25, Ui.medium, Ui.ink(0.58f))
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = Path()
    private val focus = FocusFraction(this) { recolor(it) }
    var dimmed = false
        set(v) { field = v; alpha = if (v) 0.45f else 1f }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        setWillNotDraw(false)
        setPadding(Ui.px(24), 0, Ui.px(26), 0)
        if (iconDrawable != null) {
            iconView.setImageDrawable(iconDrawable)
            addView(iconView, LayoutParams(Ui.px(30), Ui.px(30)).apply { rightMargin = Ui.px(20) })
        }
        labelView.text = label
        addView(labelView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(valueView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { leftMargin = Ui.px(20) })
        if (chevronDrawable != null) {
            addView(ImageView(context).apply { setImageDrawable(chevronDrawable) },
                LayoutParams(Ui.px(22), Ui.px(22)).apply { leftMargin = Ui.px(14) })
        }
        if (!Ui.light) outlineProvider = squircleOutline { height * Squircle.ROUND }
        recolor(0f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Squircle.into(shape, 0f, 0f, w.toFloat(), h.toFloat(), h * Squircle.ROUND)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Ui.px(82), MeasureSpec.EXACTLY))

    private fun recolor(f: Float) {
        labelView.setTextColor(mix(Ui.ink(), Ui.INK, f))
        valueView.setTextColor(mix(Ui.ink(0.58f), Color.argb(148, 10, 10, 10), f))
        iconDrawable?.color = mix(Ui.ink(0.82f), Ui.INK, f)
        chevronDrawable?.color = mix(Ui.ink(0.42f), Color.argb(115, 10, 10, 10), f)
    }

    override fun applyColors() {
        recolor(focus.value)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val f = focus.value
        if (f > 0f) {
            bg.color = Ui.white(f)
            canvas.drawPath(shape, bg)
        }
        super.onDraw(canvas)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val scale = if (gainFocus) 1.035f else 1f
        animate().scaleX(scale).scaleY(scale).translationZ(if (gainFocus && !Ui.light) Ui.pxf(18) else 0f)
            .setInterpolator(Ui.ease).setDuration(Ui.ms(200)).start()
        focus.to(if (gainFocus) 1f else 0f)
    }
}

class DotsView(context: Context) : View(context) {
    var count = 0
        set(v) { field = v; requestLayout(); invalidate() }
    var index = 0
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = if (count <= 1) 0 else Ui.px(30 + (count - 1) * 9 + (count - 1) * 10)
        setMeasuredDimension(w, Ui.px(9))
    }

    override fun onDraw(canvas: Canvas) {
        if (count <= 1) return
        var x = 0f
        val h = Ui.pxf(9)
        for (i in 0 until count) {
            val w = if (i == index) Ui.pxf(30) else h
            p.color = if (i == index) Color.WHITE else Ui.white(0.42f)
            canvas.drawRoundRect(x, 0f, x + w, h, h / 2, h / 2, p)
            x += w + Ui.pxf(10)
        }
    }
}

class ProgressView(context: Context, private val trackAlpha: Float = 0.3f) : View(context) {
    var value = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val r = height / 2f
        p.color = Ui.white(trackAlpha)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), r, r, p)
        p.color = Color.WHITE
        canvas.drawRoundRect(0f, 0f, width * value, height.toFloat(), r, r, p)
    }
}

class IconDrawable(private val kind: String) : Drawable() {
    var color = Ui.ink()
        set(v) { field = v; invalidateSelf() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val s = b.width() / 24f
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        canvas.scale(s, s)
        p.color = color
        p.strokeWidth = 1.9f
        p.style = Paint.Style.STROKE
        path.reset()
        when (kind) {
            "appearance" -> { canvas.drawCircle(12f, 12f, 8.5f, p); path.addArc(RectF(3.5f, 3.5f, 20.5f, 20.5f), 90f, 180f); fillPath(canvas) }
            "favorites" -> { star(12f, 12.5f, 9f, 4f); canvas.drawPath(path, p) }
            "remote" -> { canvas.drawRoundRect(7.5f, 2.5f, 16.5f, 21.5f, 4f, 4f, p); canvas.drawCircle(12f, 8.5f, 2.2f, p); dot(canvas, 12f, 14.5f); dot(canvas, 12f, 17.8f) }
            "home" -> { path.moveTo(3.5f, 11f); path.lineTo(12f, 3.8f); path.lineTo(20.5f, 11f); path.moveTo(6f, 9.5f); path.lineTo(6f, 20f); path.lineTo(18f, 20f); path.lineTo(18f, 9.5f); canvas.drawPath(path, p) }
            "clean" -> { sparkle(10f, 13f, 7f); sparkle(18.5f, 5.5f, 3f); canvas.drawPath(path, p) }
            "seeclap" -> { canvas.drawRoundRect(3f, 5f, 21f, 19f, 4f, 4f, p); path.moveTo(10.2f, 9f); path.lineTo(15.2f, 12f); path.lineTo(10.2f, 15f); path.close(); fillPath(canvas) }
            "about", "info" -> { canvas.drawCircle(12f, 12f, 9f, p); canvas.drawLine(12f, 11f, 12f, 16.5f, p); dot(canvas, 12f, 7.6f) }
            "tv" -> { canvas.drawRoundRect(2.5f, 4.5f, 21.5f, 17f, 2.5f, 2.5f, p); canvas.drawLine(8f, 20.5f, 16f, 20.5f, p) }
            "clock" -> { canvas.drawCircle(12f, 12f, 9f, p); path.moveTo(12f, 7f); path.lineTo(12f, 12f); path.lineTo(15.5f, 14f); canvas.drawPath(path, p) }
            "motion" -> { canvas.drawLine(4f, 8f, 14f, 8f, p); canvas.drawLine(7f, 12f, 20f, 12f, p); canvas.drawLine(4f, 16f, 14f, 16f, p) }
            "size" -> { canvas.drawRoundRect(3f, 6f, 12f, 18f, 2f, 2f, p); canvas.drawRoundRect(14f, 9f, 21f, 15f, 2f, 2f, p) }
            "update" -> { path.addArc(RectF(4f, 4f, 20f, 20f), -60f, 300f); path.moveTo(16.5f, 2.8f); path.lineTo(16.6f, 6.6f); path.lineTo(12.9f, 6.9f); canvas.drawPath(path, p) }
            "restart" -> { path.addArc(RectF(4f, 4f, 20f, 20f), 200f, 300f); path.moveTo(4.2f, 6f); path.lineTo(5.4f, 9.8f); path.lineTo(9.2f, 8.8f); canvas.drawPath(path, p) }
            "check" -> { path.moveTo(5f, 12.5f); path.lineTo(10f, 17.5f); path.lineTo(19.5f, 7f); canvas.drawPath(path, p) }
            "lock" -> { canvas.drawRoundRect(5f, 10.5f, 19f, 20.5f, 2.5f, 2.5f, p); path.addArc(RectF(8f, 3.5f, 16f, 12.5f), 180f, 180f); path.moveTo(8f, 8f); path.lineTo(8f, 10.5f); path.moveTo(16f, 8f); path.lineTo(16f, 10.5f); canvas.drawPath(path, p) }
            "plus" -> { canvas.drawLine(12f, 5f, 12f, 19f, p); canvas.drawLine(5f, 12f, 19f, 12f, p) }
            "minus" -> canvas.drawLine(5f, 12f, 19f, 12f, p)
            "up" -> { path.moveTo(6f, 14.5f); path.lineTo(12f, 8.5f); path.lineTo(18f, 14.5f); canvas.drawPath(path, p) }
            "down" -> { path.moveTo(6f, 9.5f); path.lineTo(12f, 15.5f); path.lineTo(18f, 9.5f); canvas.drawPath(path, p) }
            "chevron" -> { p.strokeWidth = 2.4f; path.moveTo(9f, 5f); path.lineTo(16f, 12f); path.lineTo(9f, 19f); canvas.drawPath(path, p) }
            "trash" -> { canvas.drawLine(4f, 6.5f, 20f, 6.5f, p); path.moveTo(9f, 6.5f); path.lineTo(9.8f, 3.5f); path.lineTo(14.2f, 3.5f); path.lineTo(15f, 6.5f); path.moveTo(6f, 6.5f); path.lineTo(7f, 20.5f); path.lineTo(17f, 20.5f); path.lineTo(18f, 6.5f); canvas.drawPath(path, p) }
            "eye" -> { path.moveTo(2.5f, 12f); path.cubicTo(6f, 5.5f, 18f, 5.5f, 21.5f, 12f); path.cubicTo(18f, 18.5f, 6f, 18.5f, 2.5f, 12f); canvas.drawPath(path, p); canvas.drawCircle(12f, 12f, 3f, p) }
            "download" -> { canvas.drawLine(12f, 3.5f, 12f, 15f, p); path.moveTo(7f, 10.5f); path.lineTo(12f, 15.5f); path.lineTo(17f, 10.5f); path.moveTo(4f, 20.5f); path.lineTo(20f, 20.5f); canvas.drawPath(path, p) }
            "bolt" -> { path.moveTo(13.5f, 2.5f); path.lineTo(5.5f, 13.5f); path.lineTo(11.5f, 13.5f); path.lineTo(10.5f, 21.5f); path.lineTo(18.5f, 10.5f); path.lineTo(12.5f, 10.5f); path.close(); canvas.drawPath(path, p) }
            "shield" -> { path.moveTo(12f, 2.8f); path.lineTo(19.5f, 5.8f); path.lineTo(19.5f, 11.5f); path.cubicTo(19.5f, 16f, 16.5f, 19.5f, 12f, 21.2f); path.cubicTo(7.5f, 19.5f, 4.5f, 16f, 4.5f, 11.5f); path.lineTo(4.5f, 5.8f); path.close(); canvas.drawPath(path, p) }
            "language" -> { canvas.drawCircle(12f, 12f, 9f, p); canvas.drawOval(8f, 3f, 16f, 21f, p); canvas.drawLine(3f, 12f, 21f, 12f, p) }
            else -> canvas.drawCircle(12f, 12f, 8f, p)
        }
        canvas.restore()
    }

    private fun dot(canvas: Canvas, x: Float, y: Float) {
        p.style = Paint.Style.FILL
        canvas.drawCircle(x, y, 1.3f, p)
        p.style = Paint.Style.STROKE
    }

    private fun fillPath(canvas: Canvas) {
        p.style = Paint.Style.FILL
        canvas.drawPath(path, p)
        p.style = Paint.Style.STROKE
    }

    private fun star(cx: Float, cy: Float, outer: Float, inner: Float) {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.toRadians(-90.0 + i * 36.0)
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    private fun sparkle(cx: Float, cy: Float, r: Float) {
        val k = r * 0.28f
        path.moveTo(cx, cy - r)
        path.quadTo(cx + k, cy - k, cx + r, cy)
        path.quadTo(cx + k, cy + k, cx, cy + r)
        path.quadTo(cx - k, cy + k, cx - r, cy)
        path.quadTo(cx - k, cy - k, cx, cy - r)
        path.close()
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
