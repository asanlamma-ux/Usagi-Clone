package org.koharu.miyo.widget.recent

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import org.koharu.miyo.R
import org.koharu.miyo.core.nav.ReaderIntent
import org.koharu.miyo.core.prefs.AppWidgetConfig
import org.koharu.miyo.core.ui.BaseAppWidgetProvider
import org.koharu.miyo.reader.ui.ReaderActivity

class RecentWidgetProvider : BaseAppWidgetProvider() {

	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		super.onUpdate(context, appWidgetManager, appWidgetIds)
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.stackView)
	}

	override fun onUpdateWidget(context: Context, config: AppWidgetConfig): RemoteViews {
		val views = RemoteViews(context.packageName, R.layout.widget_recent)
		if (!config.hasBackground) {
			views.setInt(R.id.widget_root, "setBackgroundColor", Color.TRANSPARENT)
		} else {
			views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_appwidget_root)
		}
		val adapter = Intent(context, RecentWidgetService::class.java)
		adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
		adapter.data = adapter.toUri(Intent.URI_INTENT_SCHEME).toUri()
		views.setRemoteAdapter(R.id.stackView, adapter)
		val intent = Intent(context, ReaderActivity::class.java)
		intent.action = ReaderIntent.ACTION_MANGA_READ
		views.setPendingIntentTemplate(
			R.id.stackView,
			PendingIntentCompat.getActivity(
				context,
				0,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT,
				true,
			),
		)
		views.setEmptyView(R.id.stackView, R.id.textView_holder)
		return views
	}
}
