package studio.seeapps.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.provider.Settings
import android.text.format.Formatter
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.net.Socket
import kotlin.math.max

class SettingsPage(private val act: HomeActivity) : FrameLayout(act), Page {
    override val view: View get() = this
    private val prefs = Prefs.get(act)

    private sealed class Item
    private class Section(val label: String) : Item()
    private class Line(
        val icon: String?, val label: String, val desc: String, val chevron: Boolean = false,
        val value: () -> String = { "" }, val enabled: () -> Boolean = { true }, val onClick: (Line) -> Unit,
    ) : Item() {
        var view: RowView? = null
    }

    private class Screen(val id: String, val title: String, val crumb: String?, val items: () -> List<Item>)

    private val stack = ArrayList<Screen>()
    private val header = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
    private val crumb = act.label(24, Ui.medium, Ui.ink(0.55f))
    private val title = act.label(64, Ui.bold, Ui.ink(), -0.03f)
    private val preview = PreviewView(act)
    private val descTitle = act.label(30, Ui.semibold, Ui.ink())
    private val descText = act.label(22, Ui.medium, Ui.ink(0.6f), 0f, 4).apply { setLineSpacing(Ui.pxf(6), 1f) }
    private val viewport = FrameLayout(act)
    private val column = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; clipChildren = false }
    private val rows = ArrayList<RowView>()
    private val lines = ArrayList<Line>()
    private var scroll = 0
    private val focusById = HashMap<String, Int>()

    init {
        clipChildren = false
        header.addView(crumb)
        header.addView(title, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.px(6) })
        place(header, 96, 112)
        place(preview, 96, 300, 800, 450)
        place(descTitle, 96, 786, 800, -2)
        place(descText, 96, 834, 800, -2)
        viewport.clipChildren = false
        viewport.addView(column, LayoutParams(-1, -2))
        place(viewport, 980, 300, 844, 760)
        push(rootScreen())
    }

    private fun s(id: Int) = act.getString(id)
    private fun yes(b: Boolean) = s(if (b) R.string.yes else R.string.no)

    private fun rootScreen() = Screen("root", s(R.string.tab_settings), null) {
        buildList {
            add(Line("appearance", s(R.string.appearance), s(R.string.appearance_desc), true, { themeLabel(prefs.theme) }) { push(appearanceScreen()) })
            add(Line("favorites", s(R.string.favorites), s(R.string.favorites_desc), true, { "${Apps.favorites(act).size} / ${prefs.favCount}" }) { push(favoritesScreen()) })
            if (BuildConfig.HOME_GUARD) add(Line("remote", s(R.string.remote), s(R.string.remote_desc), true, { keyValue() }) { push(remoteScreen()) })
            add(Line("home", s(R.string.home_button), s(R.string.home_button_desc), true, { homeValue() }) { push(homeScreen()) })
            add(Line("clean", s(R.string.clean), s(R.string.clean_desc), true, { act.getString(R.string.free_space, Formatter.formatShortFileSize(act, Cleaner.freeBytes())) }) { push(cleanScreen()) })
            add(Line("seeclap", "SeeClap", s(R.string.seeclap_desc), true, { seeclapValue() }) { push(seeclapScreen()) })
            add(Line("tv", s(R.string.tv_settings), s(R.string.tv_settings_desc), true) { HomeButton.open(act, Settings.ACTION_SETTINGS) })
            add(Line("restart", s(R.string.restart_setup), s(R.string.restart_setup_desc)) { act.startSetup() })
            add(Line("about", s(R.string.about), s(R.string.about_desc), true, { "${BuildConfig.VERSION_NAME}" }) { push(aboutScreen()) })
        }
    }

    private fun appearanceScreen() = Screen("appearance", s(R.string.appearance), s(R.string.tab_settings)) {
        buildList {
            add(Line("appearance", s(R.string.theme), s(R.string.theme_desc), true, { themeLabel(prefs.theme) }) { line ->
                choose(line, listOf("auto", "light", "dark"), prefs.theme, ::themeLabel) { theme ->
                    prefs.theme = theme
                    act.applyTheme()
                }
            })
            add(Section(s(R.string.section_home)))
            add(Line("size", s(R.string.fav_size), s(R.string.fav_size_desc), false, { s(if (prefs.favCount == 5) R.string.fav_size_5 else R.string.fav_size_6) }) {
                prefs.favCount = if (prefs.favCount == 5) 6 else 5
                prefs.favorites = prefs.favorites.take(prefs.favCount - 1)
                Banners.clear()
                act.rebuildAll()
                refresh()
            })
            add(Line("clock", s(R.string.show_clock), s(R.string.show_clock_desc), false, { yes(prefs.showClock) }) {
                prefs.showClock = !prefs.showClock
                act.top.applyClock()
                refresh()
            })
            add(Section(s(R.string.section_comfort)))
            add(Line("motion", s(R.string.reduce_motion), s(R.string.reduce_motion_desc), false, { yes(prefs.reduceMotion) }) {
                prefs.reduceMotion = !prefs.reduceMotion
                Ui.reduceMotion = prefs.reduceMotion
                refresh()
            })
        }
    }

    private fun favoritesScreen() = Screen("favorites", s(R.string.favorites), s(R.string.tab_settings)) {
        val favs = Apps.favorites(act)
        val items = ArrayList<Item>()
        favs.forEachIndexed { i, pkg ->
            val name = Apps.find(pkg)?.label ?: "SeeClap"
            if (i == 0) items += Line("lock", name, s(R.string.seeclap_locked_desc), false, { s(R.string.always_there) }) {
                act.toast(s(R.string.always_on_home))
            } else items += Line("favorites", name, s(R.string.favorite_row_desc), true) { act.appMenu(pkg) }
        }
        if (favs.size < prefs.favCount) items += Line("plus", s(R.string.add_app), s(R.string.add_app_desc), true) { pickFavorite() }
        items
    }

    private fun remoteScreen() = Screen("remote", s(R.string.remote), s(R.string.tab_settings)) {
        val items = arrayListOf<Item>(
            Line("remote", s(R.string.key_for_seeclap), s(R.string.key_for_seeclap_desc), true, { keyValue() }) { learnKey() },
        )
        if (prefs.keyCode != 0 || prefs.scanCode != 0) {
            val original = prefs.keyOriginal
            if (original.isNotEmpty() && SeeClap.isInstalled(act, original)) {
                items += Line("clock", act.getString(R.string.hold_opens, Cleaner.label(act, original)), s(R.string.hold_opens_desc), false, { yes(prefs.holdOriginal) }) {
                    prefs.holdOriginal = !prefs.holdOriginal
                    HomeGuardService.reload()
                    refresh()
                }
            }
            items += Line("trash", s(R.string.remove_key), s(R.string.remove_key_desc)) {
                prefs.clearKey()
                HomeGuardService.reload()
                refresh()
            }
        }
        items
    }

    private fun homeScreen() = Screen("home", s(R.string.home_button), s(R.string.tab_settings)) {
        val items = arrayListOf<Item>(
            Line("home", s(R.string.home_status), s(R.string.home_status_desc), true, { homeValue() }) { activateHome() },
        )
        if (BuildConfig.HOME_GUARD) {
            items += Line("motion", s(R.string.guard_pause), s(R.string.guard_pause_desc), false, { yes(!prefs.guardOn) }) {
                prefs.guardOn = !prefs.guardOn
                HomeGuardService.reload()
                refresh()
            }
        }
        if (!HomeButton.isDefault(act)) items += Line("tv", s(R.string.open_stock_home), s(R.string.open_stock_home_desc)) {
            if (!HomeButton.openStockHome(act)) act.toast(s(R.string.cannot_open))
        }
        items
    }

    private fun cleanScreen() = Screen("clean", s(R.string.clean), s(R.string.tab_settings)) {
        val disabled = prefs.disabledByUs
        val items = arrayListOf<Item>(
            Line("clean", s(R.string.quick_clean), s(R.string.quick_clean_desc), true) { quickClean() },
            Line("bolt", s(R.string.deep_clean), s(R.string.deep_clean_desc), true) { DeepClean(act).start() },
        )
        if (disabled.isNotEmpty()) items += Line("restart", s(R.string.disabled_apps), s(R.string.disabled_apps_desc), true, { disabled.size.toString() }) { restoreApps() }
        items += Line("trash", s(R.string.clear_home_cache), s(R.string.clear_home_cache_desc)) {
            Cleaner.clearOwnCache(act)
            act.rebuildAll()
            act.toast(s(R.string.done))
        }
        items
    }

    private fun seeclapScreen() = Screen("seeclap", "SeeClap", s(R.string.tab_settings)) {
        val installed = SeeClap.installedPackage(act)
        val items = ArrayList<Item>()
        if (installed == null) {
            items += Line("download", s(R.string.install_seeclap), s(R.string.install_seeclap_desc), true, { installValue() }) { act.installSeeClapSheet() }
        } else {
            items += Line("seeclap", s(R.string.open_seeclap), s(R.string.open_seeclap_desc), false, { SeeClap.versionName(act, installed) }) { Apps.launch(act, installed) }
            val fromPlay = SeeClap.installerOf(act, installed) == SeeClap.PLAY_STORE
            when {
                fromPlay -> items += Line("update", s(R.string.updates), s(R.string.updates_play_desc), true, { s(R.string.by_google_play) }) { SeeClap.openStore(act) }
                BuildConfig.SITE && installed == SeeClap.PKG -> items += Line("update", s(R.string.updates), s(R.string.updates_site_desc), false, { installValue() }) {
                    Installer.updateSeeClap(act, auto = false)
                }
                else -> items += Line("update", s(R.string.updates), s(R.string.updates_store_desc), true) { SeeClap.openStore(act) }
            }
        }
        items
    }

    private fun aboutScreen() = Screen("about", s(R.string.about), s(R.string.tab_settings)) {
        val items = arrayListOf<Item>(
            Line("info", s(R.string.version), s(R.string.version_desc), false, { BuildConfig.VERSION_NAME + if (BuildConfig.SITE) " · site" else "" }) {},
        )
        if (BuildConfig.SITE) items += Line("update", s(R.string.home_update), s(R.string.home_update_desc), false, { selfUpdateValue() }) {
            Installer.updateSelf(act, install = true)
        }
        items += Line("shield", s(R.string.licence), s(R.string.licence_desc), true, { s(R.string.licence_value) }) {
            Sheet(act, s(R.string.licence), s(R.string.licence_text)).apply { row(null, s(R.string.ok)) { close() } }.show()
        }
        items += Line("favorites", s(R.string.credits), s(R.string.credits_desc), true) {
            Sheet(act, s(R.string.credits), s(R.string.credits_text)).apply { row(null, s(R.string.ok)) { close() } }.show()
        }
        items
    }

    private fun themeLabel(mode: String) = s(Appearance.themeLabel(mode))

    private fun keyValue(): String =
        if (prefs.keyCode == 0 && prefs.scanCode == 0) s(R.string.no_key) else RemoteKeys.name(act, prefs.keyCode)

    private fun homeValue(): String = when {
        HomeButton.isDefault(act) -> s(R.string.home_default)
        HomeButton.guardEnabled(act) && !prefs.guardOn -> s(R.string.home_paused)
        HomeButton.guardEnabled(act) -> s(R.string.home_guarded)
        else -> s(R.string.home_off)
    }


    private fun seeclapValue(): String {
        val pkg = SeeClap.installedPackage(act) ?: return installValue()
        val st = Installer.state(SeeClap.PKG)
        if (st is Installer.State.Ready) return s(R.string.update_ready)
        return SeeClap.versionName(act, pkg)
    }

    private fun stateText(st: Installer.State?): String? = when (st) {
        is Installer.State.Checking -> s(R.string.checking)
        is Installer.State.Downloading -> act.getString(R.string.downloading, st.percent)
        is Installer.State.Verifying -> s(R.string.verifying)
        is Installer.State.NeedsPermission -> s(R.string.needs_permission)
        is Installer.State.Confirming -> s(R.string.confirm_on_tv)
        is Installer.State.Done -> s(R.string.done)
        is Installer.State.UpToDate -> s(R.string.up_to_date)
        is Installer.State.Ready -> s(R.string.update_ready)
        is Installer.State.Failed -> s(st.reason)
        is Installer.State.Idle, null -> null
    }

    private fun installValue() = stateText(Installer.state(SeeClap.PKG)) ?: s(R.string.not_installed)

    private fun selfUpdateValue() = stateText(Installer.state(act.packageName)) ?: s(R.string.check_now)

    private fun choose(line: Line, choices: List<String>, selected: String, label: (String) -> String, pick: (String) -> Unit) {
        val sheet = Sheet(act, line.label)
        for (choice in choices) {
            sheet.row(null, label(choice), if (choice == selected) "✓" else "") {
                sheet.close()

                lines.firstOrNull { it.label == line.label }?.view?.apply {
                    requestFocus()
                    valueView.text = label(choice)
                }
                pick(choice)
            }
        }
        sheet.show()
        sheet.rows.getOrNull(choices.indexOf(selected))?.requestFocus()
    }

    private fun syncWallpaper() {
        if (act.current != 2 || !isShown) return
        val key = Layers.ambientKey
        fun update(layer: Layer) {
            if (act.current != 2 || !isShown || layer.key != Layers.ambientKey) return
            if (preview.layer !== layer) preview.layer = layer
            if (!act.backdrop.showsProgram) act.backdrop.show(layer)
        }
        val current = preview.layer
        if (current != null && current.key == key) update(current) else Layers.ambient(act, ::update)
    }

    private fun activateHome() {
        when {
            HomeButton.isDefault(act) -> act.toast(s(R.string.home_default))
            HomeButton.canAskRole(act) -> if (!HomeButton.askRole(act, 41)) HomeButton.openHomeSettings(act)
            !BuildConfig.HOME_GUARD -> if (!HomeButton.openHomeSettings(act)) act.toast(s(R.string.setup_home_open_from_apps))
            else -> act.requestGuard()
        }
    }

    private fun pickFavorite() {
        val favs = Apps.favorites(act)
        val sheet = Sheet(act, s(R.string.add_app))
        if (Apps.all.none { it.pkg !in favs && !SeeClap.isSeeClap(it.pkg) }) sheet.row(null, s(R.string.ok)) { sheet.close() }
        Apps.all.filter { it.pkg !in favs && !SeeClap.isSeeClap(it.pkg) }.forEach { app ->
            sheet.row(null, app.label) {
                prefs.favorites = prefs.favorites + app.pkg
                prefs.hidden = prefs.hidden - app.pkg
                sheet.close()
                act.rebuildAll()
                refresh()
            }
        }
        sheet.show()
    }

    private fun learnKey() {
        if (HomeGuardService.running == null) {
            Sheet(act, s(R.string.key_needs_guard_title), s(R.string.key_needs_guard)).apply {
                row("home", s(R.string.home_button), chevron = true) { close(); push(homeScreen()) }
            }.show()
            return
        }
        val sheet = Sheet(act, s(R.string.press_a_key_title), s(R.string.press_a_key))
        sheet.row(null, s(R.string.cancel)) { sheet.close() }
        sheet.onClose = { HomeGuardService.learner = null }
        HomeGuardService.learner = { code, scan ->
            prefs.keyCode = code
            prefs.scanCode = scan
            prefs.keyOriginal = RemoteKeys.original(code) ?: ""
            HomeGuardService.reload()
            sheet.close()
            act.toast(act.getString(R.string.key_saved, RemoteKeys.name(act, code)))
            refresh()
        }
        sheet.show()
    }

    private fun quickClean() {
        val sheet = Sheet(act, s(R.string.quick_clean), s(R.string.quick_clean_sheet))
        val list = Cleaner.userApps(act)
        if (list.isEmpty()) sheet.row(null, s(R.string.nothing_to_clean)) { sheet.close() }
        list.forEach { item ->
            sheet.row("trash", item.app.label, Formatter.formatShortFileSize(act, item.bytes)) { sheet.close(); Apps.uninstall(act, item.app.pkg) }
        }
        sheet.row("trash", s(R.string.clear_home_cache)) { Cleaner.clearOwnCache(act); sheet.close(); act.rebuildAll(); act.toast(s(R.string.done)) }
        sheet.show()
    }

    private fun restoreApps() {
        val sheet = Sheet(act, s(R.string.disabled_apps), s(R.string.restore_sheet))
        val all = prefs.disabledByUs.toList()
        sheet.row("restart", s(R.string.restore_all)) { sheet.close(); DeepClean(act).connect { adb -> val n = Cleaner.restore(act, adb, all); Bg.main.post { act.toast(act.getString(R.string.restored, n)); refresh() } } }
        all.forEach { pkg ->
            sheet.row(null, Cleaner.label(act, pkg)) { sheet.close(); DeepClean(act).connect { adb -> val n = Cleaner.restore(act, adb, listOf(pkg)); Bg.main.post { act.toast(act.getString(R.string.restored, n)); refresh() } } }
        }
        sheet.show()
    }

    private fun push(screen: Screen) {
        stack.lastOrNull()?.let { focusById[it.id] = rows.indexOfFirst { r -> r.hasFocus() }.coerceAtLeast(0) }
        stack += screen
        render(focusIndex = 0, focus = stack.size > 1)
    }

    private fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.size - 1)
        render(focusIndex = focusById[stack.last().id] ?: 0, focus = true)
        return true
    }

    fun refresh() {
        if (stack.isEmpty()) return
        val idx = rows.indexOfFirst { it.hasFocus() }
        render(focusIndex = max(0, idx), focus = idx >= 0)
    }

    fun applyColors() {
        crumb.setTextColor(Ui.ink(0.55f))
        title.setTextColor(Ui.ink())
        descTitle.setTextColor(Ui.ink())
        descText.setTextColor(Ui.ink(0.6f))

        for (i in 0 until column.childCount) {
            (column.getChildAt(i) as? TextView)?.setTextColor(Ui.ink(0.5f))
        }
        preview.invalidate()
        syncWallpaper()
    }

    private fun render(focusIndex: Int, focus: Boolean) {
        val screen = stack.last()
        crumb.text = screen.crumb ?: ""
        crumb.visibility = if (screen.crumb == null) View.INVISIBLE else View.VISIBLE
        title.text = screen.title
        if (rows.any { it.hasFocus() }) act.holdFocus()
        column.removeAllViews()
        rows.clear()
        lines.clear()
        for (item in screen.items()) when (item) {
            is Section -> column.addView(act.label(22, Ui.semibold, Ui.ink(0.5f), 0.02f).apply { text = item.label },
                LinearLayout.LayoutParams(-2, -2).apply {
                    topMargin = Ui.px(if (column.childCount == 0) 0 else 22)
                    bottomMargin = Ui.px(8)
                    leftMargin = Ui.px(24)
                })
            is Line -> {
                val r = RowView(act, item.icon, item.label, item.chevron)
                r.valueView.text = item.value()
                r.dimmed = !item.enabled()
                r.setOnClickListener { item.onClick(item) }
                r.setOnFocusChangeListener { v, has -> if (has) onRowFocus(item, v) }
                item.view = r
                column.addView(r, LinearLayout.LayoutParams(-1, Ui.px(82)).apply { topMargin = Ui.px(if (rows.isEmpty()) 0 else 8) })
                rows += r
                lines += item
            }
        }
        column.animate().cancel()
        scroll = 0
        column.translationY = 0f
        preview.favs = Apps.favorites(act)
        syncWallpaper()
        if (focus) rows.getOrNull(focusIndex.coerceIn(0, max(0, rows.size - 1)))?.requestFocus()
        else lines.firstOrNull()?.let { describe(it) }
    }

    private fun onRowFocus(line: Line, v: View) {
        describe(line)
        if (v.isLaidOut) reveal(v)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        rows.firstOrNull { it.hasFocus() }?.let { reveal(it) }
    }

    private fun reveal(v: View) {
        val visible = Ui.px(740)
        val top = v.top
        val bottom = v.bottom
        val oldScroll = scroll
        if (bottom - scroll > visible) scroll = bottom - visible + Ui.px(10)
        if (top < scroll) scroll = max(0, top - Ui.px(10))
        if (oldScroll != scroll) column.animate().translationY(-scroll.toFloat()).setInterpolator(Ui.ease).setDuration(Ui.ms(220)).start()
    }

    private fun describe(line: Line) {
        descTitle.text = line.label
        descText.text = line.desc
    }

    fun onInstallState(pkg: String, state: Installer.State) {
        if (stack.lastOrNull()?.id in setOf("root", "seeclap", "about")) {
            lines.forEachIndexed { i, l -> rows.getOrNull(i)?.valueView?.text = l.value() }
        }
        if (state is Installer.State.Ready && pkg == SeeClap.PKG && stack.lastOrNull()?.id == "seeclap") {

        }
    }

    override fun entry(): View? = rows.firstOrNull()

    override fun route(focused: View, direction: Int): View? {
        val i = rows.indexOf(focused)
        if (i < 0) return null
        return when (direction) {
            View.FOCUS_UP -> rows.getOrNull(i - 1) ?: act.top.tab(2)
            View.FOCUS_DOWN -> rows.getOrNull(i + 1) ?: focused
            View.FOCUS_LEFT -> if (stack.size > 1) { pop(); rows.getOrNull(focusById[stack.last().id] ?: 0) ?: focused } else focused
            View.FOCUS_RIGHT -> focused
            else -> null
        }
    }

    override fun back(): Boolean = pop()

    override fun rebuild() = refresh()

    override fun onShown() {
        act.back.animateTo(blur = 1f, dim = 0.62f, bottom = 0f, left = 0f)
        act.top.fade(true)
        refresh()
    }
}

class PreviewView(context: Context) : View(context) {
    var layer: Layer? = null
        set(v) { field = v; blurShader = v?.let { BitmapShader(it.blur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }; invalidate() }
    var favs: List<String> = emptyList()
        set(v) {
            field = v
            banners.clear()
            v.forEach { pkg -> Banners.get(context, pkg, Ui.px(150), Ui.px(85)) { b -> banners[pkg] = b; invalidate() } }
            invalidate()
        }
    private val banners = HashMap<String, Bitmap>()
    private var blurShader: BitmapShader? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val m = Matrix()
    private val path = Path()

    init {
        outlineProvider = roundOutline { Ui.pxf(24) }
        clipToOutline = true
        if (!Ui.light) elevation = Ui.pxf(14)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val l = layer
        canvas.drawColor(Ui.surface)
        p.color = Color.WHITE
        p.shader = null
        if (l != null) {
            val sc = max(w / l.sharp.width, h / l.sharp.height)
            val dw = l.sharp.width * sc
            val dh = l.sharp.height * sc
            canvas.drawBitmap(l.sharp, null, RectF((w - dw) / 2, (h - dh) * 0.3f, (w + dw) / 2, (h - dh) * 0.3f + dh), p)
        }

        val k = w / 1920f
        val g = Grid(Prefs.get(context).favCount)
        val tw = g.tw.toFloat()
        val th = g.th.toFloat()
        val band = RectF(g.bandLeft * k, g.bandTop * k, (g.bandLeft + g.bw) * k, (g.bandTop + g.bh) * k)
        Squircle.into(path, band.left, band.top, band.right, band.bottom, g.bandRadius * k)
        blurShader?.let { sh ->
            val bmp = l!!.blur
            val sc = max(w / bmp.width, h / bmp.height)
            m.setScale(sc, sc)
            m.postTranslate((w - bmp.width * sc) / 2, (h - bmp.height * sc) * 0.3f)
            sh.setLocalMatrix(m)
            p.shader = sh
            canvas.drawPath(path, p)
        }
        p.shader = GlassView.tint(band.top, band.bottom)
        p.alpha = GlassView.bandTintAlpha(1f)
        canvas.drawPath(path, p)
        p.alpha = 255
        p.shader = null
        favs.forEachIndexed { i, pkg ->
            val x = band.left + g.favX(i, favs.size) * k
            val y = band.top + g.pad * k
            val rect = RectF(x, y, x + tw * k, y + th * k)
            val b = banners[pkg]
            if (b != null) {
                val bs = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                m.setScale(rect.width() / b.width, rect.height() / b.height)
                m.postTranslate(rect.left, rect.top)
                bs.setLocalMatrix(m)
                p.color = Color.WHITE
                p.shader = bs
            } else p.color = Ui.ink(0.12f)
            canvas.drawPath(Squircle.into(path, rect.left, rect.top, rect.right, rect.bottom, rect.height() * Squircle.ROUND), p)
            p.shader = null
        }
    }
}

class DeepClean(private val act: HomeActivity) {
    private fun s(id: Int) = act.getString(id)

    fun start() = connect(keepOpen = true) { adb -> Bg.main.post { plan(adb) } }

    fun connect(keepOpen: Boolean = false, then: (LocalAdb) -> Unit) {
        val wait = Sheet(act, s(R.string.deep_clean), s(R.string.deep_connecting))
        val socket = Socket()
        var cancelled = false
        fun waiting() = !cancelled && wait.isAttachedToWindow && !act.isFinishing && !act.isDestroyed
        wait.onClose = {
            cancelled = true
            socket.close()
        }
        wait.row(null, s(R.string.cancel)) { wait.close() }
        wait.show()
        Bg.run {
            val result = runCatching {
                LocalAdb.connect(act, socket) {
                    Bg.main.post { if (waiting()) wait.message(s(R.string.deep_allow_on_tv)) }
                }
            }
            Bg.main.post {
                if (!waiting()) {
                    socket.close()
                    return@post
                }
                wait.onClose = null
                wait.close()
                val adb = result.getOrNull()
                if (adb == null) {
                    socket.close()
                    help(result.exceptionOrNull() is LocalAdb.NeedsPairing) { connect(keepOpen, then) }
                } else Bg.run {
                    try {
                        then(adb)
                    } catch (e: Exception) {
                        adb.close()
                        Bg.main.post { if (!act.isDestroyed && !act.isFinishing) act.toast(s(R.string.deep_failed)) }
                    } finally {
                        if (!keepOpen) adb.close()
                    }
                }
            }
        }
    }

    private fun help(pairing: Boolean, retry: () -> Unit) {
        val adbOn = Settings.Global.getInt(act.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        val needsComputer = pairing || adbOn
        val sheet = Sheet(act, s(if (needsComputer) R.string.deep_needs_computer_title else R.string.deep_off_title),
            s(if (needsComputer) R.string.deep_pairing else R.string.deep_off))
        sheet.row("restart", s(R.string.retry)) { sheet.close(); retry() }
        if (!needsComputer) sheet.row("tv", s(R.string.open_dev_options), chevron = true) {
            sheet.close()
            if (!HomeButton.open(act, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) HomeButton.open(act, Settings.ACTION_DEVICE_INFO_SETTINGS)
        }
        sheet.row(null, s(R.string.ok)) { sheet.close() }
        sheet.show()
    }

    private fun plan(adb: LocalAdb) {
        var caches = true
        var telemetry = true
        var faster = true
        var guard = false
        val chosen = LinkedHashSet<String>()
        fun show() {
            val sheet = Sheet(act, s(R.string.deep_clean), s(R.string.deep_plan))
            sheet.row("trash", s(R.string.plan_caches), s(if (caches) R.string.yes else R.string.no)) { caches = !caches; it.valueView.text = s(if (caches) R.string.yes else R.string.no) }
            sheet.row("shield", s(R.string.plan_telemetry), s(if (telemetry) R.string.yes else R.string.no)) { telemetry = !telemetry; it.valueView.text = s(if (telemetry) R.string.yes else R.string.no) }
            sheet.row("motion", s(R.string.plan_faster), s(if (faster) R.string.yes else R.string.no)) { faster = !faster; it.valueView.text = s(if (faster) R.string.yes else R.string.no) }
            if (BuildConfig.HOME_GUARD && !HomeButton.isDefault(act) && !HomeButton.guardEnabled(act)) {
                sheet.row("home", s(R.string.plan_guard), s(if (guard) R.string.yes else R.string.no)) { guard = !guard; it.valueView.text = s(if (guard) R.string.yes else R.string.no) }
            }
            sheet.row("tv", s(R.string.plan_preinstalled), chosen.size.toString(), chevron = true) {
                sheet.onClose = null
                sheet.close()
                pick(chosen) { show() }
            }
            sheet.row("bolt", s(R.string.plan_run)) {
                sheet.onClose = null
                sheet.close()
                val running = Sheet(act, s(R.string.deep_clean), s(R.string.deep_running))
                running.row(null, s(R.string.please_wait)) {}
                running.show()
                Bg.run {
                    val result = runCatching { Cleaner.deepClean(act, adb, Cleaner.Plan(caches, telemetry, faster, chosen.toList())) }.getOrNull()
                    adb.close()
                    Bg.main.post {
                        running.close()
                        val complete = result != null && result.errors == 0
                        val text = buildList {
                            if (!complete) add(s(R.string.deep_failed))
                            if (result != null) add(act.getString(R.string.deep_result, Formatter.formatShortFileSize(act, result.freedBytes), result.disabled))
                        }.joinToString("\n")
                        Sheet(act, s(if (complete) R.string.deep_done_title else R.string.deep_clean), text).apply {
                            row(null, s(R.string.ok)) {
                                close()
                                if (guard && !HomeButton.ready(act)) act.requestGuard()
                            }
                        }.show()
                        act.reloadApps()
                        act.settings.refresh()
                    }
                }
            }
            sheet.onClose = { adb.close() }
            sheet.show()
        }
        show()
    }

    private fun pick(chosen: MutableSet<String>, back: () -> Unit) {
        val sheet = Sheet(act, s(R.string.plan_preinstalled), s(R.string.pick_preinstalled))
        val list = Cleaner.preinstalled(act)
        if (list.isEmpty()) sheet.row(null, s(R.string.nothing_to_clean)) { sheet.close() }
        list.forEach { app ->
            sheet.row(null, app.label, s(if (app.pkg in chosen) R.string.yes else R.string.no)) {
                if (!chosen.remove(app.pkg)) chosen += app.pkg
                it.valueView.text = s(if (app.pkg in chosen) R.string.yes else R.string.no)
            }
        }
        sheet.onClose = back
        sheet.show()
    }
}
