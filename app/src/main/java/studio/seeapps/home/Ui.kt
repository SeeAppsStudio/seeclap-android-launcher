package studio.seeapps.home

import android.animation.TimeInterpolator
import android.app.ActivityManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.roundToInt

object Ui {
    var isDay = false
        private set

    fun updateTheme(context: Context) {
        isDay = Appearance.isDay(Prefs.get(context).theme, context.resources.configuration.uiMode)
    }

    val surface: Int get() = if (isDay) 0xFFDDE2DC.toInt() else NIGHT

    private fun withOpacity(rgb: Int, alpha: Float): Int =
        (rgb and 0x00ffffff) or ((alpha.coerceIn(0f, 1f) * 255).roundToInt() shl 24)

    fun ink(alpha: Float = 1f): Int = withOpacity(if (isDay) 0x172222 else Color.WHITE, alpha)
    fun scrim(alpha: Float): Int = withOpacity(if (isDay) 0xF3F3EB else Color.BLACK, alpha)

    var unit = 1f
        private set

    var light = false
        private set

    var reduceMotion = false

    lateinit var regular: Typeface
        private set
    lateinit var medium: Typeface
        private set
    lateinit var semibold: Typeface
        private set
    lateinit var bold: Typeface
        private set

    val ease: TimeInterpolator = PathInterpolator(0.2f, 0.9f, 0.25f, 1f)
    val decel: TimeInterpolator = DecelerateInterpolator(1.6f)

    fun init(context: Context) {
        updateTheme(context)
        val dm = context.resources.displayMetrics
        unit = max(dm.widthPixels, dm.heightPixels) / 1920f
        val am = context.getSystemService(ActivityManager::class.java)
        light = am == null || am.isLowRamDevice || am.memoryClass <= 96 || Runtime.getRuntime().availableProcessors() <= 2
        if (!::regular.isInitialized) {
            regular = face(400)
            medium = face(500)
            semibold = face(600)
            bold = face(700)
        }
        reduceMotion = Prefs.get(context).reduceMotion
    }

    private fun face(weight: Int): Typeface =
        if (Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.SANS_SERIF, weight, false)
        else when {
            weight >= 700 -> Typeface.create("sans-serif", Typeface.BOLD)
            weight >= 500 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else -> Typeface.create("sans-serif", Typeface.NORMAL)
        }

    fun px(v: Number): Int = (v.toFloat() * unit).roundToInt()
    fun pxf(v: Number): Float = v.toFloat() * unit

    fun ms(base: Long): Long = when {
        reduceMotion -> 0L
        light -> base * 2 / 3
        else -> base
    }

    fun white(alpha: Float): Int = Color.argb((alpha * 255).roundToInt(), 255, 255, 255)
    fun black(alpha: Float): Int = Color.argb((alpha * 255).roundToInt(), 0, 0, 0)
    const val INK = 0xFF0A0A0A.toInt()
    const val NIGHT = 0xFF050506.toInt()
    const val WRAP = -2
    const val FILL = -1
}

class Grid(n: Int) {
    val n = if (n == 5) 5 else 6

    val margin = 36
    val bw = 1920 - 2 * margin

    val pad = 24
    val gap = if (this.n == 6) 34 else 40
    val tw = (bw - 2 * pad - (this.n - 1) * gap) / this.n
    val th = Math.round(tw * 9f / 16f)
    val step = tw + gap

    val bh = pad + th + pad
    val bandLeft = margin

    val bandTop = 1080 - PEEK - GAP - bh
    val x0 = bandLeft + pad

    val gridTop = bandTop + bh + GAP

    val tileRadius = th * Squircle.ROUND

    val bandRadius = tileRadius + pad

    fun favX(i: Int, count: Int) = pad + (this.n - count.coerceIn(1, this.n)) * step / 2 + i * step

    companion object {

        const val PEEK = 44

        const val GAP = 26
    }
}

fun Context.label(size: Number, face: Typeface, color: Int = Color.WHITE, spacing: Float = 0f, lines: Int = 1): TextView =
    TextView(this).styled(size, face, color, spacing, lines)

fun Context.themeLabel(size: Number, face: Typeface, opacity: Float = 1f, spacing: Float = 0f, lines: Int = 1): TextView =
    object : TextView(this), ThemeAware {
        override fun applyColors() = setTextColor(Ui.ink(opacity))
    }.styled(size, face, Ui.ink(opacity), spacing, lines)

private fun TextView.styled(size: Number, face: Typeface, color: Int, spacing: Float, lines: Int): TextView =
    apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.pxf(size))
        typeface = face
        setTextColor(color)
        letterSpacing = spacing
        includeFontPadding = false
        maxLines = lines
        ellipsize = TextUtils.TruncateAt.END
    }

fun <T : View> FrameLayout.place(view: T, x: Number, y: Number, w: Number = Ui.WRAP, h: Number = Ui.WRAP): T {
    val lp = FrameLayout.LayoutParams(size(w), size(h))
    lp.leftMargin = Ui.px(x)
    lp.topMargin = Ui.px(y)
    addView(view, lp)
    return view
}

fun size(v: Number): Int = when (v.toInt()) {
    Ui.WRAP -> ViewGroup.LayoutParams.WRAP_CONTENT
    Ui.FILL -> ViewGroup.LayoutParams.MATCH_PARENT
    else -> Ui.px(v)
}

fun View.moveTo(x: Number, y: Number) {
    val lp = layoutParams as FrameLayout.LayoutParams
    lp.leftMargin = Ui.px(x)
    lp.topMargin = Ui.px(y)
    layoutParams = lp
}

fun View.glide(alpha: Float? = null, ty: Float? = null, scale: Float? = null, duration: Long = 260, then: (() -> Unit)? = null) {
    val a = animate().setInterpolator(Ui.ease).setDuration(Ui.ms(duration))
    alpha?.let { a.alpha(it) }
    ty?.let { a.translationY(Ui.pxf(it)) }
    scale?.let { a.scaleX(it).scaleY(it) }
    a.withEndAction { then?.invoke() }
    a.start()
}

fun View.show(visible: Boolean, duration: Long = 200) {
    if (visible) {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
        }
        glide(alpha = 1f, duration = duration)
    } else if (visibility == View.VISIBLE) {
        glide(alpha = 0f, duration = duration) { visibility = View.INVISIBLE }
    }
}
