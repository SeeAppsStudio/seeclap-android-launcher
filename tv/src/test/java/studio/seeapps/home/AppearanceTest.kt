package studio.seeapps.home

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceTest {
    private val day = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_NO
    private val night = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_YES

    @Test fun automaticFollowsTheTvAndDefaultsToLight() {
        assertEquals(true, Appearance.isDay("auto", day))
        assertEquals(false, Appearance.isDay("auto", night))
        assertEquals(true, Appearance.isDay("auto", Configuration.UI_MODE_TYPE_TELEVISION))
        assertEquals(true, Appearance.isDay("unknown", day))
    }

    @Test fun manualThemeOverridesTheSystem() {
        assertEquals(true, Appearance.isDay("light", night))
        assertEquals(false, Appearance.isDay("dark", day))
    }

    @Test fun ambientBackgroundFollowsTheResolvedTheme() {
        assertEquals(R.drawable.wall_ambient_night, Appearance.ambientResource(Appearance.isDay("auto", night)))
        assertEquals(R.drawable.wall_ambient_day, Appearance.ambientResource(Appearance.isDay("light", night)))
    }
}
