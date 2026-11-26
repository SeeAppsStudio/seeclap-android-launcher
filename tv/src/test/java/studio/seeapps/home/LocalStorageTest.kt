package studio.seeapps.home

import android.content.ContextWrapper
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.security.KeyPairGenerator

class LocalStorageTest {
    @get:Rule val temp = TemporaryFolder()

    private val dirs by lazy {
        val files = temp.newFolder("files")
        val privateFiles = temp.newFolder("no_backup")
        val cache = temp.newFolder("cache")
        object : ContextWrapper(null) {
            override fun getFilesDir() = files
            override fun getNoBackupFilesDir() = privateFiles
            override fun getCacheDir() = cache
        }
    }

    @Test fun uninstallNeverOpensAndroidForEitherSeeClapEdition() {
        assertFalse(Apps.uninstall(dirs, SeeClap.PKG))
        assertFalse(Apps.uninstall(dirs, SeeClap.PKG_AMAZON))
    }

    @Test fun trimKeepsTheRecentArtworkAndLeavesActiveDownloadsAlone() {
        val img = File(dirs.cacheDir, "img").apply { mkdir() }
        fun make(name: String, mb: Int, stamp: Long) = File(img, name).apply {
            RandomAccessFile(this, "rw").use { it.setLength(mb * 1024L * 1024L) }
            assertTrue(setLastModified(stamp))
        }
        val old = make("old.img", 18, 1000)
        val current = make("current.img", 24, 2000)
        val download = make("active.part", 2, 500)
        Images.trim(dirs)
        assertTrue(current.exists())
        assertFalse(old.exists())
        assertTrue(download.exists())
    }

    @Test fun existingAdbIdentityMovesOutsideBackupsAndSurvivesReload() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
        File(dirs.filesDir, "adbkey").writeBytes(pair.private.encoded)
        File(dirs.filesDir, "adbkey.pub").writeBytes(pair.public.encoded)
        AdbKey.load(dirs)
        assertFalse(File(dirs.filesDir, "adbkey").exists())
        assertFalse(File(dirs.filesDir, "adbkey.pub").exists())
        assertArrayEquals(pair.private.encoded, File(dirs.noBackupFilesDir, "adbkey").readBytes())
        AdbKey.load(dirs)
        assertArrayEquals(pair.public.encoded, File(dirs.noBackupFilesDir, "adbkey.pub").readBytes())
    }
}
