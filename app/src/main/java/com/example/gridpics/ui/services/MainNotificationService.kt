package com.example.gridpics.ui.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import com.example.gridpics.ui.activity.MainActivity.Companion.DESCRIPTION_NAMING
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
import com.example.gridpics.ui.activity.MainActivity.Companion.countExitNavigation
import com.example.gridpics.ui.activity.MainActivity.Companion.jobForNotification
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainNotificationService: Service()
{
	private val binder = NetworkServiceBinder()
	private var isActive = true
	private lateinit var contentText: String
	private val _subscriberCount = MutableStateFlow(0)
	val subscriberCount: Flow<Int> = _subscriberCount
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		jobForNotification.cancelChildren()
		isActive = true
		Log.d("service", "service onStartCommand")
		val dontUseSound = countExitNavigation > 1
		val resultIntent = Intent(instance, MainActivity::class.java)
		val resultPendingIntent = PendingIntent.getActivity(instance, 0, resultIntent,
			PendingIntent.FLAG_IMMUTABLE)
		val extras = intent?.extras
		contentText = if(!extras?.getString(DESCRIPTION_NAMING).isNullOrEmpty() && extras?.getString(DESCRIPTION_NAMING) != DEFAULT_STRING_VALUE)
		{
			extras!!.getString(DESCRIPTION_NAMING)!!
		}
		else
		{
			getString(R.string.notification_content_text)
		}
		Log.d("intent", "we got $contentText")

		if(!contentText.contains(getString(R.string.notification_content_text)))
		{
			val words = contentText.substring(1, contentText.length - 1).split(",")
			val description = words[0].trim()
			val stringImage = words[1].trim()

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
				.setColor(getColor(R.color.green))
				.setContentTitle(getString(R.string.gridpics))
				.setContentText(description)
				.setLargeIcon(bitmap)
				.setStyle(NotificationCompat.BigPictureStyle()
					.bigPicture(bitmap)
					.bigLargeIcon(null as Icon?))
			createNotificationChannel()
			showNotification(builder)
		}
		else
		{
			val builder = Builder(this@MainNotificationService, MainActivity.CHANNEL_NOTIFICATIONS_ID)
				.setContentIntent(resultPendingIntent)
				.setAutoCancel(true)
				.setOngoing(true)
				.setSilent(dontUseSound)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setColor(getColor(R.color.green))
				.setContentTitle(getString(R.string.gridpics))
				.setContentText(contentText)
			createNotificationChannel()
			showNotification(builder)
		}
		return START_NOT_STICKY
	}

	@OptIn(DelicateCoroutinesApi::class)
	override fun onBind(intent: Intent?): IBinder
	{
		_subscriberCount.value += 1
		isActive = true
		Log.d("service", "service onBind")
		val dontUseSound = countExitNavigation > 1
		val resultIntent = Intent(instance, MainActivity::class.java)
		val resultPendingIntent = PendingIntent.getActivity(instance, 0, resultIntent,
			PendingIntent.FLAG_IMMUTABLE)
		val extras = intent?.extras
		contentText = if(!extras?.getString(DESCRIPTION_NAMING).isNullOrEmpty() && extras?.getString(DESCRIPTION_NAMING) != DEFAULT_STRING_VALUE)
		{
			extras!!.getString(DESCRIPTION_NAMING)!!
		}
		else
		{
			getString(R.string.notification_content_text)
		}
		Log.d("intent", "we got $contentText")
		if(!contentText.contains(getString(R.string.notification_content_text)))
		{
			val words = contentText.substring(1, contentText.length - 1).split(",")
			val description = words[0].trim()
			val stringImage = words[1].trim()

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
				.setColor(getColor(R.color.green))
				.setContentTitle(getString(R.string.gridpics))
				.setContentText(description)
				.setLargeIcon(bitmap)
				.setStyle(NotificationCompat.BigPictureStyle()
					.bigPicture(bitmap)
					.bigLargeIcon(null as Icon?))
			createNotificationChannel()
			showNotification(builder)
		}
		else
		{
			val builder = Builder(this@MainNotificationService, MainActivity.CHANNEL_NOTIFICATIONS_ID)
				.setContentIntent(resultPendingIntent)
				.setAutoCancel(true)
				.setOngoing(true)
				.setSilent(dontUseSound)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setColor(getColor(R.color.green))
				.setContentTitle(getString(R.string.gridpics))
				.setContentText(contentText)
			createNotificationChannel()
			showNotification(builder)
		}
		return binder
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
			val channel = NotificationChannel(MainActivity.CHANNEL_NOTIFICATIONS_ID, name, importance)
			channel.description = description
			val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	override fun onRebind(intent: Intent?)
	{
		isActive = true
		_subscriberCount.value += 2
		super.onRebind(intent)
	}

	private fun showNotification(builder: Builder)
	{
		Log.d("description in service", contentText.length.toString())
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(NOTIFICATION_ID, builder.build())
		startForeground(NOTIFICATION_ID, builder.build())
	}

	@OptIn(DelicateCoroutinesApi::class)
	override fun onUnbind(intent: Intent?): Boolean
	{
		_subscriberCount.value -= 1
		isActive = false
		Log.d("service", "onUnbind()")
		//stopNotificationCoroutine()
		return super.onUnbind(intent)
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun stopNotificationCoroutine()
	{
		GlobalScope.launch(Dispatchers.IO + jobForNotification) {
			Log.d("service", "stopNotificationCoroutine has been started")
			for(i in 0 .. 10)
			{
				delay(200)
				if(isActive)
				{
					cancel()
				}
				else if(i == 10)
				{
					stopSelf()
					Log.d("service", "service was stopped")
				}
			}
		}
	}

	inner class NetworkServiceBinder: Binder()
	{
		fun get(): MainNotificationService = this@MainNotificationService
	}
}