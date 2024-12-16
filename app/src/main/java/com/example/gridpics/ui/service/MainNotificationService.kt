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
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
import com.example.gridpics.ui.activity.MainActivity.Companion.WAS_OPENED_SCREEN
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class MainNotificationService: Service()
{
	private val binder = NetworkServiceBinder()
	private var jobForCancelingNotification: Job? = null
	private var notificationCreationCounter = 0
	private lateinit var gridPics: String
	private lateinit var defaultText: String
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d("service", "onStartCommand()")
		prepareNotification()
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder
	{
		prepareNotification()
		Log.d("service", "onBind()")
		return binder
	}

	private fun createNotificationChannel()
	{
		val name = this@MainNotificationService.getString(R.string.my_notification_channel)
		val description = this@MainNotificationService.getString(R.string.channel_for_my_notification)

		if(Build.VERSION.SDK_INT >= VERSION_CODES.O)
		{
			val importance = if(notificationCreationCounter < 1)
			{
				NotificationManager.IMPORTANCE_MAX
			}
			else
			{
				NotificationManager.IMPORTANCE_DEFAULT
			}
			notificationCreationCounter++
			val channel = NotificationChannel(MainActivity.CHANNEL_NOTIFICATIONS_ID, name, importance)
			channel.description = description
			val notificationManager = this@MainNotificationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	override fun onRebind(intent: Intent?)
	{
		super.onRebind(intent)
		Log.d("service", "onRebind()")
		jobForCancelingNotification?.cancel()
	}

	private fun showNotification(builder: Builder)
	{
		val notificationManager = this@MainNotificationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(NOTIFICATION_ID, builder.build())
		Log.d("counterrrr", "$notificationCreationCounter")
		if(notificationCreationCounter == 1 || notificationCreationCounter == 0)
		{
			startForeground(NOTIFICATION_ID, builder.build())
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

	private fun createLogic(description: String, bitmap: Bitmap?)
	{
		val dontUseSound = notificationCreationCounter > 1
		val resultIntent = Intent(this@MainNotificationService, MainActivity::class.java)
		if(bitmap != null)
		{
			resultIntent.action = Intent.ACTION_SEND
			resultIntent.addCategory(Intent.CATEGORY_DEFAULT)
			resultIntent.putExtra(WAS_OPENED_SCREEN, description)
		}
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
		Log.d("intent URI", resultIntent.toUri(0))
		val resultPendingIntent = PendingIntent.getActivity(this@MainNotificationService, 0, resultIntent,
			PendingIntent.FLAG_IMMUTABLE)
		val color = getColor(R.color.green)
		val gridPics = this@MainNotificationService.gridPics
		val defaultText = this@MainNotificationService.defaultText
		if(description == DEFAULT_STRING_VALUE)
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
				.setLargeIcon(bitmap)
				.setStyle(NotificationCompat.BigPictureStyle()
					.bigPicture(bitmap)
					.bigLargeIcon(null as Icon?))
			showNotification(builder)
		}
	}

	fun putValues(valuesPair: Pair<String, Bitmap?>)
	{
		createLogic(valuesPair.first, valuesPair.second)
		notificationCreationCounter++
	}

	private fun prepareNotification()
	{
		gridPics = this@MainNotificationService.getString(R.string.gridpics)
		defaultText = this@MainNotificationService.getString(R.string.notification_content_text)
		createNotificationChannel()
		createLogic(DEFAULT_STRING_VALUE, null)
	}

	inner class NetworkServiceBinder: Binder()
	{
		fun get(): MainNotificationService = this@MainNotificationService
	}
}