package studio.seeapps.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateCacheTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun updatingOneAppKeepsTheOtherAppsPendingDownload() {
        val old = temp.newFile("studio.seeapps.home-10.apk")
        val current = temp.newFile("studio.seeapps.home-1.apk")
        val pending = temp.newFile("com.seeappsstudio.seeclap-163.apk")

        assertEquals(current, updateCacheFile(temp.root, "studio.seeapps.home", 1))
        assertTrue(current.exists())
        assertTrue(pending.exists())
        assertFalse(old.exists())
    }
}
