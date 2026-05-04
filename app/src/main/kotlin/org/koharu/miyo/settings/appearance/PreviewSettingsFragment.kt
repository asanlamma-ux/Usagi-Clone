package org.koharu.miyo.settings.appearance

import android.os.Bundle
import androidx.preference.ListPreference
import dagger.hilt.android.AndroidEntryPoint
import org.koharu.miyo.R
import org.koharu.miyo.core.prefs.AppSettings
import org.koharu.miyo.core.prefs.DetailsUiMode
import org.koharu.miyo.core.ui.BasePreferenceFragment
import org.koharu.miyo.core.util.ext.setDefaultValueCompat
import org.koitharu.kotatsu.parsers.util.names
import org.koharu.miyo.settings.utils.PercentSummaryProvider
import org.koharu.miyo.settings.utils.SliderPreference

@AndroidEntryPoint
class PreviewSettingsFragment :
    BasePreferenceFragment(R.string.details_appearance) {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_details_appearance)

        findPreference<ListPreference>(AppSettings.KEY_DETAILS_UI)?.run {
            entryValues = DetailsUiMode.entries.names()
            setDefaultValueCompat(DetailsUiMode.MODERN.name)
        }

        findPreference<SliderPreference>(AppSettings.KEY_DETAILS_BACKDROP_BLUR_AMOUNT)
            ?.summaryProvider = PercentSummaryProvider()
    }
}
