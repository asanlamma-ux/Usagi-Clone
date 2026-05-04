package org.koharu.miyo.download.ui.worker

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.Dispatchers
import org.koharu.miyo.R
import org.koharu.miyo.core.util.ext.goAsync
import org.koharu.miyo.core.util.ext.toUUIDOrNull
import org.koharu.miyo.download.ui.list.DownloadsActivity
import java.util.UUID

class DownloadCancelReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent?) {
		if (intent?.action != ACTION_CANCEL) {
			return
		}
		val id = intent.getStringExtra(EXTRA_UUID)?.toUUIDOrNull()
			?: intent.data?.host?.toUUIDOrNull()
			?: return
		val isSilent = intent.getBooleanExtra(EXTRA_SILENT, false)
		val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
		notificationManager.notify(
			id.hashCode(),
			createStatusNotification(
				context = context,
				isSilent = isSilent,
				text = context.getString(R.string.download_canceling),
				isOngoing = true,
			),
		)
		goAsync(Dispatchers.Default) {
			WorkManager.getInstance(context).cancelWorkById(id).await()
			notificationManager.notify(
				id.hashCode(),
				createStatusNotification(
					context = context,
					isSilent = isSilent,
					text = context.getString(R.string.canceled),
					isOngoing = false,
				),
			)
		}
	}

	companion object {

		private const val ACTION_CANCEL = "org.koharu.miyo.download.CANCEL"
		private const val EXTRA_UUID = "uuid"
		private const val EXTRA_SILENT = "silent"
		private const val SCHEME = "workuid"
		private const val CHANNEL_ID_DEFAULT = "download"
		private const val CHANNEL_ID_SILENT = "download_bg"

		fun createPendingIntent(context: Context, id: UUID, isSilent: Boolean) = PendingIntentCompat.getBroadcast(
			context,
			id.hashCode(),
			createIntent(context, id, isSilent),
			PendingIntent.FLAG_UPDATE_CURRENT,
			false,
		)

		private fun createIntent(context: Context, id: UUID, isSilent: Boolean) =
			Intent(context, DownloadCancelReceiver::class.java)
				.setAction(ACTION_CANCEL)
				.setData("$SCHEME://$id".toUri())
				.setPackage(context.packageName)
				.putExtra(EXTRA_UUID, id.toString())
				.putExtra(EXTRA_SILENT, isSilent)

		private fun createStatusNotification(
			context: Context,
			isSilent: Boolean,
			text: String,
			isOngoing: Boolean,
		): Notification {
			val queueIntent = PendingIntentCompat.getActivity(
				context,
				0,
				Intent(context, DownloadsActivity::class.java),
				0,
				false,
			)
			return NotificationCompat.Builder(context, if (isSilent) CHANNEL_ID_SILENT else CHANNEL_ID_DEFAULT)
				.setContentTitle(context.getString(R.string.manga_downloading_))
				.setContentText(text)
				.setContentIntent(queueIntent)
				.setSmallIcon(R.drawable.ic_stat_paused)
				.setProgress(0, 0, isOngoing)
				.setOngoing(isOngoing)
				.setAutoCancel(!isOngoing)
				.setOnlyAlertOnce(true)
				.setSilent(true)
				.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				.build()
		}
	}
}
