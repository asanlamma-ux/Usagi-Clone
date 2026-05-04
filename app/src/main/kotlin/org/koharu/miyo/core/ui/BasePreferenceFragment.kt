package org.koharu.miyo.core.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.preference.MultiSelectListPreference
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.get
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import org.koharu.miyo.R
import org.koharu.miyo.core.exceptions.resolve.ExceptionResolver
import org.koharu.miyo.core.prefs.AppSettings
import org.koharu.miyo.core.ui.util.RecyclerViewOwner
import org.koharu.miyo.core.util.ext.consumeAllSystemBarsInsets
import org.koharu.miyo.core.util.ext.container
import org.koharu.miyo.core.util.ext.end
import org.koharu.miyo.core.util.ext.getThemeColor
import org.koharu.miyo.core.util.ext.getThemeDrawable
import org.koharu.miyo.core.util.ext.parentView
import org.koharu.miyo.core.util.ext.start
import org.koharu.miyo.core.util.ext.systemBarsInsets
import org.koharu.miyo.core.util.ext.withArgs
import org.koharu.miyo.settings.SettingsActivity
import javax.inject.Inject
import com.google.android.material.R as materialR

@AndroidEntryPoint
abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(),
	OnApplyWindowInsetsListener,
	RecyclerViewOwner {

	protected lateinit var exceptionResolver: ExceptionResolver
		private set

	@Inject
	lateinit var settings: AppSettings

	override val recyclerView: RecyclerView?
		get() = listView

	override fun onAttach(context: Context) {
		super.onAttach(context)
		val entryPoint = EntryPointAccessors.fromApplication<BaseActivityEntryPoint>(context)
		exceptionResolver = entryPoint.exceptionResolverFactory.create(this)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		ViewCompat.setOnApplyWindowInsetsListener(view, this)
		val themedContext = (view.parentView ?: view).context
		view.setBackgroundColor(themedContext.getThemeColor(android.R.attr.colorBackground))
		listView.clipToPadding = false
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		val isTablet = !resources.getBoolean(R.bool.is_tablet)
		val isMaster = container?.id == R.id.container_master
		listView.setPaddingRelative(
			if (isTablet && !isMaster) 0 else barsInsets.start(v),
			0,
			if (isTablet && isMaster) 0 else barsInsets.end(v),
			barsInsets.bottom,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onResume() {
		super.onResume()
		setTitle(if (titleId != 0) getString(titleId) else null)
		arguments?.getString(SettingsActivity.ARG_PREF_KEY)?.let {
			focusPreference(it)
			arguments?.remove(SettingsActivity.ARG_PREF_KEY)
		}
	}

	override fun onDisplayPreferenceDialog(preference: Preference) {
		val dialog = when (preference) {
			is ListPreference -> MaterialListPreferenceDialogFragment.newInstance(preference.key)
			is MultiSelectListPreference -> MaterialMultiSelectListPreferenceDialogFragment.newInstance(preference.key)
			else -> null
		}
		if (dialog == null) {
			super.onDisplayPreferenceDialog(preference)
			return
		}
		if (parentFragmentManager.findFragmentByTag(PREFERENCE_DIALOG_TAG) != null) {
			return
		}
		@Suppress("DEPRECATION")
		dialog.setTargetFragment(this, 0)
		dialog.show(parentFragmentManager, PREFERENCE_DIALOG_TAG)
	}

	protected open fun setTitle(title: CharSequence?) {
		(activity as? SettingsActivity)?.setSectionTitle(title)
	}

	protected fun getWarningIcon(): Drawable? = context?.let { ctx ->
		ContextCompat.getDrawable(ctx, R.drawable.ic_alert_outline)?.also {
			it.setTint(ContextCompat.getColor(ctx, R.color.warning))
		}
	}

	private fun focusPreference(key: String) {
		val pref = findPreference<Preference>(key)
		if (pref == null) {
			scrollToPreference(key)
			return
		}
		scrollToPreference(pref)
		val prefIndex = preferenceScreen.indexOf(key)
		val view = if (prefIndex >= 0) {
			listView.findViewHolderForAdapterPosition(prefIndex)?.itemView ?: return
		} else {
			return
		}
		view.context.getThemeDrawable(materialR.attr.colorTertiaryContainer)?.let {
			view.background = it
		}
	}

	private fun PreferenceScreen.indexOf(key: String): Int {
		for (i in 0 until preferenceCount) {
			if (get(i).key == key) {
				return i
			}
		}
		return -1
	}

	class MaterialListPreferenceDialogFragment : ListPreferenceDialogFragmentCompat() {

		override fun onStart() {
			super.onStart()
			dialog?.window?.setBackgroundDrawable(
				ContextCompat.getDrawable(requireContext(), R.drawable.m3_popup_background),
			)
		}

		companion object {

			fun newInstance(key: String) = MaterialListPreferenceDialogFragment().withArgs(1) {
				putString(ARG_KEY, key)
			}
		}
	}

	class MaterialMultiSelectListPreferenceDialogFragment : MultiSelectListPreferenceDialogFragmentCompat() {

		override fun onStart() {
			super.onStart()
			dialog?.window?.setBackgroundDrawable(
				ContextCompat.getDrawable(requireContext(), R.drawable.m3_popup_background),
			)
		}

		companion object {

			fun newInstance(key: String) = MaterialMultiSelectListPreferenceDialogFragment().withArgs(1) {
				putString(ARG_KEY, key)
			}
		}
	}

	companion object {

		private const val PREFERENCE_DIALOG_TAG = "androidx.preference.PreferenceFragment.DIALOG"
	}
}
