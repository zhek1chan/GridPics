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
import com.example.gridpics.ui.activity.MainActivity.Companion.CHANNEL_NOTIFICATIONS_ID
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
import com.example.gridpics.ui.activity.MainActivity.Companion.SAVED_URL_FROM_SCREEN_DETAILS
import com.example.gridpics.ui.activity.MainActivity.Companion.TEXT_PLAIN
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainNotificationService: Service()
{
	private val binder = ServiceBinder()
	private var jobForCancelingNotification: Job? = null
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d("service", "onStartCommand()")
		prepareNotification(false)
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder
	{
		prepareNotification(true)
		Log.d("service", "onBind()")
		return binder
	}

	private fun createNotificationChannel()
	{
		if(Build.VERSION.SDK_INT >= VERSION_CODES.R)
		{
			val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			if(notificationManager.getNotificationChannel(CHANNEL_NOTIFICATIONS_ID) == null)
			{
				val name = this.getString(R.string.my_notification_channel)
				val description = this.getString(R.string.channel_for_my_notification)
				val channel = NotificationChannel(CHANNEL_NOTIFICATIONS_ID, name, NotificationManager.IMPORTANCE_HIGH)
				channel.description = description
				notificationManager.createNotificationChannel(channel)
			}
		}
	}

	override fun onRebind(intent: Intent?)
	{
		super.onRebind(intent)
		Log.d("service", "onRebind()")
		jobForCancelingNotification?.cancel()
	}

	private fun showNotification(builder: Builder, launchServiceInBackground: Boolean)
	{
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(NOTIFICATION_ID, builder.build())
		if(!launchServiceInBackground)
		{
			Log.d("started foreground", "true")
			startForeground(NOTIFICATION_ID, builder.build())
		}
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
		jobForCancelingNotification = GlobalScope.launch {
			Log.d("service", "stopNotificationCoroutine has been started")
			delay(2000)
			stopSelf()
			Log.d("service", "service was stopped")
		}
	}

	private fun createLogic(description: String?, bitmap: Bitmap?, useSound: Boolean)
	{
		val resultIntent = Intent(this@MainNotificationService, MainActivity::class.java)
		if(bitmap != null)
		{
			Log.d("OldString put", description.toString())
			resultIntent.action = Intent.ACTION_SEND
			resultIntent.addCategory(Intent.CATEGORY_DEFAULT)
			resultIntent.setType(TEXT_PLAIN)
			resultIntent.putExtra(SAVED_URL_FROM_SCREEN_DETAILS, description)
		}
		val resultPendingIntent = PendingIntent.getActivity(this@MainNotificationService, 100, resultIntent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)
		val color = getColor(R.color.green)
		val gridPics = this@MainNotificationService.getString(R.string.gridpics)
		val defaultText = description ?: this@MainNotificationService.getString(R.string.notification_content_text)
		val builder = Builder(this@MainNotificationService, CHANNEL_NOTIFICATIONS_ID)
			.setContentIntent(resultPendingIntent)
			.setAutoCancel(true)
			.setOngoing(true)
			.setSilent(!useSound)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setColor(color)
			.setContentTitle(gridPics)
			.setContentText(defaultText)
		Log.d("description in service", description.toString())
		if(bitmap != null)
		{
			builder.setLargeIcon(bitmap)
				.setStyle(NotificationCompat.BigPictureStyle()
					.bigPicture(bitmap)
					.bigLargeIcon(null as Icon?))
		}
		if(description == defaultText)
		{
			showNotification(builder, true)
		}
		else
		{
			showNotification(builder, useSound)
		}
	}

	fun putValues(valuesPair: Pair<String?, Bitmap?>)
	{
		Log.d("url in service", valuesPair.first.toString())
		createLogic(valuesPair.first, valuesPair.second, false)
	}

	private fun prepareNotification(useSound: Boolean)
	{
		createNotificationChannel()
		createLogic(null, null, useSound)
	}

	inner class ServiceBinder: Binder()
	{
		fun get(): MainNotificationService = this@MainNotificationService
	}
}