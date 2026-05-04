package org.koharu.miyo.settings

import android.os.Bundle
import androidx.preference.Preference
import leakcanary.LeakCanary
import org.koharu.miyo.R
import org.koharu.miyo.core.model.TestMangaSource
import org.koharu.miyo.core.nav.router
import org.koharu.miyo.core.ui.BasePreferenceFragment
import org.koharu.miyo.settings.utils.SplitSwitchPreference
import org.koitharu.workinspector.WorkInspector

class DebugSettingsFragment : BasePreferenceFragment(R.string.debug), Preference.OnPreferenceChangeListener,
	Preference.OnPreferenceClickListener {

	private var isLeakCanaryEnabled: Boolean
		get() = LeakCanary.config.dumpHeap
		set(value) {
			LeakCanary.config = LeakCanary.config.copy(dumpHeap = value)
		}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.pref_debug)
		findPreference<SplitSwitchPreference>(KEY_LEAK_CANARY)?.let { pref ->
			pref.isChecked = isLeakCanaryEnabled
			pref.onPreferenceChangeListener = this
			pref.onContainerClickListener = this
		}
	}

	override fun onResume() {
		super.onResume()
		findPreference<SplitSwitchPreference>(KEY_LEAK_CANARY)?.isChecked = isLeakCanaryEnabled
	}

	override fun onPreferenceTreeClick(preference: Preference): Boolean = when (preference.key) {
		KEY_WORK_INSPECTOR -> {
			startActivity(WorkInspector.getIntent(preference.context))
			true
		}

		KEY_TEST_PARSER -> {
			router.openList(TestMangaSource, null, null)
			true
		}

		else -> super.onPreferenceTreeClick(preference)
	}

	override fun onPreferenceClick(preference: Preference): Boolean = when (preference.key) {
		KEY_LEAK_CANARY -> {
			startActivity(LeakCanary.newLeakDisplayActivityIntent())
			true
		}

		else -> super.onPreferenceTreeClick(preference)
	}

	override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean = when (preference.key) {
		KEY_LEAK_CANARY -> {
			isLeakCanaryEnabled = newValue as Boolean
			true
		}

		else -> false
	}

	private companion object {

		const val KEY_LEAK_CANARY = "leak_canary"
		const val KEY_WORK_INSPECTOR = "work_inspector"
		const val KEY_TEST_PARSER = "test_parser"
	}
}
