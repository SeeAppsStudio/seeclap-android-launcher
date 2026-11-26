package studio.seeapps.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object Cleaner {

    val TELEMETRY = listOf(
        "com.google.android.feedback",
        "com.android.tv.feedbackconsent",
        "com.google.android.tv.remotecontrol.logging",
        "com.google.android.apps.tv.netoscope",
        "com.google.android.federatedcompute",
        "com.google.android.ondevicepersonalization.services",
        "com.google.android.adservices.api",
        "com.google.android.ext.adservices.api",
        "com.android.printspooler",
    )

    private val PROTECTED = setOf(
        "android", "com.android.systemui", "com.android.settings", "com.android.tv.settings", "com.google.android.tv.settings",
        "com.android.vending", "com.google.android.gms", "com.google.android.gsf", "com.google.android.webview",
        "com.android.webview", "com.google.android.tungsten.setupwraith", "com.google.android.apps.mediashell",
        "com.android.packageinstaller", "com.google.android.packageinstaller", "com.android.providers.tv",
        "com.google.android.tvlauncher", HomeButton.GOOGLE_TV_HOME, "com.google.android.katniss",
        "com.google.android.tv.remote.service", "com.android.bluetooth", "com.google.android.inputmethod.latin",
        "com.google.android.leanback.ime", "com.android.tv.inputs", "com.google.android.apps.tv.launcherx.coreservices",
        SeeClap.PKG, SeeClap.PKG_AMAZON,
    )

    fun isProtected(context: Context, pkg: String) = pkg in PROTECTED || pkg == context.packageName ||
        pkg.startsWith("com.android.providers.") || pkg.startsWith("com.android.inputmethod")

    class Sized(val app: AppEntry, val bytes: Long)

    fun userApps(context: Context): List<Sized> {
        val pm = context.packageManager
        return Apps.all.filter { !it.system && !SeeClap.isSeeClap(it.pkg) }.map { app ->
            val ai = runCatching { pm.getApplicationInfo(app.pkg, 0) }.getOrNull()
            val bytes = ai?.let { info -> (listOf(info.sourceDir) + info.splitSourceDirs.orEmpty()).sumOf { File(it).length() } } ?: 0L
            Sized(app, bytes)
        }.sortedByDescending { it.bytes }
    }

    fun preinstalled(context: Context): List<AppEntry> =
        Apps.all.filter { it.system && !isProtected(context, it.pkg) }

    fun clearOwnCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        Banners.clear()
    }

    fun freeBytes(): Long = runCatching { StatFs(Environment.getDataDirectory().path).availableBytes }.getOrDefault(0L)

    class Plan(
        val caches: Boolean,
        val telemetry: Boolean,
        val faster: Boolean,
        val disable: List<String>,
    )

    class Result(val freedBytes: Long, val disabled: Int, val errors: Int)

    fun deepClean(context: Context, adb: LocalAdb, plan: Plan): Result {
        val prefs = Prefs.get(context)
        val before = freeBytes()
        var errors = 0
        var disabled = 0
        val done = prefs.disabledByUs.toMutableSet()
        val targets = (if (plan.telemetry) TELEMETRY else emptyList()) + plan.disable
        if (targets.isNotEmpty()) {
            val enabled = runCatching { adb.shell("pm list packages -e") }.getOrDefault("")
                .lineSequence().filter { it.startsWith("package:") }.map { it.removePrefix("package:").trim() }.toSet()
            if (enabled.isEmpty()) errors++
            for (pkg in targets.distinct()) {
                if (isProtected(context, pkg) || pkg !in enabled) continue
                val out = runCatching { adb.shell("pm disable-user --user 0 $pkg") }.getOrDefault("")
                if (out.trimEnd().endsWith(": disabled-user")) { disabled++; done += pkg } else errors++
            }
        }
        prefs.disabledByUs = done
        if (plan.caches && !commandSucceeded(adb, "pm trim-caches 999999999999")) errors++
        if (plan.faster) {
            for (key in listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")) {
                if (!commandSucceeded(adb, "settings put global $key 0.5")) errors++
            }
        }
        return Result((freeBytes() - before).coerceAtLeast(0), disabled, errors)
    }

    private fun commandSucceeded(adb: LocalAdb, command: String) = runCatching {
        adb.shell("$command; echo status=$?").trimEnd().lineSequence().last() == "status=0"
    }.getOrDefault(false)

    fun openAccessibilitySettings(adb: LocalAdb): Boolean = BuildConfig.SITE && runCatching {
        adb.shell("am start -W -a ${Settings.ACTION_ACCESSIBILITY_SETTINGS}")
            .lineSequence().any { it.trim() == "Status: ok" }
    }.getOrDefault(false)

    fun restore(context: Context, adb: LocalAdb, pkgs: Collection<String>): Int {
        val prefs = Prefs.get(context)
        val left = prefs.disabledByUs.toMutableSet()
        var n = 0
        for (pkg in pkgs) {
            val out = runCatching { adb.shell("pm enable $pkg") }.getOrDefault("")
            if (out.contains("enabled")) { n++; left -= pkg }
        }
        prefs.disabledByUs = left
        return n
    }

    fun isUserApp(context: Context, pkg: String) = runCatching {
        (context.packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }.getOrDefault(false)

    fun label(context: Context, pkg: String): String = Apps.find(pkg)?.label ?: runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS)).toString()
    }.getOrDefault(pkg)
}

class AdbKey private constructor(private val pair: KeyPair) {
    companion object {
        @Synchronized
        fun load(context: Context): AdbKey {
            val priv = File(context.noBackupFilesDir, "adbkey")
            val pub = File(context.noBackupFilesDir, "adbkey.pub")

            for (target in listOf(priv, pub)) {
                val old = File(context.filesDir, target.name)
                if (!old.exists()) continue
                val migrated = if (target.exists()) old.delete() else old.renameTo(target)
                if (!migrated) throw IOException("Cannot migrate ADB key")
            }
            val kf = KeyFactory.getInstance("RSA")
            if (priv.isFile && pub.isFile) {
                runCatching {
                    return AdbKey(KeyPair(kf.generatePublic(X509EncodedKeySpec(pub.readBytes())), kf.generatePrivate(PKCS8EncodedKeySpec(priv.readBytes()))))
                }
            }
            val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
            priv.writeBytes(pair.private.encoded)
            pub.writeBytes(pair.public.encoded)
            return AdbKey(pair)
        }

        private val PADDING: ByteArray = ByteArray(236).also { p ->
            p[0] = 0; p[1] = 1
            for (i in 2 until 220) p[i] = 0xff.toByte()
            p[220] = 0
            val sha1 = intArrayOf(0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14)
            for (i in sha1.indices) p[221 + i] = sha1[i].toByte()
        }
    }

    fun sign(token: ByteArray): ByteArray {
        val c = Cipher.getInstance("RSA/ECB/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, pair.private)
        c.update(PADDING)
        return c.doFinal(token)
    }

    fun publicPayload(): ByteArray {
        val key = pair.public as RSAPublicKey
        val words = 64
        val r32 = BigInteger.ZERO.setBit(32)
        var n = key.modulus
        val r = BigInteger.ZERO.setBit(words * 32)
        var rr = r.modPow(BigInteger.valueOf(2), n)
        val n0inv = n.remainder(r32).modInverse(r32)
        val buf = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(words)
        buf.putInt(n0inv.negate().toInt())
        val nWords = IntArray(words)
        val rrWords = IntArray(words)
        for (i in 0 until words) {
            var d = rr.divideAndRemainder(r32); rr = d[0]; rrWords[i] = d[1].toInt()
            d = n.divideAndRemainder(r32); n = d[0]; nWords[i] = d[1].toInt()
        }
        nWords.forEach { buf.putInt(it) }
        rrWords.forEach { buf.putInt(it) }
        buf.putInt(key.publicExponent.toInt())
        val text = Base64.encodeToString(buf.array(), Base64.NO_WRAP) + " seeclap-launcher@tv\u0000"
        return text.toByteArray(Charsets.UTF_8)
    }
}

class LocalAdb private constructor(private val socket: Socket) : Closeable {
    class Refused : IOException("refused")
    class NeedsPairing : IOException("tls")

    private val input = DataInputStream(socket.getInputStream().buffered())
    private val output: OutputStream = socket.getOutputStream()
    private var nextId = 1

    companion object {
        private const val CNXN = 0x4e584e43
        private const val AUTH = 0x48545541
        private const val OPEN = 0x4e45504f
        private const val OKAY = 0x59414b4f
        private const val CLSE = 0x45534c43
        private const val WRTE = 0x45545257
        private const val STLS = 0x534c5453
        private const val VERSION = 0x01000000
        private const val MAX_DATA = 256 * 1024
        const val PORT = 5555

        fun connect(context: Context, socket: Socket, onPrompt: () -> Unit): LocalAdb {
            try {
                socket.connect(InetSocketAddress("127.0.0.1", PORT), 2500)
            } catch (e: IOException) {
                socket.close()
                throw Refused()
            }
            socket.soTimeout = 10_000
            val adb = LocalAdb(socket)
            try {
                adb.handshake(AdbKey.load(context), onPrompt)
            } catch (e: Exception) {
                adb.close()
                throw e
            }
            return adb
        }
    }

    private class Msg(val cmd: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    private fun send(cmd: Int, arg0: Int, arg1: Int, data: ByteArray = ByteArray(0)) {
        val head = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        head.putInt(cmd).putInt(arg0).putInt(arg1).putInt(data.size)
        head.putInt(data.sumOf { it.toInt() and 0xff })
        head.putInt(cmd.inv())
        output.write(head.array())
        output.write(data)
        output.flush()
    }

    private fun read(): Msg {
        val head = ByteArray(24)
        input.readFully(head)
        val b = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = b.int
        val arg0 = b.int
        val arg1 = b.int
        val len = b.int
        if (len < 0 || len > MAX_DATA * 4) throw IOException("bad length")
        val data = ByteArray(len)
        input.readFully(data)
        return Msg(cmd, arg0, arg1, data)
    }

    private fun handshake(key: AdbKey, onPrompt: () -> Unit) {
        send(CNXN, VERSION, MAX_DATA, "host::\u0000".toByteArray())
        var signed = false
        var sentKey = false
        while (true) {
            val m = read()
            when (m.cmd) {
                CNXN -> return
                STLS -> throw NeedsPairing()
                AUTH -> when {
                    m.arg0 != 1 -> {}
                    !signed -> { send(AUTH, 2, 0, key.sign(m.data)); signed = true }
                    !sentKey -> {
                        send(AUTH, 3, 0, key.publicPayload())
                        sentKey = true
                        socket.soTimeout = 65_000
                        onPrompt()
                    }
                    else -> throw Refused()
                }
            }
        }
    }

    @Synchronized
    fun shell(command: String): String {
        val local = nextId++
        send(OPEN, local, 0, "shell:$command\u0000".toByteArray())
        val out = ByteArrayOutputStream()
        socket.soTimeout = 60_000
        while (true) {
            val m = read()
            if (m.arg1 != local) continue
            when (m.cmd) {
                OKAY -> {}
                WRTE -> { out.write(m.data); send(OKAY, local, m.arg0) }
                CLSE -> { send(CLSE, local, m.arg0); return out.toString("UTF-8") }
            }
        }
    }

    override fun close() { runCatching { socket.close() } }
}
