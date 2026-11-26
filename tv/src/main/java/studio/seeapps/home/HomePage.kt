package studio.seeapps.home

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class HomePage(private val act: HomeActivity) : FrameLayout(act), Page {
    override val view: View get() = this
    private val prefs = Prefs.get(act)

    private enum class State { QUIET, INFO, ALL }
    private var state = State.QUIET

    private var n = 5
    private var tw = 300
    private var th = 169
    private var gap = 36
    private var pad = 40
    private var bw = 0
    private var bh = 0
    private var bandLeft = 0
    private var bandTop = 0
    private var favLeft: (Int) -> Int = { pad + it * step }
    private val x0 get() = bandLeft + pad
    private val step get() = tw + gap
    private val rowStep get() = th + 91
    private val allTop = 150
    private var gridY = 0
    private val lift get() = bandTop - allTop

    private val content = FrameLayout(act)

    private val grid = FrameLayout(act).apply { clipChildren = false; clipToPadding = false }
    private var band: GlassView? = null
    private val favTiles = ArrayList<TileView>()
    private val gridTiles = ArrayList<TileView>()
    private val gridName = act.themeLabel(24, Ui.semibold).apply { gravity = Gravity.CENTER; alpha = 0f }

    private val info = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; alpha = 0f; visibility = View.INVISIBLE }

    private val infoBanner = TileView(act, 84, 47).apply { isFocusable = false; isClickable = false }
    private val infoKind = act.label(22, Ui.semibold, Ui.white(0.85f))
    private val infoTitle = act.label(72, Ui.bold, Color.WHITE, -0.03f)
    private val infoMeta = act.label(24, Ui.medium, Ui.white(0.78f))
    private val infoProgress = ProgressView(act)
    private val infoLeft = act.label(24, Ui.medium, Ui.white(0.78f))

    private val infoLogo = ImageView(act).apply {
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_START
        maxWidth = Ui.px(720)
        maxHeight = Ui.px(150)
        visibility = View.GONE
    }
    private val infoOverview = act.label(26, Ui.medium, Ui.white(0.82f), lines = 3).apply { setLineSpacing(Ui.pxf(6), 1f) }
    private val infoButtons = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }

    private val logoCache = android.util.LruCache<String, Bitmap>(8)
    private val btnPlay = Pill(act, tab = false)
    private val btnRestart = Pill(act, tab = false)
    private val dots = DotsView(act)
    private val shelf = ProgramShelf(act, ::openProgram) { entry()?.requestFocus() }

    private var lastFav = 0
    private var programs: List<Program> = emptyList()
    private var heroIndex = 0
    private var heroPkg = ""
    private var scroll = 0f
    private var slide: ValueAnimator? = null
    private val ticker = Runnable { advance(1) }

    init {
        clipChildren = false
        clipToPadding = false
        content.clipChildren = false
        addView(content, LayoutParams(-1, -1))
        buildInfo()
        addView(shelf)
        rebuild()
    }

    private fun buildInfo() {
        val head = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(infoBanner, LinearLayout.LayoutParams(Ui.px(84), Ui.px(47)))
        head.addView(infoKind, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = Ui.px(14) })

        info.gravity = Gravity.BOTTOM
        info.addView(head)
        info.addView(infoLogo, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.px(18) })
        info.addView(infoTitle, LinearLayout.LayoutParams(Ui.px(1100), -2).apply { topMargin = Ui.px(18) })
        val meta = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        meta.addView(infoMeta)
        meta.addView(infoProgress, LinearLayout.LayoutParams(Ui.px(220), Ui.px(6)).apply { leftMargin = Ui.px(18) })
        meta.addView(infoLeft, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = Ui.px(18) })
        info.addView(meta, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.px(18) })
        info.addView(infoOverview, LinearLayout.LayoutParams(Ui.px(900), -2).apply { topMargin = Ui.px(16) })
        infoButtons.addView(btnPlay, LinearLayout.LayoutParams(-2, Ui.px(72)))
        infoButtons.addView(btnRestart, LinearLayout.LayoutParams(-2, Ui.px(72)).apply { leftMargin = Ui.px(18) })
        info.clipChildren = false
        info.addView(infoButtons, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.px(32) })
        btnPlay.setOnFocusChangeListener { _, has -> if (has) setState(State.INFO) }
        btnRestart.setOnFocusChangeListener { _, has -> if (has) setState(State.INFO) }
        btnPlay.setOnClickListener { current()?.let { openProgram(heroPkg, it) } }
        btnRestart.setOnClickListener { current()?.let { SeeClap.open(act, it.restartLink ?: it.link) } }
    }

    private fun openProgram(pkg: String, item: Program) {
        val opened = if (SeeClap.isSeeClap(pkg)) SeeClap.open(act, item.link) else TvListings.open(act, pkg, item.link)
        if (!opened) act.toast(act.getString(R.string.cannot_open))
    }

    override fun rebuild() {
        val focusedPkg = (findFocus() as? TileView)?.pkg
        val focusedShelf = shelf.hasFocus()
        val g = Grid(prefs.favCount)
        n = g.n
        tw = g.tw
        th = g.th
        gap = g.gap
        pad = g.pad
        val favs = Apps.favorites(act)

        bw = g.bw
        bh = g.bh
        bandLeft = g.bandLeft
        bandTop = g.bandTop
        favLeft = { i -> g.favX(i, favs.size) }

        val hadFocus = hasFocus()
        if (hadFocus) act.holdFocus()
        content.removeAllViews()
        grid.removeAllViews()
        favTiles.clear()
        gridTiles.clear()
        val b = GlassView(act, g.bandRadius, continuous = true, role = GlassView.Role.FAVORITES)
        band = b
        content.place(b, bandLeft, bandTop, bw, bh)
        content.addView(grid, LayoutParams(-1, -1))
        favs.forEachIndexed { i, pkg ->
            val t = tile(pkg, tw, th)
            b.place(t, favLeft(i), g.pad, tw, th)
            t.setOnFocusChangeListener { _, has -> if (has) onFavFocus(i, t) }
            favTiles += t
        }
        gridY = g.gridTop
        val rest = Apps.all.filter { it.pkg !in favs && it.pkg !in prefs.hidden && !SeeClap.isSeeClap(it.pkg) }
        rest.forEachIndexed { k, app ->
            val t = tile(app.pkg, tw, th)
            grid.place(t, x0 + (k % n) * step, gridY + (k / n) * rowStep, tw, th)
            t.setOnFocusChangeListener { _, has -> if (has) onGridFocus(k, t) }
            gridTiles += t
        }
        grid.place(gridName, 0, 0, tw + 120, -2)
        val rows = (rest.size + n - 1) / n
        content.layoutParams = (content.layoutParams as LayoutParams).apply { height = Ui.px(gridY + rows * rowStep + 240) }

        removeView(info)
        place(info, x0 - 4, bandTop - 648, Ui.WRAP, 580)
        removeView(dots)
        addView(dots, LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = Ui.px(bandTop - 44) })
        shelf.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, Ui.px(ProgramShelf.HEIGHT)).apply {
            topMargin = Ui.px((bandTop - ProgramShelf.HEIGHT) / 2 - 8)
        }

        lastFav = lastFav.coerceIn(0, max(0, favTiles.size - 1))
        val again = (favTiles + gridTiles).firstOrNull { it.pkg == focusedPkg }
        if (focusedShelf && heroPkg in favs) {
            lastFav = favs.indexOf(heroPkg)
            shelf.entry()?.requestFocus()
        }
        else if (again != null) again.requestFocus()
        else {
            if (state != State.QUIET) setState(State.QUIET, animate = false)
            if (hadFocus) entry()?.requestFocus()
        }
        if (heroPkg !in favs && favs.isNotEmpty()) heroFor(favs[lastFav])
    }

    private fun tile(pkg: String, w: Int, h: Int): TileView = TileView(act, w, h).apply {
        this.pkg = pkg
        Banners.get(act, pkg, Ui.px(w * 1.12f), Ui.px(h * 1.12f)) { if (this.pkg == pkg) bitmap = it }
        if (SeeClap.isSeeClap(pkg) && SeeClap.installedPackage(act) == null) badge = act.getString(R.string.install)
        isLongClickable = true
        setOnClickListener { act.open(pkg) }
        setOnLongClickListener { act.appMenu(pkg); true }
        setOnKeyListener { _, code, e ->
            if (code == KeyEvent.KEYCODE_MENU && e.action == KeyEvent.ACTION_UP) { act.appMenu(pkg); true } else false
        }
    }

    override fun entry(): View? = favTiles.getOrNull(lastFav) ?: favTiles.firstOrNull()

    fun goTop() {
        setState(State.QUIET)
        lastFav = 0
        favTiles.firstOrNull()?.requestFocus()
    }

    override fun route(focused: View, direction: Int): View? {
        val fi = favTiles.indexOf(focused)
        if (fi >= 0) return when (direction) {
            View.FOCUS_LEFT -> favTiles.getOrNull(fi - 1) ?: focused
            View.FOCUS_RIGHT -> favTiles.getOrNull(fi + 1) ?: focused
            View.FOCUS_UP -> shelf.entry() ?: if (current() != null) { setState(State.INFO); btnPlay } else act.top.tab(0)
            View.FOCUS_DOWN -> gridTiles.getOrNull(min(favColumn(fi), gridTiles.size - 1)) ?: focused
            else -> null
        }
        if (shelf.hasFocus()) return when (direction) {
            View.FOCUS_UP -> act.top.tab(0)
            View.FOCUS_DOWN -> entry()
            else -> shelf.neighbor(focused, direction)
        }
        if (focused === btnPlay || focused === btnRestart) return when (direction) {
            View.FOCUS_LEFT -> if (focused === btnRestart) btnPlay else { advance(-1); focused }
            View.FOCUS_RIGHT -> if (focused === btnPlay && btnRestart.visibility == View.VISIBLE) btnRestart else { advance(1); focused }
            View.FOCUS_UP -> act.top.tab(0)
            View.FOCUS_DOWN -> favTiles.getOrNull(lastFav) ?: focused
            else -> null
        }
        val gi = gridTiles.indexOf(focused)
        if (gi >= 0) {
            val col = gi % n
            return when (direction) {
                View.FOCUS_LEFT -> if (col > 0) gridTiles[gi - 1] else focused
                View.FOCUS_RIGHT -> if (col < n - 1 && gi + 1 < gridTiles.size) gridTiles[gi + 1] else focused
                View.FOCUS_UP -> if (gi < n) favTiles.getOrNull((col - favShift()).coerceIn(0, max(0, favTiles.size - 1))) ?: focused else gridTiles[gi - n]
                View.FOCUS_DOWN -> gridTiles.getOrNull(gi + n)
                    ?: if (gi / n < (gridTiles.size - 1) / n) gridTiles.last() else focused
                else -> null
            }
        }
        return null
    }

    private fun favShift() = (n - favTiles.size.coerceIn(1, n)) / 2
    private fun favColumn(i: Int) = (i + (n - favTiles.size.coerceIn(1, n)) / 2f).roundToInt().coerceIn(0, n - 1)

    override fun back(): Boolean {
        if (shelf.hasFocus()) {
            entry()?.requestFocus()
            return true
        }
        if (state == State.QUIET && lastFav == 0 && findFocus() === favTiles.firstOrNull()) return false
        goTop()
        return true
    }

    private fun onFavFocus(i: Int, t: TileView) {
        lastFav = i
        setState(State.QUIET)
        if (t.pkg != heroPkg) heroFor(t.pkg)
    }

    private fun onGridFocus(k: Int, t: TileView) {
        setState(State.ALL)
        val row = k / n
        gridName.text = Apps.find(t.pkg)?.label ?: ""
        gridName.moveTo(x0 + (k % n) * step - 60, gridY + row * rowStep + th + 24)
        gridName.alpha = 0f
        gridName.glide(alpha = 1f, duration = 160)

        val top = gridY + Grid.GAP + row * rowStep - lift
        var s = scroll
        if (row == 0) s = 0f
        else {
            if (top + th + 90 - s > 1080) s = (top + th + 90 - 1080).toFloat()
            if (top - s < 130) s = max(0f, (top - 130).toFloat())
        }
        if (s != scroll) {
            scroll = s
            slideContent(-(lift + scroll))
        }
        act.top.fade(scroll == 0f)
    }

    private fun setState(s: State, animate: Boolean = true) {
        if (s == state && animate) return
        state = s
        val b = band ?: return
        updateShelf()
        when (s) {
            State.QUIET -> {
                scroll = 0f
                slideContent(0f)
                grid.glide(ty = 0f, duration = 420)
                b.glide(alpha = 1f)
                gridName.glide(alpha = 0f, duration = 120)
                act.back.animateTo(blur = 0f, dim = 0f, bottom = 1f, left = 0f)
                act.top.fade(true)
                dots.glide(alpha = 1f)
                showRestInfo()
                restartTicker()
            }
            State.INFO -> {

                info.visibility = View.VISIBLE
                info.glide(alpha = 1f, ty = 0f, duration = 240)
                infoButtons.show(true, 240)
                act.back.animateTo(left = 1f)
                stopTicker()
            }
            State.ALL -> {
                scroll = 0f
                slideContent(-lift.toFloat())

                grid.glide(ty = Grid.GAP.toFloat(), duration = 420)
                info.show(false, 120)
                act.back.animateTo(blur = 1f, dim = 0.5f, bottom = 0f, left = 0f, duration = 380)
                dots.glide(alpha = 0f)
                stopTicker()
            }
        }
    }

    private fun slideContent(targetDesign: Float) {
        val target = Ui.pxf(targetDesign)
        slide?.cancel()
        val from = content.translationY
        if (from == target) return
        val ms = Ui.ms(420)
        if (ms == 0L) { content.translationY = target; band?.invalidate(); return }
        slide = ValueAnimator.ofFloat(from, target).apply {
            duration = ms
            interpolator = Ui.ease
            addUpdateListener { content.translationY = it.animatedValue as Float; band?.invalidate() }
            start()
        }
    }

    private fun current(): Program? = if (source?.legacy == false) programs.getOrNull(heroIndex) else null

    private fun updateShelf() {
        val cards = if (source?.legacy == true) programs else emptyList()
        shelf.display(heroPkg, cards, state == State.QUIET && act.homeBackdropVisible())
    }

    private fun heroFor(pkg: String) {
        if (pkg != heroPkg) {
            heroPkg = pkg
            heroIndex = 0
            programs = emptyList()
            source = null
            refreshProgramSource()
        }
        if (!act.homeBackdropVisible()) return
        if (state == State.QUIET && needsTvPermission(pkg)) act.offerTvListings()
        updateShelf()
        dots.count = if (source?.legacy == false) programs.size else 0
        val p = current()
        if (p == null) {
            showWallpaper()
            showRestInfo()
            stopTicker()
            return
        }
        dots.index = heroIndex
        fillInfo(p)
        showRestInfo()
        val url = p.image
        if (url == null) showWallpaper()
        else Layers.image(act, url) { layer ->
            if (act.homeBackdropVisible() && current() === p) {
                if (layer != null) act.backdrop.show(layer) else showWallpaper()
            }
        }
        restartTicker()
    }

    fun refreshWallpaper() {
        val image = current()?.image
        if (image == null || act.backdrop.current?.key != "img:$image") showWallpaper()
    }

    private fun showWallpaper() {
        val expected = current()
        Layers.ambient(act) { layer ->
            if (!act.homeBackdropVisible() || current() !== expected ||
                layer.key != Layers.ambientKey) return@ambient

            if (expected?.image != null && act.backdrop.current?.key == "img:${expected.image}") return@ambient
            act.backdrop.show(layer)
        }
    }

    private fun showRestInfo() {
        if (state != State.QUIET) return
        val shown = current() != null
        act.back.animateTo(left = if (shown) 0.75f else 0f)
        if (!shown) {
            info.show(false, 160)
            return
        }
        infoButtons.animate().cancel()
        infoButtons.alpha = 0f
        infoButtons.visibility = View.INVISIBLE
        info.translationY = 0f
        info.show(true, 240)
    }

    private fun fillTitle(p: Program) {
        infoTitle.text = p.title
        val url = p.logo
        val cached = url?.let { logoCache.get(it) }
        if (url == null || cached != null) {
            infoLogo.setImageBitmap(cached)
            infoLogo.visibility = if (cached != null) View.VISIBLE else View.GONE
            infoTitle.visibility = if (cached != null) View.GONE else View.VISIBLE
            return
        }
        infoLogo.visibility = View.GONE
        infoTitle.visibility = View.INVISIBLE
        val app = act.applicationContext
        Bg.run {
            val bmp = runCatching { Images.load(app, url, Ui.px(720), alpha = true) }.getOrNull()
            Bg.main.post {
                if (current() !== p) return@post
                if (bmp != null) {
                    logoCache.put(url, bmp)
                    infoLogo.setImageBitmap(bmp)
                    infoLogo.visibility = View.VISIBLE
                    infoTitle.visibility = View.GONE
                } else {
                    infoTitle.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun fillInfo(p: Program) {
        val pkg = heroPkg
        if (infoBanner.pkg != pkg) {
            infoBanner.pkg = pkg
            infoBanner.bitmap = null
        }
        Banners.get(act, pkg, Ui.px(168), Ui.px(94)) { if (heroPkg == pkg) infoBanner.bitmap = it }
        infoKind.text = when {
            p.live -> act.getString(R.string.live_now)
            p.section == null -> act.getString(R.string.resume_kind)
            else -> p.section.ifBlank { Apps.find(pkg)?.label.orEmpty() }
        }
        fillTitle(p)
        infoOverview.text = p.overview
        infoOverview.visibility = if (p.overview.isEmpty()) View.GONE else View.VISIBLE
        infoMeta.text = p.meta
        infoMeta.visibility = if (p.meta.isEmpty()) View.GONE else View.VISIBLE
        infoProgress.value = p.progress
        infoProgress.visibility = if (p.progress > 0f) View.VISIBLE else View.GONE
        infoLeft.text = if (!p.live && p.remainingMin > 0) act.getString(R.string.minutes_left, p.remainingMin) else ""
        infoLeft.visibility = if (infoLeft.text.isEmpty()) View.GONE else View.VISIBLE
        btnPlay.text = act.getString(when {
            p.opensDetails -> R.string.open
            p.live -> R.string.watch
            else -> R.string.resume
        })
        btnRestart.text = act.getString(R.string.from_start)
        btnRestart.visibility = if (!p.live && p.restartLink != null) View.VISIBLE else View.GONE
    }

    private fun advance(d: Int) {
        val list = programs
        if (current() == null || list.size < 2) return
        heroIndex = (heroIndex + d + list.size) % list.size
        heroFor(heroPkg)
    }

    private fun restartTicker() {
        stopTicker()
        if (act.homeBackdropVisible() && state == State.QUIET && !Ui.reduceMotion && current() != null && programs.size > 1) postDelayed(ticker, 8000)
    }

    private fun stopTicker() { removeCallbacks(ticker) }

    fun onResumeHome() {
        resumed = true
        refreshProgramSource()
        if (act.homeBackdropVisible()) refreshWallpaper()
        restartTicker()
    }

    fun onPauseHome() {
        resumed = false
        programGeneration++
        stopTicker()
        stopWatchingPrograms()
    }

    private var resumed = false
    private var programGeneration = 0
    private var source: ProgramSource? = null

    fun showsPackage(pkg: String) = pkg == heroPkg

    fun needsTvPermission(pkg: String) = resumed && source?.let { it.legacy && it.pkg == pkg } == true && !TvListings.allowed(act)

    fun refreshProgramSource() {
        if (!resumed) return
        val pkg = heroPkg
        val generation = ++programGeneration
        val previous = source
        stopWatchingPrograms()
        Bg.load({ runCatching { Programs.source(act, pkg) } }) { result ->
            if (generation != programGeneration) return@load
            val found = result.getOrNull()
            source = found
            if (found == null || needsTvPermission(found.pkg)) {
                displayPrograms(emptyList())
                return@load
            }
            if (previous != found && programs.isNotEmpty()) displayPrograms(emptyList())
            val uris = if (found.legacy) TvListings.observedUris() else listOf(SeeClap.programsUri(found.pkg))
            for (uri in uris) runCatching { act.contentResolver.registerContentObserver(uri, found.legacy, programsObserver) }
            loadPrograms()
        }
    }

    private fun loadPrograms() {
        if (!resumed) return
        val currentSource = source ?: return
        val generation = ++programGeneration
        Bg.load({ Programs.read(act, currentSource) }) { list ->
            if (generation == programGeneration) displayPrograms(list)
        }
    }

    private fun displayPrograms(list: List<Program>) {
        programs = list
        if (heroIndex >= list.size) heroIndex = 0
        if (list.isEmpty() && state == State.INFO) {
            setState(State.QUIET)
            if (hasFocus()) entry()?.requestFocus()
        }
        heroFor(heroPkg)
    }

    private val reloadPrograms = Runnable { loadPrograms() }
    private val programsObserver = object : ContentObserver(Bg.main) {
        override fun onChange(selfChange: Boolean) {
            if (!resumed || source == null) return
            removeCallbacks(reloadPrograms)
            postDelayed(reloadPrograms, 400)
        }
    }
    private fun stopWatchingPrograms() {
        runCatching { act.contentResolver.unregisterContentObserver(programsObserver) }
        removeCallbacks(reloadPrograms)
    }

    override fun onShown() {
        if (state == State.QUIET) {
            act.back.animateTo(blur = 0f, dim = 0f, bottom = 1f, left = 0f)
            showRestInfo()
        } else setState(State.QUIET)
        act.top.fade(true)
        heroFor(heroPkg.ifEmpty { SeeClap.tilePackage(act) })
    }

    override fun onHidden() { stopTicker() }
}
