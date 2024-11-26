package com.example.gridpics.ui.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import com.example.gridpics.App.Companion.instance
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
import com.example.gridpics.ui.activity.MainActivity.Companion.countExitNavigation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainNotificationService: Service()
{
	private val binder = NetworkServiceBinder()
	private val jobForNotification = Job()
	private lateinit var contentText: String
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d("service", "onStartCommand()")
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder
	{
		Log.d("service", "onBind()")
		return binder
	}

	private fun createNotificationChannel()
	{
		val name = this@MainNotificationService.getString(R.string.my_notification_channel)
		val description = this@MainNotificationService.getString(R.string.channel_for_my_notification)

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
			val channel = NotificationChannel(MainActivity.CHANNEL_NOTIFICATIONS_ID, name, importance)
			channel.description = description
			val notificationManager = this@MainNotificationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	override fun onRebind(intent: Intent?)
	{
		Log.d("service", "onRebind()")
		jobForNotification.cancelChildren()
		super.onRebind(intent)
	}

	private fun showNotification(builder: Builder)
	{
		val notificationManager = this@MainNotificationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(NOTIFICATION_ID, builder.build())
		startForeground(NOTIFICATION_ID, builder.build())
	}

	override fun onUnbind(intent: Intent?): Boolean
	{
		Log.d("service", "onUnbind()")
		stopNotificationCoroutine()
		return true
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun stopNotificationCoroutine()
	{
		GlobalScope.launch(Dispatchers.IO + jobForNotification) {
			Log.d("service", "stopNotificationCoroutine has been started")
			delay(2000)
			val notificationManager = this@MainNotificationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.cancelAll()
			stopSelf()
			Log.d("service", "service was stopped")
		}
	}

	private fun createLogic(values: Pair<String, String?>)
	{
		val dontUseSound = countExitNavigation > 1
		val resultIntent = Intent(instance, MainActivity::class.java)
		val resultPendingIntent = PendingIntent.getActivity(instance, 0, resultIntent,
			PendingIntent.FLAG_IMMUTABLE)
		if(values.first == DEFAULT_STRING_VALUE)
		{
			contentText = getString(R.string.notification_content_text)
			val builder = Builder(this@MainNotificationService, MainActivity.CHANNEL_NOTIFICATIONS_ID)
				.setContentIntent(resultPendingIntent)
				.setAutoCancel(true)
				.setOngoing(true)
				.setSilent(dontUseSound)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setColor(getColor(R.color.green))
				.setContentTitle(this@MainNotificationService.getString(R.string.gridpics))
				.setContentText(this@MainNotificationService.getString(R.string.notification_content_text))
			createNotificationChannel()
			showNotification(builder)
		}
		else
		{
			contentText = values.toList().toString()
			Log.d("wtfWtf", contentText)
			val words = contentText.substring(1, contentText.length - 1).split(",")
			val description = words[0].trim()
			val stringImage = words[1].trim()
			Log.d("description in service", description)
			Log.d("intent", "we got $contentText")
			Log.d("wtf", stringImage)
			val decoded = Base64.decode(stringImage, 0)
			val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
			Log.d("wtf", "$bitmap")
			val builder = Builder(this@MainNotificationService, MainActivity.CHANNEL_NOTIFICATIONS_ID)
				.setContentIntent(resultPendingIntent)
				.setAutoCancel(true)
				.setOngoing(true)
				.setSilent(dontUseSound)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setColor(this@MainNotificationService.resources.getColor(R.color.green, theme))
				.setContentTitle(this@MainNotificationService.getString(R.string.gridpics))
				.setContentText(description)
				.setLargeIcon(bitmap)
				.setStyle(NotificationCompat.BigPictureStyle()
					.bigPicture(bitmap)
					.bigLargeIcon(null as Icon?))
			createNotificationChannel()
			showNotification(builder)
		}
	}

	override fun startForegroundService(service: Intent?): ComponentName?
	{
		return super.startForegroundService(service)
	}

	fun putValues(valuesPair: Pair<String, String?>)
	{
		createLogic(valuesPair)
	}

	inner class NetworkServiceBinder: Binder()
	{
		fun get(): MainNotificationService = this@MainNotificationService
	}
}