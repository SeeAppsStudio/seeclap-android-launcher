package studio.seeapps.home

import android.graphics.Bitmap
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.roundToInt

internal fun shelfScroll(scroll: Int, left: Int, right: Int, margin: Int, viewport: Int): Int =
    scroll.coerceIn((right + margin - viewport).coerceAtLeast(0), (left - margin).coerceAtLeast(0))

internal fun shelfCardWidth(height: Int, aspect: Float): Int =
    (height * aspect.coerceIn(0.5f, 2.4f)).roundToInt()

private val languagePrefix = Regex("""^\|[A-Z]{2,3}\|\s*""")
private val releaseTags = Regex("""(?i)(?:\s+(?:4K|UHD|FHD|HD|HDR10\+?|HDR|1080p|2160p|MULTI|VOSTFR|VF))+$""")
private val datedEdition = Regex("""\s+\((.+?)\)\s+(?:19|20)\d{2}$""")

internal fun shelfCoverTitle(title: String): String {
    val name = title.trim().replaceFirst(languagePrefix, "").replace(releaseTags, "").trim()
    return name.replace(datedEdition, "").ifBlank { title }
}

internal class ProgramShelf(
    private val act: HomeActivity,
    private val openProgram: (String, Program) -> Unit,
    private val returnToFavorite: () -> Unit,
) : FrameLayout(act) {
    companion object {
        private const val MARGIN = 84
        private const val GAP = 34
        private const val CARD_TOP = 66
        private const val ART_HEIGHT = 360

        private const val FOCUS_MARGIN = 28
        private const val FOCUS_SCALE = 1.06f
        const val HEIGHT = CARD_TOP + ART_HEIGHT + 102
    }

    private var owner = ""
    private var programs: List<Program> = emptyList()
    private val cards = ArrayList<Card>()
    private val headings = LinkedHashMap<Int, TextView>()
    private var selected = 0
    private var loading = false

    init {
        visibility = INVISIBLE
        isFocusable = false
        clipChildren = false
        clipToPadding = false
    }

    fun entry(): View? = if (visibility == VISIBLE) cards.getOrNull(selected) else null

    fun neighbor(focused: View, direction: Int): View? = when (direction) {
        FOCUS_LEFT -> cards.getOrNull(selected - 1) ?: focused
        FOCUS_RIGHT -> cards.getOrNull(selected + 1) ?: focused
        else -> null
    }

    fun display(pkg: String, items: List<Program>, shown: Boolean) {
        val changed = pkg != owner || programs != items
        val hadFocus = hasFocus()
        if (hadFocus && (items.isEmpty() || !shown)) returnToFavorite()
        if (changed) {
            val oldLink = programs.getOrNull(selected)?.link
            selected = if (pkg != owner || items.isEmpty()) 0 else
                items.indexOfFirst { it.link == oldLink }.takeIf { it >= 0 } ?: selected.coerceIn(items.indices)
            if (hasFocus()) act.holdFocus()
            owner = pkg
            programs = items
            removeAllViews()
            cards.clear()
            headings.clear()
            scrollTo(0, 0)
            items.forEachIndexed { index, program ->
                if (index == 0 || program.section != items[index - 1].section) {
                    val label = act.themeLabel(32, Ui.semibold).apply {
                        text = program.section?.ifBlank { Apps.find(pkg)?.label.orEmpty() } ?: act.getString(R.string.resume_kind)
                    }
                    headings[index] = place(label, 0, 4)
                }
                val card = Card(program)
                cards += card
                place(card, 0, CARD_TOP - FOCUS_MARGIN, card.designWidth + 2 * FOCUS_MARGIN, HEIGHT - CARD_TOP + FOCUS_MARGIN)
                card.setOnClickListener { openProgram(pkg, program) }
                card.setOnFocusChangeListener { _, focused ->
                    card.highlight(focused)
                    if (focused) {
                        selected = index
                        reveal()
                    }
                }
            }
            arrangeCards()
        }
        visibility = if (shown && items.isNotEmpty()) VISIBLE else INVISIBLE
        if (visibility == VISIBLE) {
            if (hadFocus && changed) entry()?.requestFocus()
            reveal()
        }
    }

    private fun arrangeCards() {
        var x = MARGIN
        cards.forEachIndexed { i, card ->
            if (i > 0 && i in headings) x += GAP
            card.designLeft = x
            card.layoutParams = (card.layoutParams as LayoutParams).apply {
                leftMargin = Ui.px(x - FOCUS_MARGIN)
            }
            x += card.designWidth + GAP
        }
        val starts = headings.keys.toList()
        for ((group, first) in starts.withIndex()) {
            val heading = headings.getValue(first)
            val start = cards[first].designLeft
            val last = cards[(starts.getOrNull(group + 1) ?: cards.size) - 1]
            heading.maxWidth = Ui.px(last.designLeft + last.designWidth - start)
            heading.moveTo(start, 4)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (visibility == VISIBLE) reveal()
    }

    private fun reveal() {
        val card = cards.getOrNull(selected) ?: return
        val left = Ui.px(card.designLeft)
        val widthPx = Ui.px(card.designWidth)
        val margin = Ui.px(MARGIN)
        if (width < widthPx + 2 * margin) return
        val end = cards.last().let { Ui.px(it.designLeft + it.designWidth) }
        val scroll = shelfScroll(scrollX, left, left + widthPx, margin, width)
        scrollTo(scroll.coerceAtMost((end + margin - width).coerceAtLeast(0)), 0)
        for ((first, label) in headings) {
            val anchor = Ui.px(cards[first].designLeft)
            label.translationX = (scrollX + margin - anchor).coerceIn(0, (label.maxWidth - label.measuredWidth).coerceAtLeast(0)).toFloat()
        }
        loadNextThumbnail()
    }

    private fun loadNextThumbnail() {
        if (loading || visibility != VISIBLE || width == 0) return
        val card = cards.firstOrNull {
            it.pendingImage && it.right > scrollX && it.left < scrollX + width
        } ?: return
        val url = card.program.image ?: return
        val pixels = Ui.px(maxOf(ART_HEIGHT, card.designWidth))
        card.pendingImage = false
        loading = true
        Bg.run {
            val bmp = runCatching { Images.load(act.applicationContext, url, pixels) }.getOrNull()
            Bg.main.post {
                loading = false
                if (bmp != null && card.parent === this) card.showImage(bmp)
                loadNextThumbnail()
            }
        }
    }

    private inner class Card(val program: Program) : FrameLayout(act) {
        var designLeft = 0
        val designWidth = shelfCardWidth(ART_HEIGHT, program.artAspect ?: (16f / 9))
        var pendingImage = program.image != null
        private val radius = minOf(designWidth, ART_HEIGHT) * 0.15f
        private val face = FrameLayout(act).apply {
            outlineProvider = squircleOutline { Ui.pxf(radius) }
        }
        private val glass = GlassView(act, radius, elevated = false, continuous = true).apply {
            val title = act.themeLabel(28, Ui.regular, 0.62f, -0.01f, lines = 5).apply {
                text = shelfCoverTitle(program.title)
                gravity = Gravity.CENTER
                if (Build.VERSION.SDK_INT >= 29) breakStrategy = LineBreaker.BREAK_STRATEGY_BALANCED
                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                if (Build.VERSION.SDK_INT >= 26) setAutoSizeTextTypeUniformWithConfiguration(
                    Ui.px(20), Ui.px(30), maxOf(1, Ui.px(2)), TypedValue.COMPLEX_UNIT_PX)
            }
            val inset = Ui.px(18)
            addView(title, LayoutParams(-1, -1).apply { setMargins(inset, inset, inset, inset) })
        }
        private val image = TileView(act, designWidth, ART_HEIGHT, radius).apply {
            isFocusable = false
            isClickable = false
            elevation = 0f
            visibility = GONE
        }
        private val caption = act.themeLabel(27, Ui.medium, lines = 2).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            text = program.title
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            visibility = INVISIBLE
        }
        init {
            isFocusable = true
            isClickable = true
            clipChildren = false
            clipToPadding = false
            descendantFocusability = FOCUS_BLOCK_DESCENDANTS
            contentDescription = program.title
            face.addView(glass, LayoutParams(-1, -1))
            face.addView(image, LayoutParams(-1, -1))
            place(face, FOCUS_MARGIN, FOCUS_MARGIN, designWidth, ART_HEIGHT)
            place(caption, FOCUS_MARGIN, FOCUS_MARGIN + ART_HEIGHT + 26, designWidth, Ui.WRAP)
        }

        fun showImage(bitmap: Bitmap) {
            image.bitmap = bitmap
            image.visibility = VISIBLE
            glass.visibility = GONE
        }

        fun highlight(focused: Boolean) {
            translationZ = if (focused) Ui.pxf(1) else 0f
            face.elevation = if (focused && !Ui.light) Ui.pxf(10) else 0f
            face.glide(scale = if (focused) FOCUS_SCALE else 1f, duration = 160)
            caption.visibility = if (focused) VISIBLE else INVISIBLE
        }
    }
}
