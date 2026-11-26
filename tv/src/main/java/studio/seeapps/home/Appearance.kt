package studio.seeapps.home

import android.content.res.Configuration

internal object Appearance {
    fun isDay(theme: String, uiMode: Int): Boolean = when (theme) {
        "light" -> true
        "dark" -> false
        else -> (uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
    }

    fun themeLabel(mode: String) = when (mode) {
        "light" -> R.string.theme_light
        "dark" -> R.string.theme_dark
        else -> R.string.theme_auto
    }

    fun ambientResource(day: Boolean) =
        if (day) R.drawable.wall_ambient_day else R.drawable.wall_ambient_night
}
