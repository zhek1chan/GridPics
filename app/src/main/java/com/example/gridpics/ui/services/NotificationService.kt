package com.example.gridpics.ui.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
import javax.annotation.Nullable

class NotificationService: Service()
{
	override fun onCreate()
	{
		super.onCreate()
		createNotificationChannel()
		showNotification()
	}

	private fun createNotificationChannel()
	{
		val name = getString(R.string.my_notification_channel)
		val description = getString(R.string.channel_for_my_notification)

		if(Build.VERSION.SDK_INT >= VERSION_CODES.O)
		{
			val importance = NotificationManager.IMPORTANCE_HIGH
			val channel = NotificationChannel(MainActivity.CHANNEL_ID, name, importance)
			channel.description = description
			val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	private fun showNotification()
	{
		// Создаем уведомление
		val builder = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.setContentIntent(null)
			.setAutoCancel(true)
			.setOngoing(true)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setColor(getColor(R.color.green))
			.setContentTitle(getString(R.string.gridpics))
			.setContentText(getString(R.string.notification_content_text))
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(NOTIFICATION_ID, builder.build())
	}

	override fun onDestroy()
	{
		super.onDestroy()
		val manager: NotificationManager = getSystemService(NotificationManager::class.java)
		manager.cancel(NOTIFICATION_ID)
	}

	@Nullable
	override fun onBind(intent: Intent?): IBinder?
	{
		return null
	}
}