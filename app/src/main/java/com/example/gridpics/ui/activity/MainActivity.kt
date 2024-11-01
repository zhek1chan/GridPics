package com.example.gridpics.ui.activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gridpics.R
import com.example.gridpics.ui.details.DetailsScreen
import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesScreen
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var picturesSharedPrefs: String? = null
	private var changedTheme: Boolean? = null
	private var idCounter = 0
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		super.onCreate(savedInstanceState)

		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white))
		)

		changedTheme = getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE).getBoolean(THEME_SP_KEY, true)
		if(!changedTheme!!)
		{
			settingsViewModel.postValue(this, false)
		}
		else
		{
			settingsViewModel.postValue(this, true)
		}
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		lifecycleScope.launch {
			detailsViewModel.observeFlow().collectLatest {
				if(it)
				{
					controller.hide(WindowInsetsCompat.Type.statusBars())
					controller.hide(WindowInsetsCompat.Type.navigationBars())
				}
				else
				{
					controller.show(WindowInsetsCompat.Type.statusBars())
					controller.show(WindowInsetsCompat.Type.navigationBars())
				}
			}
		}

		lifecycleScope.launch {
			picturesViewModel.observeBackNav().collectLatest {
				if(it)
				{
					cancelNotification(idCounter)
					picturesViewModel.backNavButtonPress(false)
					this@MainActivity.finish()
				}
			}
		}

		picturesSharedPrefs = this.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, null)
		idCounter = getSharedPreferences(ID_COUNTER, MODE_PRIVATE).getInt(ID_COUNTER, 0)
		setContent {
			ComposeTheme {
				val navController = rememberNavController()
				NavigationSetup(navController = navController)
			}
		}
	}

	@Composable
	fun NavigationSetup(navController: NavHostController)
	{
		NavHost(navController, startDestination = BottomNavItem.Home.route, enterTransition = {
			EnterTransition.None
		},
			exitTransition = {
				ExitTransition.None
			}) {
			composable(BottomNavItem.Home.route) {
				PicturesScreen(navController, picturesViewModel)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(settingsViewModel, changedTheme!!, navController)
			}
			composable(Screen.Details.route) {
				DetailsScreen(navController, detailsViewModel, picturesViewModel)
			}
		}
	}

	private fun createNotificationChannel()
	{
		val name = "My Notification Channel"
		val description = "Channel for my notification"
		// Создаем канал уведомлений (для Android O и выше)
		if(Build.VERSION.SDK_INT >= VERSION_CODES.O)
		{
			val importance = NotificationManager.IMPORTANCE_HIGH
			val channel = NotificationChannel(CHANNEL_ID, name, importance)
			channel.description = description
			val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	private fun showNotification()
	{
		val intent = Intent(this, MainActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
		val builder = NotificationCompat.Builder(this, CHANNEL_ID)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
			.setContentIntent(null)
			.setAutoCancel(true)
			.setOngoing(true)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setColor(getColor(R.color.green))
			.setContentTitle("GridPics")
			.setContentText("Вы видите это уведомление, потому что приложение активно")
			.setContentIntent(pendingIntent)
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(idCounter, builder.build())
		Log.d("lifecycle", "show notification $idCounter")
	}

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		checkMultiWindowMode()
	}

	private fun checkMultiWindowMode()
	{
		if(isInMultiWindowMode || isInPictureInPictureMode)
		{
			detailsViewModel.postState(true)
		}
		else
		{
			detailsViewModel.postState(false)
		}
	}

	override fun onStart()
	{
		Log.d("lifecycle", "onStart()")
		if(Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU)
		{
			if(
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED
			)
			{
				createNotificationChannel()
				showNotification()
			}
			else
			{
				ActivityCompat.requestPermissions(
					this,
					arrayOf(Manifest.permission.POST_NOTIFICATIONS),
					100
				)
			}
		}
		else
		{
			createNotificationChannel()
			showNotification()
		}
		super.onStart()
	}

	override fun onRestart()
	{
		idCounter += 1
		Log.d("lifecycle", "onRestart()")
		createNotificationChannel()
		showNotification()
		super.onRestart()
	}

	override fun onStop()
	{
		Log.d("lifecycle", "onStop()")
		this.lifecycleScope.launch {
			delay(3000)
			cancelNotification(idCounter)
		}
		super.onStop()
	}

	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		val count = getSharedPreferences(ID_COUNTER, MODE_PRIVATE)
		val editor = count.edit()
		editor.putInt(ID_COUNTER, 0)
		editor.apply()
		this.lifecycleScope.launch {
			delay(3000)
			cancelNotification(idCounter)
		}
		cancelNotification(idCounter)
		val vis = getSharedPreferences(WE_WERE_HERE_BEFORE, MODE_PRIVATE)
		val editorVis = vis.edit()
		editorVis.putBoolean(WE_WERE_HERE_BEFORE, false)
		editorVis.apply()
		super.onDestroy()
	}

	override fun onPause()
	{
		Log.d("lifecycle", "onPause()")
		this.lifecycleScope.launch {
			delay(3000)
			cancelNotification(idCounter)
		}
		super.onPause()
	}

	private fun cancelNotification(id: Int)
	{
		Log.d("lifecycle", "canceling notification $id")
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancel(id)
	}

	companion object
	{
		const val ID_COUNTER = "ID_COUNTER"
		const val CACHE = "CACHE"
		const val PICTURES = "PICTURES_SHARED_PREFS"
		const val PIC = "PIC"
		const val THEME_SP_KEY = "THEME_SHARED_PREFS"
		const val CHANNEL_ID = "GRID_PICS_CHANEL_ID"
		const val NULL_STRING = "NULL"
		const val TOP_BAR_VISABILITY = "TOP_BAR_VISABILITY"
		const val WE_WERE_HERE_BEFORE = "WE_WERE_HERE_BEFORE"
	}
}
