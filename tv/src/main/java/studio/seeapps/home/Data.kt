package studio.seeapps.home

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.Collator
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Bg {
    private val pool = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "home-bg").apply { priority = Thread.NORM_PRIORITY - 1; isDaemon = true }
    }
    val main = Handler(Looper.getMainLooper())
    fun run(task: () -> Unit) = pool.execute { runCatching(task) }
    fun <T> load(task: () -> T, done: (T) -> Unit) = pool.execute {
        val value = runCatching(task).getOrNull() ?: return@execute
        main.post { done(value) }
    }
}

class Prefs private constructor(context: Context) {
    private val p: SharedPreferences = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    companion object {
        @Volatile private var instance: Prefs? = null
        fun get(context: Context): Prefs = instance ?: synchronized(this) {
            instance ?: Prefs(context.applicationContext).also { instance = it }
        }
    }

    private fun list(key: String): List<String> = p.getString(key, "")!!.split(',').filter { it.isNotBlank() }
    private fun putList(key: String, value: Collection<String>) = p.edit().putString(key, value.joinToString(",")).apply()

    var setupDone: Boolean
        get() = p.getBoolean("setup_done", false)
        set(v) = p.edit().putBoolean("setup_done", v).apply()

    var tvListingsAsked: Boolean
        get() = p.getBoolean("tv_listings_asked", false)
        set(v) = p.edit().putBoolean("tv_listings_asked", v).apply()

    var favCount: Int
        get() = if (p.getInt("fav_count", 6) == 5) 5 else 6
        set(v) = p.edit().putInt("fav_count", if (v == 5) 5 else 6).apply()

    var favorites: List<String>
        get() = list("favorites")
        set(v) = putList("favorites", v.filter { !SeeClap.isSeeClap(it) }.distinct())

    var hidden: Set<String>
        get() = list("hidden").filterNot(SeeClap::isSeeClap).toSet()
        set(v) = putList("hidden", v.filterNot(SeeClap::isSeeClap))

    var theme: String
        get() = p.getString("theme", "auto") ?: "auto"
        set(v) = p.edit().putString("theme", v).apply()

    var showClock: Boolean
        get() = p.getBoolean("clock", true)
        set(v) = p.edit().putBoolean("clock", v).apply()

    var reduceMotion: Boolean
        get() = p.getBoolean("reduce_motion", false)
        set(v) = p.edit().putBoolean("reduce_motion", v).apply()

    var guardOn: Boolean
        get() = p.getBoolean("guard_on", true)
        set(v) = p.edit().putBoolean("guard_on", v).apply()

    var keyCode: Int
        get() = p.getInt("key_code", 0)
        set(v) = p.edit().putInt("key_code", v).apply()
    var scanCode: Int
        get() = p.getInt("scan_code", 0)
        set(v) = p.edit().putInt("scan_code", v).apply()

    var keyOriginal: String
        get() = p.getString("key_original", "")!!
        set(v) = p.edit().putString("key_original", v).apply()
    var holdOriginal: Boolean
        get() = p.getBoolean("hold_original", true)
        set(v) = p.edit().putBoolean("hold_original", v).apply()

    var disabledByUs: Set<String>
        get() = list("disabled_by_us").toSet()
        set(v) = putList("disabled_by_us", v)

    var awaitingGuard: Boolean
        get() = p.getBoolean("awaiting_guard", false)
        set(v) = p.edit().putBoolean("awaiting_guard", v).apply()

    var lastUpdateCheck: Long
        get() = p.getLong("last_update_check", 0)
        set(v) = p.edit().putLong("last_update_check", v).apply()

    fun clearKey() = p.edit().remove("key_code").remove("scan_code").remove("key_original").apply()
}

data class AppEntry(val pkg: String, val component: ComponentName, val label: String, val system: Boolean)

object Apps {
    @Volatile var all: List<AppEntry> = emptyList()
        private set

    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val found = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (ri in runCatching { pm.queryIntentActivities(query, 0) }.getOrDefault(emptyList())) {
                val ai = ri.activityInfo ?: continue
                if (ai.packageName == context.packageName || found.containsKey(ai.packageName)) continue
                val label = runCatching { ri.loadLabel(pm).toString().trim() }.getOrDefault(ai.packageName)
                found[ai.packageName] = AppEntry(
                    ai.packageName, ComponentName(ai.packageName, ai.name), label,
                    (ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
        }
        val collator = Collator.getInstance()
        return found.values.sortedWith { a, b -> collator.compare(a.label, b.label) }.also { all = it }
    }

    fun find(pkg: String): AppEntry? = all.firstOrNull { it.pkg == pkg }

    fun launch(context: Context, pkg: String): Boolean {
        val pm = context.packageManager
        val intent = runCatching { pm.getLeanbackLaunchIntentForPackage(pkg) }.getOrNull()
            ?: runCatching { pm.getLaunchIntentForPackage(pkg) }.getOrNull() ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    fun openDetails(context: Context, pkg: String) =
        HomeButton.systemPage(context, Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    fun uninstall(context: Context, pkg: String) =
        !SeeClap.isSeeClap(pkg) && HomeButton.systemPage(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    fun favorites(context: Context): List<String> {
        val prefs = Prefs.get(context)
        val installed = all.map { it.pkg }.toSet()
        val chosen = prefs.favorites.filter { it in installed && !SeeClap.isSeeClap(it) }
        val picked = if (chosen.isEmpty() && !prefs.setupDone) defaultFavorites(installed) else chosen
        return listOf(SeeClap.tilePackage(context)) + picked.take(prefs.favCount - 1)
    }

    private fun defaultFavorites(installed: Set<String>): List<String> = listOf(
        "com.google.android.youtube.tv", "com.netflix.ninja", "com.disney.disneyplus", "com.amazon.amazonvideo.livingroom",
        "com.canal.android.canal", "com.google.android.videos", "com.spotify.tv.android",
    ).filter { it in installed }
}

object SeeClap {
    const val PKG = "com.seeappsstudio.seeclap"

    const val PKG_AMAZON = "com.codingbyjerez.seeclap"
    const val PLAY_STORE = "com.android.vending"
    const val SITE_MANIFEST = "https://downloads.seeapps.studio/seeclap/android/latest.json"
    const val SITE_ORIGIN = "https://downloads.seeapps.studio"
    const val SITE_PAGE = "https://downloads.seeapps.studio/seeclap/android/"

    const val SIGNING_SHA256 = "6b35301365629dc1f4ebdc6a77e19152dc90d059079b39c03572602001b9888e"

    fun programsUri(pkg: String): Uri = Uri.parse("content://$pkg.home/programs")

    fun isSeeClap(pkg: String) = pkg == PKG || pkg == PKG_AMAZON

    fun isInstalled(context: Context, pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    fun installedPackage(context: Context): String? = listOf(PKG, PKG_AMAZON).firstOrNull { isInstalled(context, it) }

    fun tilePackage(context: Context): String = installedPackage(context) ?: if (isFireTv(context)) PKG_AMAZON else PKG

    fun hasPlayStore(context: Context) = isInstalled(context, PLAY_STORE)

    fun isFireTv(context: Context) = context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")

    fun versionCode(context: Context, pkg: String = PKG): Long = runCatching {
        val info = context.packageManager.getPackageInfo(pkg, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrDefault(0L)

    fun versionName(context: Context, pkg: String): String =
        runCatching { context.packageManager.getPackageInfo(pkg, 0).versionName ?: "" }.getOrDefault("")

    fun installerOf(context: Context, pkg: String): String? = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 30) context.packageManager.getInstallSourceInfo(pkg).installingPackageName
        else @Suppress("DEPRECATION") context.packageManager.getInstallerPackageName(pkg)
    }.getOrNull()

    fun openStore(context: Context): Boolean {
        val intent = if (isFireTv(context)) Intent(Intent.ACTION_VIEW, Uri.parse("amzn://apps/android?p=$PKG_AMAZON"))
        else Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PKG")).setPackage(PLAY_STORE)
        return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    }

    fun open(context: Context, link: String?): Boolean {
        val pkg = installedPackage(context) ?: return false
        if (link.isNullOrBlank()) return Apps.launch(context, pkg)
        return runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrElse { Apps.launch(context, pkg) }
    }
}

data class Program(
    val live: Boolean,
    val title: String,
    val meta: String,
    val image: String?,
    val progress: Float,
    val remainingMin: Int,
    val link: String?,
    val restartLink: String?,

    val logo: String? = null,

    val overview: String = "",
    val opensDetails: Boolean = false,

    val section: String? = null,

    val artAspect: Float? = null,
)

internal data class ProgramSource(val pkg: String, val legacy: Boolean)

object Programs {

    private const val DEBUG_HOME_SHA256 = "8d7ac536d36b7647a40b3c08ddbc8968930c74b66ed0535924131919dd3a03be"

    internal fun source(context: Context, pkg: String): ProgramSource? {
        if (!SeeClap.isInstalled(context, pkg)) return null
        val pm = context.packageManager
        val direct = if (SeeClap.isSeeClap(pkg)) pm.resolveContentProvider("$pkg.home", 0) else null
        return source(pkg, direct?.packageName, android.os.Build.VERSION.SDK_INT, canReadDirect = {
            direct?.exported == true && (
                pm.checkSignatures(context.packageName, pkg) == PackageManager.SIGNATURE_MATCH ||
                    (direct.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 &&
                        hasDebugHomeCertificate(pm, context.packageName))
                )
        }) {
            pm.resolveContentProvider(android.media.tv.TvContract.AUTHORITY, 0) != null &&
                runCatching { pm.getPermissionInfo(TvListings.PERMISSION, 0) }.isSuccess
        }
    }

    private fun hasDebugHomeCertificate(pm: PackageManager, pkg: String): Boolean = runCatching {
        val digest = DEBUG_HOME_SHA256.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            pm.hasSigningCertificate(pkg, digest, PackageManager.CERT_INPUT_SHA256)
        } else {
            @Suppress("DEPRECATION")
            val signatures = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures.orEmpty()
            val sha256 = MessageDigest.getInstance("SHA-256")
            signatures.isNotEmpty() && signatures.all { sha256.digest(it.toByteArray()).contentEquals(digest) }
        }
    }.getOrDefault(false)

    internal fun source(
        pkg: String, directOwner: String?, sdk: Int,
        canReadDirect: () -> Boolean = { false }, hasTvListings: () -> Boolean,
    ): ProgramSource? {
        if (SeeClap.isSeeClap(pkg) && directOwner != null) {
            if (directOwner != pkg) return null
            if (canReadDirect()) return ProgramSource(pkg, false)
        }
        return if (sdk >= 26 && pkg != SeeClap.PKG_AMAZON && hasTvListings()) ProgramSource(pkg, true) else null
    }

    internal fun read(context: Context, source: ProgramSource): List<Program> {
        if (source.legacy) return TvListings.read(context, source.pkg)
        val out = ArrayList<Program>()
        runCatching {
            context.contentResolver.query(SeeClap.programsUri(source.pkg), null, null, null, null)?.use { c ->
                fun s(name: String) = c.getColumnIndex(name).takeIf { it >= 0 }?.let { c.getString(it) }
                fun l(name: String) = c.getColumnIndex(name).takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L
                while (c.moveToNext() && out.size < 8) {
                    val title = s("title")?.trim().orEmpty()
                    if (title.isEmpty()) continue
                    val image = s("image_url")?.takeIf { it.startsWith("https://") || it.startsWith("content://") }
                    out += Program(
                        live = s("kind") == "live",
                        title = title,
                        meta = s("subtitle")?.trim().orEmpty(),
                        image = image,
                        progress = (l("progress").coerceIn(0, 1000)) / 1000f,
                        remainingMin = ((l("remaining_ms") + 59_999) / 60_000).toInt(),
                        link = s("deep_link"),
                        restartLink = s("restart_link"),
                        logo = s("logo_url")?.takeIf { it.startsWith("https://") },
                        overview = s("overview")?.trim().orEmpty(),
                    )
                }
            }
        }
        return out
    }
}

object Banners {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 6).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun clear() = cache.evictAll()

    fun get(context: Context, pkg: String, w: Int, h: Int, done: (Bitmap) -> Unit) {
        val key = "$pkg@$w"
        cache.get(key)?.let { done(it); return }
        val app = context.applicationContext
        Bg.load({ render(app, pkg, w, h) }) { bmp ->
            cache.put(key, bmp)
            done(bmp)
        }
    }

    private fun render(context: Context, pkg: String, w: Int, h: Int): Bitmap {
        val pm = context.packageManager
        val bmp = Bitmap.createBitmap(w, h, if (Ui.light) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val entry = Apps.find(pkg)
        if (SeeClap.isSeeClap(pkg) && SeeClap.installedPackage(context) == null) {
            drawBitmapFill(canvas, BitmapFactory.decodeResource(context.resources, R.drawable.seeclap_banner), w, h)
            return bmp
        }
        val banner: Drawable? = entry?.let { runCatching { pm.getActivityBanner(it.component) }.getOrNull() }
            ?: runCatching { pm.getApplicationBanner(pkg) }.getOrNull()
        if (banner != null) {
            canvas.drawColor(0xFF1C1C1E.toInt())
            banner.setBounds(0, 0, w, h)
            banner.draw(canvas)
            return bmp
        }
        val icon = entry?.let { runCatching { pm.getActivityIcon(it.component) }.getOrNull() }
            ?: runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
        drawFallback(canvas, w, h, icon, entry?.label ?: pkg)
        return bmp
    }

    private fun drawBitmapFill(canvas: Canvas, src: Bitmap?, w: Int, h: Int) {
        if (src == null) return
        canvas.drawBitmap(src, null, android.graphics.Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawFallback(canvas: Canvas, w: Int, h: Int, icon: Drawable?, label: String) {
        val tint = icon?.let { averageColor(it) } ?: 0xFF3A3A3C.toInt()
        val hsv = FloatArray(3).also { Color.colorToHSV(tint, it) }
        hsv[1] = min(hsv[1], 0.55f)
        val top = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 0.42f))
        val bottom = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 0.2f))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        val size = (h * 0.4f).roundToInt()
        icon?.let {
            val left = (w - size) / 2
            val topY = (h * 0.16f).roundToInt()
            it.setBounds(left, topY, left + size, topY + size)
            it.draw(canvas)
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = h * 0.13f
            typeface = Ui.semibold
            textAlign = Paint.Align.CENTER
        }
        var shown = label
        while (shown.length > 3 && text.measureText(shown) > w * 0.86f) shown = shown.dropLast(2) + "…"
        canvas.drawText(shown, w / 2f, h * 0.84f, text)
    }

    private fun averageColor(icon: Drawable): Int {
        val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 8, 8)
        icon.draw(Canvas(bmp))
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0L
        for (x in 0 until 8) for (y in 0 until 8) {
            val c = bmp.getPixel(x, y)
            if (Color.alpha(c) < 128) continue
            r += Color.red(c); g += Color.green(c); b += Color.blue(c); n++
        }
        bmp.recycle()
        return if (n == 0L) 0xFF3A3A3C.toInt() else Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }
}

object Images {
    private const val MAX_BYTES = 8 * 1024 * 1024
    private const val CACHE_BUDGET = 40L * 1024 * 1024
    private var cacheChecked = false

    fun load(context: Context, url: String, maxWidth: Int, alpha: Boolean = false): Bitmap? {
        val dir = File(context.cacheDir, "img").apply { mkdirs() }
        synchronized(this) {
            if (!cacheChecked) {

                dir.listFiles { _, name -> name.endsWith(".part") }?.forEach { it.delete() }
                trim(context)
                cacheChecked = true
            }
        }
        val file = File(dir, sha1(url) + ".img")
        val needsDownload = !file.isFile || file.length() == 0L
        if (needsDownload) {
            var connection: HttpURLConnection? = null
            val tmp = File.createTempFile("art-", ".part", file.parentFile)
            try {
                val input = when (Uri.parse(url).scheme) {
                    "content", "android.resource" -> context.contentResolver.openInputStream(Uri.parse(url))
                    "https" -> {
                        val conn = URL(url).openConnection() as HttpURLConnection
                        connection = conn
                        conn.connectTimeout = 8000
                        conn.readTimeout = 15000
                        if (conn.responseCode != 200) return null
                        conn.inputStream
                    }
                    else -> null
                } ?: return null
                input.use { stream ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(32 * 1024)
                        var total = 0
                        while (true) {
                            val n = stream.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > MAX_BYTES) return null
                            out.write(buf, 0, n)
                        }
                    }
                }
                if (!tmp.renameTo(file) && !file.isFile) return null
            } finally {
                connection?.disconnect()
                tmp.delete()
            }
        }

        return file.inputStream().use { input ->
            synchronized(this) {
                file.setLastModified(System.currentTimeMillis())
                if (needsDownload) trim(context)
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFileDescriptor(input.fd, null, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outHeight > 8L * bounds.outWidth) return null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxWidth) sample *= 2
            BitmapFactory.decodeFileDescriptor(input.fd, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = if (alpha) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            })
        }
    }

    fun trim(context: Context) {
        val dir = File(context.cacheDir, "img")
        val files = dir.listFiles { _, name -> name.endsWith(".img") }?.sortedByDescending { it.lastModified() } ?: return
        var kept = 0L
        for (f in files) {
            val size = f.length()
            if (kept + size <= CACHE_BUDGET || !f.delete()) kept += size
        }
    }

    private fun sha1(s: String) = MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    fun maxWidth(): Int = max(640, Ui.px(if (Ui.light) 1280 else 1920))
}
