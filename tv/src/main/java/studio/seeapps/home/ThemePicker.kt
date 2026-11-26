package studio.seeapps.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

internal class ThemePicker(context: Context, onChoose: (String) -> Unit) : FrameLayout(context) {
    private val modes = listOf("light", "auto", "dark")
    private val cards = ArrayList<TileView>(modes.size)
    private var highlighted = modes.indexOf("auto")
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var swiping = false

    init {
        clipChildren = false
        modes.forEachIndexed { index, mode ->
            val label = context.getString(Appearance.themeLabel(mode))
            val x = 312 + index * 456
            val tile = TileView(context, 384, 216).apply {
                lifted = 1.06f
                contentDescription = label
                setOnFocusChangeListener { _, hasFocus -> if (hasFocus) highlighted = index }
                setOnClickListener { onChoose(mode) }
            }
            cards += place(tile, x, 18, 384, 216)
            place(context.themeLabel(30, Ui.semibold).apply {
                text = label
                gravity = Gravity.CENTER
                setOnClickListener { tile.performClick() }
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, x, 248, 384, 56)
        }
        val app = context.applicationContext
        Bg.load({
            val light = preview(app, true)
            val dark = preview(app, false)
            val both = light.copy(Bitmap.Config.ARGB_8888, true)
            Canvas(both).apply {
                clipRect(width / 2, 0, width, height)
                drawBitmap(dark, 0f, 0f, null)
            }
            listOf(light, both, dark)
        }) { previews -> cards.forEachIndexed { i, card -> card.bitmap = previews[i] } }
    }

    fun focusChoice(mode: String) {
        highlighted = modes.indexOf(mode).takeIf { it >= 0 } ?: modes.indexOf("auto")
        entry().requestFocus()
    }

    fun entry(): View = cards[highlighted]

    fun route(focused: View, direction: Int): View {
        val i = cards.indexOf(focused)
        if (i < 0) return entry()
        return when (direction) {
            View.FOCUS_LEFT -> cards.getOrNull(i - 1) ?: focused
            View.FOCUS_RIGHT -> cards.getOrNull(i + 1) ?: focused
            else -> focused
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                swiping = false
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val horizontal = abs(dx) > touchSlop && abs(dx) > abs(event.y - downY)
                if (!swiping && horizontal) {

                    val cancel = MotionEvent.obtain(event)
                    cancel.action = MotionEvent.ACTION_CANCEL
                    super.dispatchTouchEvent(cancel)
                    cancel.recycle()
                    swiping = true
                }
                if (swiping) {
                    if (event.actionMasked == MotionEvent.ACTION_UP && horizontal) {
                        val direction = if (dx < 0) View.FOCUS_RIGHT else View.FOCUS_LEFT
                        route(entry(), direction).requestFocusFromTouch()
                    }
                    return true
                }
            }
        }
        super.dispatchTouchEvent(event)
        return true
    }

    companion object {

        private fun preview(context: Context, day: Boolean): Bitmap {
            val resource = Appearance.ambientResource(day)
            val options = BitmapFactory.Options().apply { inMutable = true }
            val bitmap = BitmapFactory.decodeResource(context.resources, resource, options)
            val canvas = Canvas(bitmap)
            canvas.scale(bitmap.width / 320f, bitmap.height / 180f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = if (day) 0xCFF9F8F3.toInt() else 0xAA272C31.toInt()
            canvas.drawRoundRect(109f, 12f, 211f, 26f, 7f, 7f, paint)
            paint.color = if (day) 0xB5778383.toInt() else 0xAADDE3E2.toInt()
            canvas.drawRoundRect(113f, 15f, 141f, 23f, 4f, 4f, paint)
            paint.color = if (day) 0x88F8F7F2.toInt() else 0x558C989E
            canvas.drawRoundRect(13f, 125f, 307f, 169f, 10f, 10f, paint)
            for (i in 0..3) {
                val x = 36f + 64f * i
                paint.color = if (day) 0xE6F6F7F4.toInt() else 0xE62D353D.toInt()
                canvas.drawRoundRect(x, 132f, x + 56f, 162f, 5f, 5f, paint)
            }
            return bitmap
        }
    }
}
