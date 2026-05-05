package org.koharu.miyo.core.prefs

import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.koharu.miyo.R
import org.koitharu.kotatsu.parsers.util.find

@Keep
enum class ColorScheme(
	@StyleRes val styleResId: Int,
	@StringRes val titleResId: Int,
) {

	NIGHT_MODE(R.style.ThemeOverlay_Miyo_NightMode, R.string.theme_name_night_mode),
	FOREST_GREEN(R.style.ThemeOverlay_Miyo_ForestGreen, R.string.theme_name_forest_green),
	LAVENDER_DREAM(R.style.ThemeOverlay_Miyo_LavenderDream, R.string.theme_name_lavender_dream),
	MIDNIGHT_OLED(R.style.ThemeOverlay_Miyo_MidnightOled, R.string.theme_name_midnight_oled),
	PARCHMENT(R.style.ThemeOverlay_Miyo_Parchment, R.string.theme_name_parchment),
	MONOCHROME(R.style.ThemeOverlay_Miyo_Monochrome, R.string.theme_name_monochrome),
	OCEAN_BLUE(R.style.ThemeOverlay_Miyo_OceanBlue, R.string.theme_name_ocean_blue),
	WARM_SUNSET(R.style.ThemeOverlay_Miyo_WarmSunset, R.string.theme_name_warm_sunset),
	NORDIC_NIGHT(R.style.ThemeOverlay_Miyo_NordicNight, R.string.theme_name_nordic_night),
	PEACH_BLOSSOM(R.style.ThemeOverlay_Miyo_PeachBlossom, R.string.theme_name_peach_blossom),
	DARK_COFFEE(R.style.ThemeOverlay_Miyo_DarkCoffee, R.string.theme_name_dark_coffee),
	MATCHA_PAPER(R.style.ThemeOverlay_Miyo_MatchaPaper, R.string.theme_name_matcha_paper),
	INK_STONE(R.style.ThemeOverlay_Miyo_InkStone, R.string.theme_name_ink_stone),
	BLUEPRINT_DAY(R.style.ThemeOverlay_Miyo_BlueprintDay, R.string.theme_name_blueprint_day),
	EMBER_NIGHT(R.style.ThemeOverlay_Miyo_EmberNight, R.string.theme_name_ember_night),
	;

	companion object {

		val default: ColorScheme
			get() = OCEAN_BLUE

		fun getAvailableList(): List<ColorScheme> {
			return ColorScheme.entries
		}

		fun safeValueOf(name: String): ColorScheme? {
			return ColorScheme.entries.find(name)
		}
	}
}
