package studio.seeapps.home

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent

/*
 * La garde du bouton Accueil et des boutons d'app de la télécommande.
 * Plusieurs mécanismes viennent de CleanYourTV (HomeKeyService, RemoteKeys), sous licence MIT :
 * Copyright (c) 2026 Gathin — voir THIRD_PARTY_NOTICES.md.
 */

object HomeButton {
    const val GOOGLE_TV_HOME = "com.google.android.apps.tv.launcherx"

    fun defaultHome(context: Context): String? = runCatching {
        context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
    }.getOrNull()

    fun isDefault(context: Context) = defaultHome(context) == context.packageName

    fun isGoogleTv(context: Context) = SeeClap.isInstalled(context, GOOGLE_TV_HOME)

    fun canAskRole(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29 || isGoogleTv(context)) return false
        val rm = context.getSystemService(RoleManager::class.java) ?: return false
        return rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)
    }

    fun askRole(activity: Activity, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val rm = activity.getSystemService(RoleManager::class.java) ?: return false
        return runCatching { activity.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), requestCode); true }
            .getOrDefault(false)
    }

    fun guardComponent(context: Context) = ComponentName(context, HomeGuardService::class.java)

    fun guardEnabled(context: Context): Boolean {
        if (BuildConfig.SITE) return false
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = guardComponent(context)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun ready(context: Context) = isDefault(context) || guardEnabled(context)

    fun openAccessibility(context: Context): Boolean = open(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun openHomeSettings(context: Context): Boolean = open(context, Settings.ACTION_HOME_SETTINGS)

    fun open(context: Context, action: String): Boolean = systemPage(context, Intent(action))

    fun systemPage(context: Context, intent: Intent): Boolean = runCatching {
        val activity = (context as? Activity) ?: HomeActivity.shown?.get()?.takeIf { !it.isFinishing }
        if (activity != null) activity.startActivity(intent) else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    fun otherGuard(context: Context): String? {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return null
        val pm = context.packageManager
        val homes = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0).map { it.activityInfo.packageName }.toSet()
        }.getOrDefault(emptySet())
        val pkg = enabled.split(':').mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
            .firstOrNull { it != context.packageName && it in homes } ?: return null
        return runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
    }

    fun openStockHome(context: Context): Boolean {
        val pkg = defaultHome(context)?.takeIf { it != context.packageName && it != "android" } ?: return false
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        }.getOrDefault(false)
    }
}

object RemoteKeys {

    class Known(val name: String?, val nameRes: Int, val original: String?)

    private val known = mapOf(
        KeyEvent.KEYCODE_BUTTON_3 to Known("YouTube", 0, "com.google.android.youtube.tv"),
        KeyEvent.KEYCODE_BUTTON_4 to Known("Netflix", 0, "com.netflix.ninja"),
        KeyEvent.KEYCODE_BOOKMARK to Known("★", 0, null),
        KeyEvent.KEYCODE_PROG_RED to Known(null, R.string.key_red, null),
        KeyEvent.KEYCODE_PROG_GREEN to Known(null, R.string.key_green, null),
        KeyEvent.KEYCODE_PROG_YELLOW to Known(null, R.string.key_yellow, null),
        KeyEvent.KEYCODE_PROG_BLUE to Known(null, R.string.key_blue, null),
        KeyEvent.KEYCODE_GUIDE to Known("Guide", 0, null),
        KeyEvent.KEYCODE_TV to Known("TV", 0, null),
        KeyEvent.KEYCODE_ALL_APPS to Known("Apps", 0, null),
    )

    private val reserved = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_SLEEP,
        KeyEvent.KEYCODE_WAKEUP, KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SEARCH,
        KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_TV_INPUT,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_UNKNOWN,
    )

    fun canLearn(event: KeyEvent) = event.keyCode !in reserved && !(event.keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9)

    fun original(keyCode: Int): String? = known[keyCode]?.original

    fun name(context: Context, keyCode: Int): String {
        val k = known[keyCode]
        if (k?.name != null) return k.name
        if (k != null && k.nameRes != 0) return context.getString(k.nameRes)
        if (keyCode <= 0) return "?"
        return KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    const val HOLD_MS = 500L
}

class HomeGuardService : AccessibilityService() {
    companion object {
        @Volatile var running: HomeGuardService? = null
            private set

        @Volatile var learner: ((keyCode: Int, scanCode: Int) -> Unit)? = null

        fun reload() = running?.loadConfig()

        const val HOME_KEY_DELAY_MS = 250L
        private const val BOOT_WINDOW_MS = 3 * 60 * 1000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val stockHome = HashSet<String>()
    private var stockPackage = ""
    private var keyCode = 0
    private var scanCode = 0
    private var holdTarget: String? = null
    private var downAt = -1L
    private var holdFired = false
    private val hold = Runnable {
        holdFired = true
        holdTarget?.let { Apps.launch(this, it) }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = this
        loadConfig()

        val prefs = Prefs.get(this)
        if (prefs.awaitingGuard) {
            prefs.awaitingGuard = false
            handler.postDelayed({
                runCatching {
                    startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                }
            }, 500)
        }
    }

    override fun onDestroy() {
        if (running == this) running = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    fun loadConfig() {
        val prefs = Prefs.get(this)
        keyCode = prefs.keyCode
        scanCode = prefs.scanCode
        holdTarget = prefs.keyOriginal.takeIf { prefs.holdOriginal && it.isNotEmpty() }
        stockHome.clear()
        stockPackage = ""
        val home = HomeButton.defaultHome(this)
        if (prefs.guardOn && home != null && home != packageName && home != "android") {
            stockPackage = home
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(home)
            for (ri in runCatching { packageManager.queryIntentActivities(intent, PackageManager.MATCH_DISABLED_COMPONENTS) }.getOrDefault(emptyList())) {
                stockHome += ri.activityInfo.name
            }
        }

        serviceInfo?.let { info ->
            info.eventTypes = if (stockHome.isEmpty()) 0 else AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.packageNames = if (stockHome.isEmpty()) emptyArray() else arrayOf(stockPackage)
            serviceInfo = info
        }
        if (stockHome.isNotEmpty()) probeAtBoot()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val cls = event.className?.toString() ?: return
        if (event.packageName?.toString() == stockPackage && cls in stockHome && Prefs.get(this).setupDone) openHome()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        learner?.let { learn ->
            if (RemoteKeys.canLearn(event)) {
                if (event.action == KeyEvent.ACTION_UP) {
                    learner = null
                    val code = event.keyCode
                    val scan = event.scanCode
                    handler.post { learn(code, scan) }
                }
                return true
            }
        }
        if (matches(event)) return press(event)
        if (event.keyCode == KeyEvent.KEYCODE_HOME && stockHome.isNotEmpty()) {

            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled &&
                event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout() && Prefs.get(this).setupDone
            ) handler.postDelayed({ openHome() }, HOME_KEY_DELAY_MS)
            return false
        }

        if (event.keyCode == KeyEvent.KEYCODE_ALL_APPS && stockHome.isNotEmpty() && keyCode != KeyEvent.KEYCODE_ALL_APPS) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) openHome()
            return true
        }
        return false
    }

    private fun matches(event: KeyEvent): Boolean {
        if (keyCode == 0 && scanCode == 0) return false

        return if (keyCode == 0 || keyCode == KeyEvent.KEYCODE_UNKNOWN) event.scanCode == scanCode
        else event.keyCode == keyCode && (scanCode == 0 || event.scanCode == 0 || event.scanCode == scanCode)
    }

    private fun press(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                downAt = event.eventTime
                holdFired = false
                handler.removeCallbacks(hold)
                if (holdTarget != null) handler.postDelayed(hold, RemoteKeys.HOLD_MS)
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(hold)
                if (!event.isCanceled && !holdFired) openSeeClap()
                downAt = -1
            }
        }
        return true
    }

    private fun openSeeClap() {
        val pkg = SeeClap.installedPackage(this)
        if (pkg == null || !Apps.launch(this, pkg)) openHome()
    }

    private fun openHome() {
        val intent = if (stockHome.isNotEmpty()) Intent(this, HomeActivity::class.java)
        else Intent(Intent.ACTION_MAIN).setPackage(packageName)
        runCatching {
            startActivity(intent.addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        }
    }

    private fun probeAtBoot() {
        val boot = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
        val prefs = getSharedPreferences("boot", MODE_PRIVATE)
        if (boot < 0 || boot == prefs.getInt("probe_boot", -1) || SystemClock.elapsedRealtime() > BOOT_WINDOW_MS) return
        prefs.edit().putInt("probe_boot", boot).apply()
        runCatching {
            startActivity(Intent(this, ProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }
    }

    override fun onInterrupt() {}
}

class ProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }
}
