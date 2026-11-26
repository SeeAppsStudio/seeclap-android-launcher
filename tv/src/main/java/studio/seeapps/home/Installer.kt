package studio.seeapps.home

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object Installer {
    const val ACTION_RESULT = "studio.seeapps.home.INSTALL_RESULT"
    private const val MAX_APK = 1024L * 1024 * 1024
    private const val MAX_MANIFEST = 64 * 1024

    sealed class State {
        object Checking : State()
        data class Downloading(val percent: Int) : State()
        object Verifying : State()

        object NeedsPermission : State()

        object Confirming : State()
        object Done : State()
        object UpToDate : State()

        data class Ready(val versionName: String) : State()
        data class Failed(val reason: Int) : State()

        object Idle : State()
    }

    private val listeners = LinkedHashSet<(String, State) -> Unit>()
    private val states = HashMap<String, State>()
    private val busy: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())

    fun listen(l: (String, State) -> Unit) { listeners += l }
    fun unlisten(l: (String, State) -> Unit) { listeners -= l }
    fun state(pkg: String): State? = states[pkg]

    fun post(pkg: String, state: State) = Bg.main.post {
        states[pkg] = state
        if (state is State.Done || state is State.Failed || state is State.UpToDate || state is State.Ready || state is State.Idle) busy.remove(pkg)
        listeners.toList().forEach { it(pkg, state) }
    }

    class Release(
        val packageName: String, val versionCode: Long, val versionName: String, val minSdk: Int,
        val size: Long, val sha256: String, val url: String, val payload: ByteArray, val signature: ByteArray,
    )

    private class Refused(val reason: Int) : Exception()

    fun installSeeClap(context: Context, fromSite: Boolean = false) {
        val app = context.applicationContext
        if (synchronized(pendingFile) { pendingFile.containsKey(SeeClap.PKG) }) {
            if (canInstall(app)) resumePending(app) else openUnknownSources(app)
            return
        }
        if (!fromSite && (SeeClap.hasPlayStore(app) || SeeClap.isFireTv(app)) && SeeClap.openStore(app)) {
            post(SeeClap.PKG, State.Confirming)
            return
        }
        if (!BuildConfig.SITE) {
            post(SeeClap.PKG, State.Failed(R.string.install_failed_store))
            return
        }
        runFlow(app, SeeClap.PKG) {
            val r = fetch(SeeClap.SITE_MANIFEST)
            checkUrl(r, "${SeeClap.SITE_ORIGIN}/seeclap/android/seeclap-${r.versionCode}.apk", SeeClap.PKG)
            val file = download(app, r)
            verifySeeClap(app, file, r)
            install(app, file, r.packageName, silent = false)
        }
    }

    fun updateSeeClap(context: Context, auto: Boolean) {
        val app = context.applicationContext
        if (!BuildConfig.SITE || SeeClap.installerOf(app, SeeClap.PKG) == SeeClap.PLAY_STORE) return
        if (!SeeClap.isInstalled(app, SeeClap.PKG)) return
        runFlow(app, SeeClap.PKG, quiet = auto) {
            val r = fetch(SeeClap.SITE_MANIFEST)
            if (r.versionCode <= SeeClap.versionCode(app)) { post(SeeClap.PKG, State.UpToDate); return@runFlow }
            checkUrl(r, "${SeeClap.SITE_ORIGIN}/seeclap/android/seeclap-${r.versionCode}.apk", SeeClap.PKG)
            val file = download(app, r)
            verifySeeClap(app, file, r)
            val silent = Build.VERSION.SDK_INT >= 31 && SeeClap.installerOf(app, SeeClap.PKG) == app.packageName
            if (auto && !silent) post(SeeClap.PKG, State.Ready(r.versionName))
            else install(app, file, r.packageName, silent)
        }
    }

    fun updateSelf(context: Context, install: Boolean) {
        val app = context.applicationContext
        if (!BuildConfig.SITE) return
        runFlow(app, app.packageName, quiet = !install) {
            val r = fetch(BuildConfig.SELF_UPDATE_MANIFEST)
            if (r.packageName != app.packageName || r.versionCode <= BuildConfig.VERSION_CODE) { post(app.packageName, State.UpToDate); return@runFlow }
            checkUrl(r, "${SeeClap.SITE_ORIGIN}/seeclap-launcher/android/seeclap-launcher-${r.versionCode}.apk", app.packageName)

            val mine = certificates(app.packageManager.getPackageInfo(app.packageName, signingFlags()))
            if (!verifySignature(r.payload, r.signature, mine)) throw Refused(R.string.install_failed_signature)
            val file = download(app, r)
            val info = archive(app, file) ?: throw Refused(R.string.install_failed_signature)
            val theirs = certificates(info)
            if (!matchesRelease(info, r) || theirs.isEmpty() || theirs.map { sha256(it.encoded) }.toSet() != mine.map { sha256(it.encoded) }.toSet())
                throw Refused(R.string.install_failed_signature)
            if (install) install(app, file, app.packageName, silent = false) else post(app.packageName, State.Ready(r.versionName))
        }
    }

    private fun runFlow(app: Context, pkg: String, quiet: Boolean = false, block: () -> Unit) {
        if (!busy.add(pkg)) return
        if (!quiet) post(pkg, State.Checking)
        Bg.run {
            try {
                block()
            } catch (r: Refused) {
                post(pkg, if (quiet) State.Idle else State.Failed(r.reason))
            } catch (e: Exception) {
                post(pkg, if (quiet) State.Idle else State.Failed(R.string.install_failed_network))
            }
        }
    }

    private fun fetch(url: String): Release {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Cache-Control", "no-cache")
        val bytes = try {
            if (conn.responseCode != 200) throw Refused(R.string.install_failed_network)
            conn.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_MANIFEST) throw Refused(R.string.install_failed_network)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            conn.disconnect()
        }
        val envelope = JSONObject(String(bytes, Charsets.UTF_8))
        val payload = Base64.decode(envelope.getString("manifest"), Base64.NO_WRAP)
        val signature = Base64.decode(envelope.getString("signature"), Base64.NO_WRAP)
        val d = JSONObject(String(payload, Charsets.UTF_8))
        if (d.getInt("schema") != 1) throw Refused(R.string.install_failed_network)
        return Release(
            d.getString("packageName"), d.getLong("versionCode"), d.getString("versionName"), d.getInt("minSdk"),
            d.getLong("size"), d.getString("sha256"), d.getString("url"), payload, signature,
        )
    }

    private fun checkUrl(r: Release, expected: String, pkg: String) {
        val ok = r.packageName == pkg && r.minSdk <= Build.VERSION.SDK_INT && r.size in 1..MAX_APK &&
            r.sha256.matches(Regex("[0-9a-f]{64}")) && runCatching { URI(r.url).toASCIIString() == expected }.getOrDefault(false)
        if (!ok) throw Refused(R.string.install_failed_signature)
    }

    private fun download(app: Context, r: Release): File {
        val dir = File(app.cacheDir, "updates").apply { mkdirs() }
        val file = updateCacheFile(dir, r.packageName, r.versionCode)
        if (file.length() == r.size && sha256(file) == r.sha256) return file
        val conn = URL(r.url).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        try {
            if (conn.responseCode != 200) throw Refused(R.string.install_failed_network)
            var got = 0L
            var last = -1
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        got += n
                        if (got > r.size) throw Refused(R.string.install_failed_signature)
                        out.write(buf, 0, n)
                        val pct = (got * 100 / r.size).toInt()
                        if (pct != last) { last = pct; post(r.packageName, State.Downloading(pct)) }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        post(r.packageName, State.Verifying)
        if (file.length() != r.size || sha256(file) != r.sha256) {
            file.delete()
            throw Refused(R.string.install_failed_signature)
        }
        return file
    }

    private fun verifySeeClap(app: Context, file: File, r: Release) {
        val info = archive(app, file)
        val certs = info?.let { certificates(it) }.orEmpty()
        val ok = info != null && matchesRelease(info, r) &&
            certs.isNotEmpty() && certs.all { sha256(it.encoded) == SeeClap.SIGNING_SHA256 } &&
            verifySignature(r.payload, r.signature, certs)
        if (!ok) {
            file.delete()
            throw Refused(R.string.install_failed_signature)
        }
    }

    private fun matchesRelease(info: PackageInfo, release: Release): Boolean {
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        return info.packageName == release.packageName && code == release.versionCode
    }

    private fun signingFlags() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    private fun archive(app: Context, file: File): PackageInfo? =
        runCatching { app.packageManager.getPackageArchiveInfo(file.path, signingFlags()) }.getOrNull()

    private fun certificates(info: PackageInfo): List<X509Certificate> {
        val raw = if (Build.VERSION.SDK_INT >= 28) {
            val si = info.signingInfo ?: return emptyList()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory?.takeLast(1)?.toTypedArray() ?: si.apkContentsSigners
        } else @Suppress("DEPRECATION") info.signatures
        val cf = CertificateFactory.getInstance("X.509")
        return raw.orEmpty().mapNotNull { runCatching { cf.generateCertificate(ByteArrayInputStream(it.toByteArray())) as X509Certificate }.getOrNull() }
    }

    fun verifySignature(payload: ByteArray, signature: ByteArray, certs: List<X509Certificate>): Boolean {
        if (payload.isEmpty() || payload.size > MAX_MANIFEST || signature.isEmpty()) return false
        return certs.any { cert ->
            runCatching {
                val algo = when (cert.publicKey.algorithm) {
                    "RSA" -> "SHA256withRSA"
                    "EC" -> "SHA256withECDSA"
                    else -> return@runCatching false
                }
                Signature.getInstance(algo).run { initVerify(cert.publicKey); update(payload); verify(signature) }
            }.getOrDefault(false)
        }
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private val pendingFile = HashMap<String, File>()

    fun resumePending(context: Context) {
        val app = context.applicationContext
        val entries = synchronized(pendingFile) { pendingFile.toMap() }
        for ((pkg, file) in entries) {
            if (canInstall(app)) {
                synchronized(pendingFile) { pendingFile.remove(pkg) }
                Bg.run { runCatching { install(app, file, pkg, silent = false) }.onFailure { post(pkg, State.Failed(R.string.install_failed_network)) } }
            }
        }
    }

    fun openUnknownSources(app: Context) {
        val opened = Build.VERSION.SDK_INT >= 26 &&
            HomeButton.systemPage(app, Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}")))
        if (!opened) HomeButton.open(app, Settings.ACTION_SECURITY_SETTINGS)
    }

    fun canInstall(context: Context) = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    private fun install(app: Context, file: File, pkg: String, silent: Boolean) {
        if (!canInstall(app)) {
            synchronized(pendingFile) { pendingFile[pkg] = file }
            post(pkg, State.NeedsPermission)
            openUnknownSources(app)
            return
        }
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(pkg)
        params.setSize(file.length())
        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(
                if (silent) PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED else PackageInstaller.SessionParams.USER_ACTION_REQUIRED,
            )
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out, 64 * 1024) }
                session.fsync(out)
            }
            val intent = Intent(app, InstallReceiver::class.java).setAction(ACTION_RESULT).putExtra("pkg", pkg)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(app, id, intent, flags).intentSender)
        }
        post(pkg, State.Confirming)
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Installer.ACTION_RESULT) return
        val pkg = intent.getStringExtra("pkg") ?: return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                if (confirm != null) runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                Installer.post(pkg, Installer.State.Confirming)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                File(context.cacheDir, "updates").listFiles()?.filter { it.name.startsWith("$pkg-") }?.forEach { it.delete() }
                Installer.post(pkg, Installer.State.Done)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> Installer.post(pkg, Installer.State.Failed(R.string.install_failed_cancelled))
            else -> Installer.post(pkg, Installer.State.Failed(R.string.install_failed_android))
        }
    }
}

internal fun updateCacheFile(dir: File, pkg: String, version: Long): File {
    val target = File(dir, "$pkg-$version.apk")
    dir.listFiles()?.filter { it.name.startsWith("$pkg-") && it != target }?.forEach { it.delete() }
    return target
}
