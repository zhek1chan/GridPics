package com.example.gridpics.ui.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
import com.example.gridpics.ui.activity.MainActivity.Companion.DETAILS
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
import com.example.gridpics.ui.activity.MainActivity.Companion.WAS_OPENED_SCREEN
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class MainNotificationService: Service()
{
	private val binder = NetworkServiceBinder()
	private var jobForCancelingNotification: Job? = null
	private var jobToCreateNotification: Job? = null
	private var count = 0
	private lateinit var gridPics: String
	private lateinit var defaultText: String
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d("service", "onStartCommand()")
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder
	{
		gridPics = this@MainNotificationService.getString(R.string.gridpics)
		defaultText = this@MainNotificationService.getString(R.string.notification_content_text)
		Log.d("service", "onBind()")
		createNotificationChannel()
		createLogic(Pair(DEFAULT_STRING_VALUE, null))
		return binder
	}

	private fun createNotificationChannel()
	{
		val name = this@MainNotificationService.getString(R.string.my_notification_channel)
		val description = this@MainNotificationService.getString(R.string.channel_for_my_notification)

		if(Build.VERSION.SDK_INT >= VERSION_CODES.O)
		{
			val importance = if(count < 1)
			{
				NotificationManager.IMPORTANCE_MAX
			}
			else
			{
				NotificationManager.IMPORTANCE_DEFAULT
			}
			count++
			val channel = NotificationChannel(MainActivity.CHANNEL_NOTIFICATIONS_ID, name, importance)
			channel.description = description
			val notificationManager = this@MainNotificationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	override fun onRebind(intent: Intent?)
	{
		Log.d("service", "onRebind()")
		jobForCancelingNotification?.cancel()
		super.onRebind(intent)
	}

	private fun showNotification(builder: Builder)
	{
		jobToCreateNotification?.let {
			GlobalScope.launch(it) {
				val notificationManager = this@MainNotificationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
				notificationManager.notify(NOTIFICATION_ID, builder.build())
				Log.d("counterrrr", "$count")
				if(count == 1 || count == 0)
				{
					startForeground(NOTIFICATION_ID, builder.build())
				}
			}
		}
	}

	override fun onUnbind(intent: Intent?): Boolean
	{
		Log.d("service", "onUnbind()")
		stopNotificationCoroutine()
		return true
	}

	private fun stopNotificationCoroutine()
	{
		jobForCancelingNotification = GlobalScope.launch {
			Log.d("service", "stopNotificationCoroutine has been started")
			delay(2000)
			stopSelf()
			Log.d("service", "service was stopped")
		}
	}

	private fun createLogic(values: Pair<String, Bitmap?>)
	{
		jobToCreateNotification = GlobalScope.launch {
			Log.d("description", "${this.coroutineContext}")
			val dontUseSound = count > 1
			val resultIntent = Intent(this@MainNotificationService, MainActivity::class.java)
			if(values.second != null)
			{
				resultIntent.putExtra(WAS_OPENED_SCREEN, DETAILS)
				resultIntent.setAction(values.first)
			}
			resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
			Log.d("intent URI", resultIntent.toUri(0))
			val resultPendingIntent = PendingIntent.getActivity(this@MainNotificationService, 0, resultIntent,
				PendingIntent.FLAG_IMMUTABLE)
			val contentText: String
			val color = getColor(R.color.green)
			val gridPics = this@MainNotificationService.gridPics
			val defaultText = this@MainNotificationService.defaultText
			if(values.first == DEFAULT_STRING_VALUE)
			{
				Log.d("description in service", DEFAULT_STRING_VALUE)
				val builder = Builder(this@MainNotificationService, MainActivity.CHANNEL_NOTIFICATIONS_ID)
					.setContentIntent(resultPendingIntent)
					.setAutoCancel(true)
					.setOngoing(true)
					.setSilent(dontUseSound)
					.setSmallIcon(R.mipmap.ic_launcher)
					.setColor(color)
					.setContentTitle(gridPics)
					.setContentText(defaultText)
				showNotification(builder)
			}
			else
			{
				contentText = values.first
				Log.d("wtfWtf", contentText)
				val description = values.first
				val stringImage = values.second
				Log.d("description in service", description)
				val builder = Builder(this@MainNotificationService, MainActivity.CHANNEL_NOTIFICATIONS_ID)
					.setContentIntent(resultPendingIntent)
					.setAutoCancel(true)
					.setOngoing(true)
					.setSilent(dontUseSound)
					.setSmallIcon(R.mipmap.ic_launcher)
					.setColor(color)
					.setContentTitle(gridPics)
					.setContentText(description)
					.setLargeIcon(stringImage)
					.setStyle(NotificationCompat.BigPictureStyle()
						.bigPicture(stringImage)
						.bigLargeIcon(null as Icon?))
				showNotification(builder)
			}
		}
	}

	fun putValues(valuesPair: Pair<String, Bitmap?>)
	{
		jobToCreateNotification?.cancelChildren()
		createLogic(valuesPair)
		count++
	}

	inner class NetworkServiceBinder: Binder()
	{
		fun get(): MainNotificationService = this@MainNotificationService
	}
}