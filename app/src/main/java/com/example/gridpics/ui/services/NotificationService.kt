package com.example.gridpics.ui.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
import com.example.gridpics.ui.activity.MainActivity.Companion.countExitNavigation
import com.example.gridpics.ui.activity.MainActivity.Companion.jobForNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class NotificationService: Service()
{
	private val binder = NetworkServiceBinder()
	private var isActive = true
	private var job = jobForNotifications
	private var scope = CoroutineScope(Dispatchers.IO + job)
	override fun onCreate()
	{
		Log.d("service", "service onCreate")
		super.onCreate()
	}

	private fun createNotificationChannel()
	{
		val name = getString(R.string.my_notification_channel)
		val description = getString(R.string.channel_for_my_notification)

		if(Build.VERSION.SDK_INT >= VERSION_CODES.O)
		{
			val importance = if(countExitNavigation < 1)
			{
				NotificationManager.IMPORTANCE_MAX
			}
			else
			{
				NotificationManager.IMPORTANCE_DEFAULT
			}
			val channel = NotificationChannel(MainActivity.CHANNEL_ID, name, importance)
			channel.description = description
			val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	@SuppressLint("ForegroundServiceType")
	private fun showNotification()
	{
		// Создаем уведомление
		val builder = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
			.setContentIntent(null)
			.setAutoCancel(true)
			.setOngoing(true)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setColor(getColor(R.color.green))
			.setContentTitle(getString(R.string.gridpics))
			.setContentText(getString(R.string.notification_content_text))
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(NOTIFICATION_ID, builder.build())
		startForeground(NOTIFICATION_ID, builder.build())
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		isActive = true
		job.cancelChildren()
		createNotificationChannel()
		showNotification()
		Log.d("service", "service onStartCommand")
		scope.launch {
			Log.d("service", "service is alive")
		}
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder
	{
		return binder
	}

	override fun stopService(name: Intent?): Boolean
	{
		isActive = false
		Log.d("service", "service onStop")
		return super.stopService(name)
	}

	override fun onDestroy()
	{
		Log.d("service", "service onDestroy")
		super.onDestroy()
	}

	override fun onTaskRemoved(rootIntent: Intent?)
	{
		stopSelf()
		super.onTaskRemoved(rootIntent)
	}

	inner class NetworkServiceBinder: Binder()
	{
		fun get(): NotificationService = this@NotificationService
	}
}