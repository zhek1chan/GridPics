package com.example.gridpics.ui.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
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
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		super.onCreate(savedInstanceState)
		/*if(Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU)
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
		}*/
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
		val isConditionAlreadySet = checkSomeCondition()
		val callback = object: OnBackPressedCallback(
			isConditionAlreadySet
		)
		{
			override fun handleOnBackPressed()
			{
				this.handleOnBackPressed()
			}
		}
		onBackPressedDispatcher.addCallback(this, callback)

		picturesSharedPrefs = this.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, null)


		setContent {
			ComposeTheme {
				val navController = rememberNavController()
				NavigationSetup(navController = navController)
			}
		}
	}

	private fun checkSomeCondition() = false

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
			val importance = NotificationManager.IMPORTANCE_LOW
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
			.setSmallIcon(R.mipmap.ic_launcher)
			.setColor(getColor(R.color.green))
			.setContentTitle("GridPics")
			.setContentText("Вы видите это уведомление, потому что приложение активно")
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setContentIntent(pendingIntent)
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(1, builder.build())
	}

	override fun onRestart()
	{
		//showNotification()
		//createNotificationChannel()
		super.onRestart()
	}

	override fun onStop()
	{
		/*this.lifecycleScope.launch {
			delay(3000)
			cancelAllNotifications()
		}*/
		super.onStop()
	}

	override fun onDestroy()
	{
		//cancelAllNotifications()
		super.onDestroy()
	}

	override fun onPause()
	{
		/*this.lifecycleScope.launch {
			delay(3000)
			cancelAllNotifications()
		}*/
		super.onPause()
	}

	private fun cancelAllNotifications()
	{
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancelAll()
	}

	companion object
	{
		const val CACHE = "CACHE"
		const val PICTURES = "PICTURES_SHARED_PREFS"
		const val PIC = "PIC"
		const val THEME_SP_KEY = "THEME_SHARED_PREFS"
		const val CHANNEL_ID = "GRID_PICS_CHANEL_ID"
		const val NULL_STRING = "NULL"
		const val TOP_BAR_VISABILITY = "TOP_BAR_VISABILITY"
	}
}
