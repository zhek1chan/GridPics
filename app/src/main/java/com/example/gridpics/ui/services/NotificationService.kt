package com.example.gridpics.ui.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gridpics.App.Companion.activityIsDestroyed
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID

class NotificationService: Service()
{
	private val binder = NetworkServiceBinder()
	private var subscriberCount = 0
	override fun onCreate()
	{
		Log.d("service", "service onCreate")
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

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d("service", "service onStartCommand")
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder
	{
		subscriberCount += 1
		Log.d("service", "subs $subscriberCount")
		Log.d("service", "service onBind")
		return binder
	}

	override fun onUnbind(intent: Intent?): Boolean
	{
		subscriberCount -= 1
		Log.d("service", "subs $subscriberCount")
		Log.d("service", "service onUnbind")
		if(subscriberCount == 0)
		{
			startTimer(1000L, 1000L) {
				Log.d("service", "times has gone")
			}
		}
		return super.onUnbind(intent)
	}

	private fun startTimer(millisInFuture: Long, countDownInterval: Long, onFinish: () -> Unit)
	{
		object: CountDownTimer(millisInFuture, countDownInterval)
		{
			override fun onTick(millisUntilFinished: Long)
			{
				// Здесь можно выполнять действия, которые нужно выполнять во время отсчета
				// Например, обновлять пользовательский интерфейс
			}

			override fun onFinish()
			{
				val manager: NotificationManager = getSystemService(NotificationManager::class.java)
				if(subscriberCount == 0)
				{
					manager.cancel(NOTIFICATION_ID)
				}
				onFinish()
			}
		}.start()
	}

	override fun onDestroy()
	{
		super.onDestroy()
		if(activityIsDestroyed)
		{
			startTimer(0L, 0L) {}
		}
	}

	inner class NetworkServiceBinder: Binder()
	{
		fun get(): NotificationService = this@NotificationService
	}
}