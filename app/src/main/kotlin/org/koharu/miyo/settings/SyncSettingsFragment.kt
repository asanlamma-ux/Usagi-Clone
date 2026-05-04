package org.koharu.miyo.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentResultListener
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.koharu.miyo.R
import org.koharu.miyo.core.ui.BasePreferenceFragment
import org.koharu.miyo.sync.data.SyncSettings
import org.koharu.miyo.sync.ui.SyncHostDialogFragment
import javax.inject.Inject

@AndroidEntryPoint
class SyncSettingsFragment : BasePreferenceFragment(R.string.sync_settings), FragmentResultListener {

	@Inject
	lateinit var syncSettings: SyncSettings

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_sync)
		bindHostSummary()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		childFragmentManager.setFragmentResultListener(SyncHostDialogFragment.REQUEST_KEY, viewLifecycleOwner, this)
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean {
		return when (preference.key) {
			SyncSettings.KEY_SYNC_URL -> {
				SyncHostDialogFragment.show(childFragmentManager, null)
				true
			}

			else -> super.onPreferenceTreeClick(preference)
		}
	}

	override fun onFragmentResult(requestKey: String, result: Bundle) {
		bindHostSummary()
	}

	private fun bindHostSummary() {
		val preference = findPreference<Preference>(SyncSettings.KEY_SYNC_URL) ?: return
		preference.summary = syncSettings.syncUrl
	}
}
