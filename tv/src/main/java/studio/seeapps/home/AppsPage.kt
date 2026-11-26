package studio.seeapps.home

import android.animation.ValueAnimator
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.roundToInt

class AppsPage(private val act: HomeActivity) : FrameLayout(act), Page {
    override val view: View get() = this
    private val prefs = Prefs.get(act)
    private val content = FrameLayout(act)
    private val tiles = ArrayList<TileView>()
    private val title = act.themeLabel(64, Ui.bold, spacing = -0.03f).apply { text = act.getString(R.string.tab_apps) }
    private var n = 5
    private var tw = 300
    private var th = 169
    private var x0 = 138
    private var step = 336
    private val gridY = 300
    private val rowStep get() = th + 104
    private var scroll = 0f
    private var lastIndex = 0
    private var slide: ValueAnimator? = null

    init {
        clipChildren = false
        content.clipChildren = false
        addView(content, LayoutParams(-1, -1))
        rebuild()
    }

    override fun rebuild() {
        val focusedPkg = (findFocus() as? TileView)?.pkg
        val g = Grid(prefs.favCount)
        n = g.n
        tw = g.tw
        th = g.th
        step = g.step
        x0 = g.x0
        val hadFocus = hasFocus()
        if (hadFocus) act.holdFocus()
        content.removeAllViews()
        tiles.clear()
        content.place(title, 96, 150)
        val hidden = prefs.hidden
        val list = listOf(SeeClap.tilePackage(act)) + Apps.all.filter { !SeeClap.isSeeClap(it.pkg) }.map { it.pkg }
        list.forEachIndexed { k, pkg ->
            val x = x0 + (k % n) * step
            val y = gridY + (k / n) * rowStep
            val t = TileView(act, tw, th).apply {
                this.pkg = pkg
                Banners.get(act, pkg, Ui.px(tw * 1.12f), Ui.px(th * 1.12f)) { if (this.pkg == pkg) bitmap = it }
                if (SeeClap.isSeeClap(pkg) && SeeClap.installedPackage(act) == null) badge = act.getString(R.string.install)
                alpha = if (pkg in hidden) 0.45f else 1f
                isLongClickable = true
                setOnClickListener { act.open(pkg) }
                setOnLongClickListener { act.appMenu(pkg); true }
                setOnKeyListener { _, code, e -> if (code == KeyEvent.KEYCODE_MENU && e.action == KeyEvent.ACTION_UP) { act.appMenu(pkg); true } else false }
                setOnFocusChangeListener { _, has -> if (has) onFocus(k) }
            }
            content.place(t, x, y, tw, th)
            val name = act.themeLabel(22, Ui.medium, if (pkg in hidden) 0.4f else 0.82f).apply {
                text = Apps.find(pkg)?.label ?: "SeeClap"
                gravity = Gravity.CENTER
            }
            content.place(name, x - 10, y + th + 22, tw + 20, -2)
            tiles += t
        }
        val rows = (list.size + n - 1) / n
        content.layoutParams = (content.layoutParams as LayoutParams).apply { height = Ui.px(gridY + rows * rowStep + 200) }
        lastIndex = lastIndex.coerceIn(0, max(0, tiles.size - 1))
        (tiles.firstOrNull { it.pkg == focusedPkg } ?: if (hadFocus) entry() else null)?.requestFocus()
    }

    private fun onFocus(k: Int) {
        lastIndex = k
        val row = k / n
        val top = gridY + row * rowStep
        var s = scroll
        if (row == 0) s = 0f
        else {
            if (top + th + 110 - s > 1080) s = (top + th + 110 - 1080).toFloat()
            if (top - s < 130) s = max(0f, (top - 130).toFloat())
        }
        if (s != scroll) {
            scroll = s
            slide?.cancel()
            val target = -Ui.pxf(s)
            slide = ValueAnimator.ofFloat(content.translationY, target).apply {
                duration = Ui.ms(380)
                interpolator = Ui.ease
                addUpdateListener { content.translationY = it.animatedValue as Float }
                start()
            }
        }
        act.top.fade(scroll == 0f)
    }

    override fun entry(): View? = tiles.getOrNull(if (scroll == 0f) lastIndex.coerceAtMost(n - 1) else lastIndex) ?: tiles.firstOrNull()

    override fun route(focused: View, direction: Int): View? {
        val k = tiles.indexOf(focused)
        if (k < 0) return null
        val col = k % n
        return when (direction) {
            View.FOCUS_LEFT -> if (col > 0) tiles[k - 1] else focused
            View.FOCUS_RIGHT -> if (col < n - 1 && k + 1 < tiles.size) tiles[k + 1] else focused
            View.FOCUS_UP -> if (k < n) act.top.tab(1) else tiles[k - n]
            View.FOCUS_DOWN -> tiles.getOrNull(k + n) ?: if (k / n < (tiles.size - 1) / n) tiles.last() else focused
            else -> null
        }
    }

    override fun back(): Boolean {
        if (scroll == 0f) return false
        tiles.getOrNull(lastIndex % n)?.requestFocus()
        return true
    }

    override fun onShown() {
        act.back.animateTo(blur = 1f, dim = 0.55f, bottom = 0f, left = 0f)
        act.top.fade(scroll == 0f)
    }
}
