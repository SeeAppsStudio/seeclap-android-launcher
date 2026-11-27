package studio.seeapps.home

import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import kotlin.math.max
import kotlin.math.min

class Setup(private val act: HomeActivity) : FrameLayout(act), ThemeAware {
    override fun applyColors() {
        (icon.drawable as? IconDrawable)?.color = Ui.ink()
        note.setTextColor(noteColor)
    }

    private enum class Step { WELCOME, THEME, SEECLAP, FAVORITES, HOME, REMOTE, CLEAN, DONE }

    private val flow = Step.values().filterNot { !BuildConfig.HOME_GUARD && it == Step.REMOTE }
    private val prefs = Prefs.get(act)
    private var step = Step.WELCOME
    private val counter = act.themeLabel(22, Ui.medium, 0.55f).apply { gravity = Gravity.CENTER }
    private val icon = ImageView(act)
    private val noteColor get() = if (Ui.isDay) 0xFF6A4000.toInt() else 0xFFFFD27A.toInt()
    private val title = act.themeLabel(60, Ui.bold, spacing = -0.025f, lines = 2).apply { gravity = Gravity.CENTER }
    private val subtitle = act.themeLabel(28, Ui.medium, 0.72f, lines = 5).apply {
        gravity = Gravity.CENTER
        setLineSpacing(Ui.pxf(8), 1f)
    }
    private val status = act.themeLabel(40, Ui.semibold, spacing = -0.01f, lines = 2).apply { gravity = Gravity.CENTER }

    private val steps = act.themeLabel(30, Ui.medium, 0.92f, lines = 4).apply { setLineSpacing(Ui.pxf(16), 1f) }
    private val note = act.label(26, Ui.semibold, noteColor, 0f, 2).apply { gravity = Gravity.CENTER }
    private var openedAccessibility = false
    private val content = FrameLayout(act).apply { clipChildren = true }
    private val buttons = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; clipChildren = false }
    private val buttonViews = ArrayList<Pill>()
    private var themePicker: ThemePicker? = null

    private val grid = FrameLayout(act).apply { clipChildren = false }
    private val gridTiles = ArrayList<TileView>()
    private val chosen = ArrayList<String>()
    private val cols get() = Grid(prefs.favCount).n
    private var gridScroll = 0

    init {
        isClickable = true
        clipChildren = false
        place(counter, 0, 96, 1920, -2)
        place(icon, 928, 168, 64, 64)
        place(title, 160, 270, 1600, -2)
        place(subtitle, 310, 380, 1300, -2)
        place(status, 160, 600, 1600, -2)
        addView(steps, LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL).apply { topMargin = Ui.px(560) })
        place(note, 160, 800, 1600, -2)
        place(content, 0, 540, 1920, 330)
        content.addView(grid, LayoutParams(-1, -2))
        place(buttons, 0, 910, 1920, 90)
    }

    fun start() {
        act.back.animateTo(blur = 1f, dim = 0.45f, bottom = 0f, left = 0f)
        chosen.clear()
        chosen += prefs.favorites.ifEmpty { Apps.favorites(act).drop(1) }
        show(Step.WELCOME)
    }

    private fun s(id: Int) = act.getString(id)

    private fun show(next: Step) {
        if (BuildConfig.HOME_GUARD && step == Step.REMOTE && next != Step.REMOTE) HomeGuardService.learner = null
        step = next
        val total = flow.size - 2
        val index = flow.indexOf(next)
        counter.text = if (next == Step.WELCOME || next == Step.DONE) "" else act.getString(R.string.step_of, index, total)
        status.text = ""
        steps.text = ""
        note.text = ""
        grid.visibility = View.GONE
        themePicker?.visibility = if (next == Step.THEME) View.VISIBLE else View.GONE
        when (next) {
            Step.WELCOME -> page("home", R.string.setup_welcome_title, s(R.string.setup_welcome_text),
                listOf(s(R.string.start) to { show(Step.THEME) }))
            Step.THEME -> themeStep()
            Step.SEECLAP -> seeclapStep()
            Step.FAVORITES -> favoritesStep()
            Step.HOME -> homeStep()
            Step.REMOTE -> if (BuildConfig.HOME_GUARD) remoteStep()
            Step.CLEAN -> page("clean", R.string.setup_clean_title, s(R.string.setup_clean_text),
                listOf(s(R.string.clean_now) to { DeepClean(act).start() }, s(R.string.next) to { show(Step.DONE) }))
            Step.DONE -> page("check", R.string.setup_done_title,
                s(if (!BuildConfig.HOME_GUARD && !HomeButton.isDefault(act)) R.string.setup_home_open_from_apps else R.string.setup_done_text),
                listOf(s(R.string.go_home) to { act.endSetup() }))
        }
        alpha = 0.6f
        glide(alpha = 1f, duration = 220)
    }

    private fun page(iconKind: String, titleRes: Int, text: String, actions: List<Pair<String, () -> Unit>>) {
        setIcon(iconKind)
        title.text = s(titleRes)
        subtitle.text = text
        setButtons(actions)
    }

    private fun setIcon(kind: String) {
        icon.setImageDrawable(IconDrawable(kind).apply { color = Ui.ink() })
    }

    private fun setButtons(actions: List<Pair<String, () -> Unit>>) {
        buttons.removeAllViews()
        buttonViews.clear()
        actions.forEachIndexed { i, (label, action) ->
            val b = Pill(act, tab = false).apply {
                text = label
                setOnClickListener { action() }
            }
            buttons.addView(b, LinearLayout.LayoutParams(-2, Ui.px(72)).apply { if (i > 0) leftMargin = Ui.px(24) })
            buttonViews += b
        }
        buttonViews.firstOrNull()?.requestFocus()
    }

    fun entry(): View? = if (step == Step.THEME) themePicker?.entry()
        else buttonViews.firstOrNull { it.isShown } ?: gridTiles.firstOrNull { it.isShown }

    private fun themeStep() {
        page("appearance", R.string.setup_theme_title, s(R.string.setup_theme_text), emptyList())
        val picker = themePicker ?: ThemePicker(act) { mode ->
            prefs.theme = mode
            act.applyTheme()
            show(Step.SEECLAP)
        }.also {
            themePicker = it
            content.addView(it, LayoutParams(-1, -1))
        }
        picker.focusChoice(prefs.theme)
    }

    private fun seeclapStep() {
        setIcon("seeclap")
        if (SeeClap.installedPackage(act) != null) {
            title.text = s(R.string.setup_seeclap_ok_title)
            subtitle.text = s(R.string.setup_seeclap_ok_text)
            setButtons(listOf(s(R.string.next) to { show(Step.FAVORITES) }))
            return
        }
        val store = SeeClap.hasPlayStore(act) || SeeClap.isFireTv(act)
        title.text = s(R.string.setup_seeclap_title)
        subtitle.text = s(if (store) R.string.setup_seeclap_store else R.string.setup_seeclap_site)
        val actions = arrayListOf<Pair<String, () -> Unit>>(s(R.string.install) to { Installer.installSeeClap(act) })
        if (store && BuildConfig.SITE) actions += s(R.string.install_from_site) to { Installer.installSeeClap(act, fromSite = true) }
        actions += s(R.string.later) to { show(Step.FAVORITES) }
        setButtons(actions)
        Installer.state(SeeClap.PKG)?.let { onInstallState(SeeClap.PKG, it) }
    }

    fun onInstallState(pkg: String, state: Installer.State) {
        if (step != Step.SEECLAP || !SeeClap.isSeeClap(pkg)) return
        status.text = when (state) {
            is Installer.State.Checking -> s(R.string.checking)
            is Installer.State.Downloading -> act.getString(R.string.downloading, state.percent)
            is Installer.State.Verifying -> s(R.string.verifying)
            is Installer.State.NeedsPermission -> s(R.string.needs_permission_long)
            is Installer.State.Confirming -> s(R.string.confirm_on_tv)
            is Installer.State.Done -> { postDelayed({ if (step == Step.SEECLAP) seeclapStep() }, 600); s(R.string.done) }
            is Installer.State.Failed -> s(state.reason)
            else -> ""
        }
    }

    fun onAppsChanged() {
        if (step == Step.SEECLAP && SeeClap.installedPackage(act) != null) seeclapStep()
        if (step == Step.FAVORITES) favoritesStep()
    }

    private fun favoritesStep() {
        setIcon("favorites")
        title.text = s(R.string.setup_favorites_title)
        subtitle.text = act.getString(R.string.setup_favorites_text, prefs.favCount - 1)
        grid.removeAllViews()
        gridTiles.clear()
        gridScroll = 0
        grid.translationY = 0f
        val list = listOf(SeeClap.tilePackage(act)) + Apps.all.filter { !SeeClap.isSeeClap(it.pkg) }.map { it.pkg }
        val g = Grid(prefs.favCount)
        val w = g.tw
        val h = g.th
        val gap = g.gap
        val left = g.x0
        list.forEachIndexed { i, pkg ->
            val t = TileView(act, w, h).apply {
                this.pkg = pkg
                Banners.get(act, pkg, Ui.px(w * 1.12f), Ui.px(h * 1.12f)) { if (this.pkg == pkg) bitmap = it }
                setOnClickListener { toggle(pkg) }
                setOnFocusChangeListener { _, has -> if (has) reveal(i / cols) }
            }
            grid.place(t, left + (i % cols) * (w + gap), 14 + (i / cols) * (h + 34), w, h)
            gridTiles += t
        }
        grid.layoutParams = (grid.layoutParams as LayoutParams).apply { height = Ui.px(14 + ((list.size + cols - 1) / cols) * (h + 34) + 40) }
        grid.visibility = View.VISIBLE
        badges()
        setButtons(listOf(s(R.string.next) to {
            prefs.favorites = chosen.toList()
            act.rebuildAll()
            show(Step.HOME)
        }))
        gridTiles.getOrNull(1)?.requestFocus() ?: buttonViews.firstOrNull()?.requestFocus()
    }

    private fun toggle(pkg: String) {
        if (SeeClap.isSeeClap(pkg)) { act.toast(s(R.string.always_on_home)); return }
        if (!chosen.remove(pkg)) {
            if (chosen.size >= prefs.favCount - 1) { act.toast(act.getString(R.string.favorites_full, prefs.favCount)); return }
            chosen += pkg
        }
        badges()
    }

    private fun badges() {
        gridTiles.forEach { t ->
            t.badge = when {
                SeeClap.isSeeClap(t.pkg) -> "1"
                t.pkg in chosen -> (chosen.indexOf(t.pkg) + 2).toString()
                else -> null
            }
        }
    }

    private fun reveal(row: Int) {
        val rowH = Grid(prefs.favCount).th + 34
        val visibleRows = 1
        val target = max(0, row - visibleRows) * rowH
        if (target != gridScroll) {
            gridScroll = target
            grid.animate().translationY(-Ui.pxf(target)).setInterpolator(Ui.ease).setDuration(Ui.ms(260)).start()
        }
    }

    private fun homeStep() {
        setIcon("home")
        title.text = s(R.string.setup_home_title)
        when {
            HomeButton.isDefault(act) || HomeButton.guardEnabled(act) -> {
                subtitle.text = s(R.string.setup_home_ready)
                status.text = s(R.string.setup_home_try)
                setButtons(listOf(s(R.string.next) to { show(if (BuildConfig.HOME_GUARD) Step.REMOTE else Step.CLEAN) }))
            }
            HomeButton.canAskRole(act) -> {
                subtitle.text = s(R.string.setup_home_role)
                setButtons(listOf(
                    s(R.string.choose_home) to { if (!HomeButton.askRole(act, 42)) HomeButton.openHomeSettings(act) },
                    s(R.string.later) to { show(if (BuildConfig.HOME_GUARD) Step.REMOTE else Step.CLEAN) },
                ))
            }
            !BuildConfig.HOME_GUARD -> {
                subtitle.text = s(R.string.setup_home_open_from_apps)
                setButtons(listOf(
                    s(R.string.choose_home) to {
                        if (!HomeButton.openHomeSettings(act)) act.toast(s(R.string.cannot_open))
                    },
                    s(R.string.next) to { show(Step.CLEAN) },
                ))
            }
            else -> {
                subtitle.text = s(R.string.setup_home_guard)
                steps.text = s(R.string.setup_home_steps)
                val other = HomeButton.otherGuard(act)
                note.text = when {
                    other != null -> act.getString(R.string.setup_home_other_guard, other)
                    openedAccessibility -> s(R.string.setup_home_not_yet)
                    else -> ""
                }
                setButtons(listOf(
                    s(R.string.open_accessibility) to {
                        act.requestGuard { openedAccessibility = true }
                    },
                    s(R.string.later) to { show(Step.REMOTE) },
                ))
            }
        }
    }

    private fun remoteStep() {
        setIcon("remote")
        title.text = s(R.string.setup_remote_title)
        if (HomeGuardService.running == null) {
            subtitle.text = s(R.string.setup_remote_needs_guard)
            setButtons(listOf(s(R.string.next) to { show(Step.CLEAN) }))
            return
        }
        val streamer = Build.MODEL.contains("Streamer", ignoreCase = true)
        subtitle.text = s(R.string.setup_remote_text) + if (streamer) "\n" + s(R.string.setup_remote_streamer) else ""
        status.text = if (prefs.keyCode != 0 || prefs.scanCode != 0) act.getString(R.string.key_selected, RemoteKeys.name(act, prefs.keyCode))
        else s(R.string.waiting_key)
        setButtons(listOf(s(R.string.next) to { show(Step.CLEAN) }, s(R.string.skip) to { show(Step.CLEAN) }))
        listen()
    }

    private fun listen() {
        HomeGuardService.learner = { code, scan ->
            if (step == Step.REMOTE) {
                prefs.keyCode = code
                prefs.scanCode = scan
                prefs.keyOriginal = RemoteKeys.original(code) ?: ""
                HomeGuardService.reload()
                status.text = act.getString(R.string.key_selected, RemoteKeys.name(act, code))
                listen()
            }
        }
    }

    fun route(focused: View, dir: Int): View? {
        if (step == Step.THEME) return themePicker?.route(focused, dir)
        val b = buttonViews.indexOf(focused)
        if (b >= 0) return when (dir) {
            View.FOCUS_LEFT -> buttonViews.getOrNull(b - 1) ?: focused
            View.FOCUS_RIGHT -> buttonViews.getOrNull(b + 1) ?: focused
            View.FOCUS_UP -> if (grid.visibility == View.VISIBLE) gridTiles.getOrNull(min(gridTiles.size - 1, (gridScroll / (Grid(prefs.favCount).th + 34)) * cols + 2)) ?: focused else focused
            else -> focused
        }
        val g = gridTiles.indexOf(focused)
        if (g >= 0) {
            val col = g % cols
            return when (dir) {
                View.FOCUS_LEFT -> if (col > 0) gridTiles[g - 1] else focused
                View.FOCUS_RIGHT -> if (col < cols - 1 && g + 1 < gridTiles.size) gridTiles[g + 1] else focused
                View.FOCUS_UP -> gridTiles.getOrNull(g - cols) ?: focused
                View.FOCUS_DOWN -> gridTiles.getOrNull(g + cols) ?: buttonViews.firstOrNull() ?: focused
                else -> null
            }
        }
        return entry()
    }

    fun back() {
        val prev = flow.getOrNull(flow.indexOf(step) - 1) ?: return
        show(prev)
    }

    fun onResumeSetup() {
        when (step) {
            Step.HOME -> homeStep()
            Step.REMOTE -> if (BuildConfig.HOME_GUARD) remoteStep()
            Step.SEECLAP -> if (SeeClap.installedPackage(act) != null) seeclapStep()
            else -> {}
        }
    }
}
