package studio.seeapps.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class RootLayout(context: Context) : FrameLayout(context) {
    var router: ((View, Int) -> View?)? = null

    override fun focusSearch(focused: View?, direction: Int): View? {
        if (focused != null) router?.invoke(focused, direction)?.let { return it }
        return super.focusSearch(focused, direction)
    }
}

interface Page {
    val view: View

    fun entry(): View?

    fun route(focused: View, direction: Int): View?

    fun back(): Boolean = false
    fun onShown() {}
    fun onHidden() {}
    fun rebuild() {}
}

class TopBar(private val act: HomeActivity) : FrameLayout(act), ThemeAware {
    val tabs = ArrayList<Pill>()
    private val pill = GlassView(act, 36f, role = GlassView.Role.TABS)
    private val clock = TextClock(act)
    private var pending: Runnable? = null
    private var shown = true

    init {
        clipChildren = false

        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(Ui.px(8), Ui.px(8), Ui.px(8), Ui.px(8))
            clipChildren = false
            clipToPadding = false
        }
        listOf(R.string.tab_home, R.string.tab_apps, R.string.tab_settings).forEachIndexed { i, res ->
            val tab = Pill(act, tab = true).apply { text = act.getString(res) }
            tab.setOnFocusChangeListener { _, has ->
                if (!has) return@setOnFocusChangeListener
                pending?.let { removeCallbacks(it) }

                pending = Runnable { if (tab.isFocused) act.showPage(i) }.also { postDelayed(it, 140) }
            }
            tab.setOnClickListener { act.showPage(i); act.focusEntry() }
            tabs += tab
            row.addView(tab, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.px(56)).apply {
                if (i > 0) leftMargin = Ui.px(6)
            })
        }
        pill.addView(row)
        addView(pill, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL).apply {
            topMargin = Ui.px(44)
        })
        clock.format12Hour = "h:mm"
        clock.format24Hour = "HH:mm"
        clock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Ui.pxf(34))
        clock.typeface = Ui.medium
        clock.letterSpacing = -0.01f
        applyColors()
        clock.includeFontPadding = false
        addView(clock, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END).apply {
            topMargin = Ui.px(54)
            rightMargin = Ui.px(96)
        })
        applyClock()
    }

    override fun applyColors() { clock.setTextColor(Ui.ink(0.95f)) }

    fun applyClock() { clock.visibility = if (Prefs.get(act).showClock) View.VISIBLE else View.GONE }

    fun select(i: Int) = tabs.forEachIndexed { k, t -> t.selectedTab = k == i }

    fun tab(i: Int): View = tabs[i]

    fun owns(v: View) = v in tabs

    fun route(focused: View, dir: Int): View? {
        val i = tabs.indexOf(focused)
        return when (dir) {
            View.FOCUS_LEFT -> tabs.getOrNull(i - 1) ?: focused
            View.FOCUS_RIGHT -> tabs.getOrNull(i + 1) ?: focused
            View.FOCUS_UP -> focused
            View.FOCUS_DOWN -> { pending?.let { removeCallbacks(it) }; act.showPage(i); act.page.entry() ?: focused }
            else -> null
        }
    }

    fun fade(visible: Boolean) {
        if (visible == shown) return
        shown = visible
        glide(alpha = if (visible) 1f else 0f, ty = if (visible) 0f else -30f, duration = 240)
    }
}

class Sheet(private val act: HomeActivity, title: String, text: String? = null, headerPkg: String? = null) : FrameLayout(act) {
    private val panel = GlassView(act, 44f, continuous = true)
    private val list = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
    private val viewport = FrameLayout(act)
    val rows = ArrayList<View>()
    var onClose: (() -> Unit)? = null
    private val maxListHeight: Int
    private var scroll = 0
    private val textView = act.themeLabel(24, Ui.medium, 0.7f, lines = 8).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setLineSpacing(Ui.pxf(6), 1f)
    }

    fun message(text: String) {
        textView.text = text
        textView.visibility = View.VISIBLE
    }

    init {
        setBackgroundColor(Ui.black(0.55f))
        isClickable = true
        val column = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.px(48), Ui.px(44), Ui.px(48), Ui.px(40))
        }
        headerPkg?.let { pkg ->
            val tile = TileView(act, 300, 169).apply { isFocusable = false }
            Banners.get(act, pkg, Ui.px(300), Ui.px(169)) { tile.bitmap = it }
            column.addView(tile, LinearLayout.LayoutParams(Ui.px(300), Ui.px(169)).apply { bottomMargin = Ui.px(26); gravity = Gravity.CENTER_HORIZONTAL })
        }
        column.addView(act.themeLabel(40, Ui.bold, spacing = -0.02f, lines = 2).apply { this.text = title; gravity = Gravity.CENTER_HORIZONTAL },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        textView.text = text ?: ""
        textView.visibility = if (text == null) View.GONE else View.VISIBLE
        column.addView(textView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.px(14) })
        viewport.clipChildren = true
        viewport.addView(list, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        column.addView(viewport, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = Ui.px(28) })
        panel.addView(column)
        addView(panel, LayoutParams(Ui.px(860), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        maxListHeight = Ui.px(if (headerPkg != null) 470 else 640)
    }

    fun row(icon: String?, label: String, value: String = "", chevron: Boolean = false, onClick: (RowView) -> Unit): RowView {
        val r = RowView(act, icon, label, chevron)
        r.valueView.text = value
        r.setOnClickListener { onClick(r) }
        r.setOnFocusChangeListener { v, has -> if (has) reveal(v) }
        list.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.px(82)).apply { if (rows.isNotEmpty()) topMargin = Ui.px(8) })
        rows += r
        return r
    }

    private fun reveal(v: View) {
        val step = Ui.px(90)
        val visible = maxListHeight
        val top = v.top
        if (top - scroll + step > visible) scroll = top + step - visible
        if (top < scroll) scroll = top
        list.animate().translationY(-scroll.toFloat()).setInterpolator(Ui.ease).setDuration(Ui.ms(220)).start()
    }

    fun show() {
        val lp = viewport.layoutParams
        lp.height = if (rows.size * Ui.px(90) > maxListHeight) maxListHeight + Ui.px(12) else ViewGroup.LayoutParams.WRAP_CONTENT
        viewport.layoutParams = lp
        act.openSheet(this)
        alpha = 0f
        panel.scaleX = 0.96f
        panel.scaleY = 0.96f
        glide(alpha = 1f, duration = 180)
        panel.glide(scale = 1f, duration = 240)
        rows.firstOrNull()?.requestFocus()
    }

    fun close() = act.closeSheet(this)

    fun route(focused: View, dir: Int): View? {
        val i = rows.indexOf(focused)
        if (i < 0) return rows.firstOrNull()
        return when (dir) {
            View.FOCUS_UP -> rows.getOrNull(i - 1) ?: focused
            View.FOCUS_DOWN -> rows.getOrNull(i + 1) ?: focused
            else -> focused
        }
    }
}

class HomeActivity : Activity(), BackdropHost {
    companion object {
        private const val TV_LISTINGS_REQUEST = 41

        @Volatile var shown: java.lang.ref.WeakReference<HomeActivity>? = null
    }

    override val backdrop = Backdrop()
    lateinit var root: RootLayout
        private set
    lateinit var back: BackdropView
        private set
    lateinit var top: TopBar
        private set
    lateinit var home: HomePage
        private set
    lateinit var apps: AppsPage
        private set
    lateinit var settings: SettingsPage
        private set
    private val pages: List<Page> get() = listOf(home, apps, settings)
    var current = 0
        private set
    val page: Page get() = pages[current]
    private var sheet: Sheet? = null
    private var setup: Setup? = null
    private var lastFocus: View? = null
    private val prefs by lazy { Prefs.get(this) }

    private val packageWatcher = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.data?.schemeSpecificPart?.let(home::showsPackage) == true) home.refreshProgramSource()
            reloadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shown = java.lang.ref.WeakReference(this)
        Ui.init(this)
        window.setBackgroundDrawable(ColorDrawable(Ui.surface))
        if (Build.VERSION.SDK_INT >= 29) window.decorView.isForceDarkAllowed = false
        root = RootLayout(this)
        back = BackdropView(this, backdrop)
        root.addView(back, FrameLayout.LayoutParams(-1, -1))
        home = HomePage(this)
        apps = AppsPage(this)
        settings = SettingsPage(this)
        for (p in pages) root.addView(p.view, FrameLayout.LayoutParams(-1, -1))
        apps.view.visibility = View.GONE
        settings.view.visibility = View.GONE

        keeper = View(this).apply { isFocusable = true }
        root.addView(keeper, FrameLayout.LayoutParams(1, 1))
        top = TopBar(this)
        root.addView(top, FrameLayout.LayoutParams(-1, -1))
        top.select(0)
        root.router = ::route
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { handleBack() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageWatcher, filter, Context.RECEIVER_EXPORTED)
        else registerReceiver(packageWatcher, filter)
        Installer.listen(installWatcher)
        reloadApps()
    }

    override fun onDestroy() {
        if (shown?.get() === this) shown = null
        runCatching { unregisterReceiver(packageWatcher) }
        Installer.unlisten(installWatcher)
        super.onDestroy()
    }

    private val installWatcher: (String, Installer.State) -> Unit = { pkg, state ->
        if (state is Installer.State.Done && SeeClap.isSeeClap(pkg)) reloadApps()
        if (state is Installer.State.Failed && setup == null && sheet == null) toast(getString(state.reason))
        settings.onInstallState(pkg, state)
        setup?.onInstallState(pkg, state)
    }

    fun reloadApps() {
        Bg.load({ Apps.load(this) }) {
            Banners.clear()
            pages.forEach { p -> p.rebuild() }
            setup?.onAppsChanged()
            if (root.findFocus() == null && setup == null && sheet == null) focusEntry()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
    }

    fun applyTheme() {
        val wasDay = Ui.isDay
        Ui.updateTheme(this)
        if (wasDay == Ui.isDay || !::top.isInitialized) return
        window.setBackgroundDrawable(ColorDrawable(Ui.surface))
        repaint(root)
        settings.applyColors()
        when {
            setup != null || current == 1 -> showPageWallpaper()
            current == 0 -> home.refreshWallpaper()
        }
    }

    private fun repaint(view: View) {
        (view as? ThemeAware)?.applyColors()
        if (view is ViewGroup) for (i in 0 until view.childCount) repaint(view.getChildAt(i))
    }

    private fun showPageWallpaper() {
        if (backdrop.showsProgram) return
        Layers.ambient(this) { layer ->
            if ((current == 1 || setup != null) && !backdrop.showsProgram &&
                layer.key == Layers.ambientKey) {
                backdrop.show(layer)
            }
        }
    }

    fun homeBackdropVisible() = current == 0 && setup == null && !isDestroyed

    fun offerTvListings() {
        if (prefs.setupDone && homeBackdropVisible() && sheet == null && !prefs.tvListingsAsked) requestTvListings()
    }

    private fun requestTvListings() {
        if (prefs.tvListingsAsked && !shouldShowRequestPermissionRationale(TvListings.PERMISSION)) {
            Apps.openDetails(this, packageName)
            return
        }
        prefs.tvListingsAsked = true
        requestPermissions(arrayOf(TvListings.PERMISSION), TV_LISTINGS_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == TV_LISTINGS_REQUEST) home.refreshProgramSource()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        Ui.reduceMotion = prefs.reduceMotion
        HomeGuardService.reload()
        Installer.resumePending(this)
        home.onResumeHome()
        settings.refresh()
        setup?.onResumeSetup()
        if (!prefs.setupDone && setup == null) startSetup()
        warnOtherGuard()
        if (BuildConfig.SITE && System.currentTimeMillis() - prefs.lastUpdateCheck > 6 * 3600_000L) {
            prefs.lastUpdateCheck = System.currentTimeMillis()
            Installer.updateSeeClap(this, auto = true)
            Installer.updateSelf(this, install = false)
        }
    }

    override fun onPause() {
        lastFocus = root.findFocus()
        home.onPauseHome()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (setup != null) return
        sheet?.let { closeSheet(it) }
        showPage(0)
        home.goTop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && Build.VERSION.SDK_INT < 33) { event.startTracking(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && Build.VERSION.SDK_INT < 33) {
            if (event.isTracking && !event.isCanceled) handleBack()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun handleBack() {
        when {
            sheet != null -> closeSheet(sheet!!)
            setup != null -> setup!!.back()
            page.back() -> {}
            top.owns(root.findFocus() ?: root) -> page.entry()?.requestFocus()
            current != 0 -> { showPage(0); home.goTop() }
            else -> home.goTop()
        }
    }

    private var keeper: View? = null

    fun holdFocus() {
        val k = keeper ?: return
        val f = root.findFocus()
        if (f != null && f !== k) k.requestFocus()
    }

    private fun route(focused: View, dir: Int): View? {
        if (focused === keeper) return page.entry() ?: focused
        sheet?.let { return it.route(focused, dir) }
        setup?.let { return it.route(focused, dir) }
        if (top.owns(focused)) return top.route(focused, dir)
        return page.route(focused, dir)
    }

    fun showPage(i: Int) {
        if (i == current) return
        val old = page
        current = i
        top.select(i)
        old.onHidden()
        old.view.show(false, 160)
        page.view.visibility = View.VISIBLE
        page.view.alpha = 0f
        page.view.glide(alpha = 1f, duration = 220)
        page.onShown()
        if (i == 1) showPageWallpaper()
    }

    fun focusEntry() { page.entry()?.requestFocus() }

    private var sheetReturn: View? = null

    fun openSheet(s: Sheet) {
        val old = sheet
        if (old != null) root.removeView(old) else sheetReturn = root.findFocus()
        sheet = s
        root.addView(s, FrameLayout.LayoutParams(-1, -1))
    }

    fun closeSheet(s: Sheet) {
        if (sheet != s) return
        sheet = null
        s.glide(alpha = 0f, duration = 160) { root.removeView(s) }
        s.onClose?.invoke()
        if (sheet != null) return
        val back = sheetReturn?.takeIf { it.isAttachedToWindow && it.isShown && !isInside(it, s) }
        sheetReturn = null
        (back ?: setup?.entry() ?: page.entry())?.requestFocus()
    }

    private fun isInside(v: View, parent: View): Boolean {
        var p: Any? = v
        while (p is View) { if (p === parent) return true; p = p.parent }
        return false
    }

    fun startSetup() {
        if (setup != null) return
        sheet?.let { closeSheet(it) }
        val s = Setup(this)
        setup = s
        showPageWallpaper()
        top.visibility = View.INVISIBLE
        pages.forEach { it.view.visibility = View.INVISIBLE }
        root.addView(s, FrameLayout.LayoutParams(-1, -1))
        s.start()
    }

    fun endSetup() {
        val s = setup ?: return
        setup = null
        prefs.setupDone = true
        HomeGuardService.reload()
        root.removeView(s)
        top.visibility = View.VISIBLE
        top.alpha = 1f
        pages.forEach { it.view.visibility = View.GONE }
        current = 0
        page.view.visibility = View.VISIBLE
        page.view.alpha = 1f
        top.select(0)
        pages.forEach { it.rebuild() }
        page.onShown()
        home.goTop()
    }

    private var otherGuardWarned = false

    fun requestGuard(viaDebug: Boolean = false, onAccepted: () -> Unit = {}) {
        val disclosure = Sheet(this, getString(R.string.guard_name), getString(R.string.guard_description))
        disclosure.row(null, getString(R.string.cancel)) { disclosure.close() }
        disclosure.row("shield", getString(R.string.yes)) {
            disclosure.close()
            onAccepted()
            val returnToSetup = setup != null
            if (BuildConfig.SITE && viaDebug) {
                DeepClean(this).connect { adb ->
                    prefs.awaitingGuard = returnToSetup
                    if (!Cleaner.openAccessibilitySettings(adb)) {
                        prefs.awaitingGuard = false
                        Bg.main.post { toast(getString(R.string.home_guard_failed)) }
                    }
                }
            } else {
                prefs.awaitingGuard = returnToSetup
                if (!HomeButton.openAccessibility(this)) {
                    prefs.awaitingGuard = false
                    toast(getString(R.string.cannot_open))
                }
            }
        }
        disclosure.show()
    }

    private fun warnOtherGuard() {
        if (otherGuardWarned || setup != null || sheet != null || !HomeButton.guardEnabled(this)) return
        val other = HomeButton.otherGuard(this) ?: return
        otherGuardWarned = true
        val s = Sheet(this, getString(R.string.other_guard_title), getString(R.string.other_guard_text, other))
        s.row("shield", getString(R.string.open_accessibility)) {
            s.close()
            if (!HomeButton.openAccessibility(this)) toast(getString(R.string.cannot_open))
        }
        s.row(null, getString(R.string.later)) { s.close() }
        s.show()
    }

    fun open(pkg: String) {
        if (SeeClap.isSeeClap(pkg) && SeeClap.installedPackage(this) == null) {
            installSeeClapSheet()
            return
        }
        if (!Apps.launch(this, pkg)) toast(getString(R.string.cannot_open))
    }

    fun installSeeClapSheet() {
        val play = SeeClap.hasPlayStore(this) || SeeClap.isFireTv(this)
        val s = Sheet(this, getString(R.string.seeclap_install_title),
            getString(if (play) R.string.seeclap_install_store else R.string.seeclap_install_site), SeeClap.PKG)
        s.row("download", getString(R.string.install)) { Installer.installSeeClap(this); s.close() }
        if (play && BuildConfig.SITE) s.row("download", getString(R.string.install_from_site)) { Installer.installSeeClap(this, fromSite = true); s.close() }
        s.show()
    }

    fun appMenu(pkg: String) {
        val entry = Apps.find(pkg)
        val name = entry?.label ?: if (SeeClap.isSeeClap(pkg)) "SeeClap" else pkg
        val s = Sheet(this, name, null, pkg)
        s.row(null, getString(R.string.open)) { s.close(); open(pkg) }
        if (home.needsTvPermission(pkg)) s.row("eye", getString(R.string.show_app_programs)) {
            s.close()
            requestTvListings()
        }
        if (SeeClap.isSeeClap(pkg)) {
            s.row("lock", getString(R.string.always_on_home)) {}.dimmed = true
        } else {
            val favs = prefs.favorites
            if (pkg in favs) {
                val i = favs.indexOf(pkg)
                if (i > 0) s.row("up", getString(R.string.move_left)) { prefs.favorites = favs.toMutableList().apply { removeAt(i); add(i - 1, pkg) }; s.close(); rebuildAll() }
                if (i < favs.size - 1) s.row("down", getString(R.string.move_right)) { prefs.favorites = favs.toMutableList().apply { removeAt(i); add(i + 1, pkg) }; s.close(); rebuildAll() }
                s.row("minus", getString(R.string.remove_favorite)) { prefs.favorites = favs - pkg; s.close(); rebuildAll() }
            } else {
                s.row("favorites", getString(R.string.add_favorite)) {
                    if (favs.size >= prefs.favCount - 1) toast(getString(R.string.favorites_full, prefs.favCount))
                    else { prefs.favorites = favs + pkg; prefs.hidden = prefs.hidden - pkg; s.close(); rebuildAll() }
                }
            }
            val hidden = pkg in prefs.hidden
            s.row("eye", getString(if (hidden) R.string.show_app else R.string.hide_app)) {
                prefs.hidden = if (hidden) prefs.hidden - pkg else prefs.hidden + pkg
                if (!hidden) prefs.favorites = prefs.favorites - pkg
                s.close(); rebuildAll()
            }
            s.row("info", getString(R.string.app_info)) { s.close(); Apps.openDetails(this, pkg) }
            if (entry != null && !entry.system) s.row("trash", getString(R.string.uninstall)) { s.close(); Apps.uninstall(this, pkg) }
        }
        s.show()
    }

    fun rebuildAll() = pages.forEach { it.rebuild() }

    fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setup?.onResumeSetup()
        settings.refresh()
    }
}

fun TextView.lines(n: Int): TextView { maxLines = n; return this }
