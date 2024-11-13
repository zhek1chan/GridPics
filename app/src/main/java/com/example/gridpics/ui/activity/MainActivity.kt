package com.example.gridpics.ui.activity

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.STOP_FOREGROUND_REMOVE
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.app.ServiceCompat.stopForeground
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gridpics.R
import com.example.gridpics.ui.details.DetailsScreen
import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesScreen
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.services.NotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class MainActivity: AppCompatActivity()
{
	private var isActive: Boolean = false
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var picturesSharedPrefs: String? = null
	private var changedTheme: Boolean? = null
	private var serviceIntent = Intent()
	private var notifyService: NotificationService? = null
	private val notifServiceReady = NotificationService()
	private val connection = object: ServiceConnection
	{
		override fun onServiceConnected(name: ComponentName?, service: IBinder?)
		{
			val binder = service as NotificationService.NetworkServiceBinder
			notifyService = binder.get()
		}

		override fun onServiceDisconnected(name: ComponentName?)
		{
			notifyService = null
		}
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		connection
		Log.d("lifecycle", "onCreate()")
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()

		serviceIntent = Intent(this, NotificationService::class.java)

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			if(
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED
			)
			{
				startForegroundService(serviceIntent)
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
			startForegroundService(serviceIntent)
		}
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
		CoroutineScope(Dispatchers.Main).launch {
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

		CoroutineScope(Dispatchers.Main).launch {
			picturesViewModel.observeBackNav().collectLatest {
				if(it)
				{
					//stopForeground(notifServiceReady, STOP_FOREGROUND_REMOVE)
					this@MainActivity.finishAffinity()
				}
			}
		}

		picturesSharedPrefs = this.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, null)
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

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		checkMultiWindowMode()
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray,
	)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if(requestCode == 100)
		{
			if(ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED)
			{
				startForegroundService(serviceIntent)
			}
		}
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
		if(ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS,
			) == PackageManager.PERMISSION_GRANTED)
		{
			startForegroundService(serviceIntent)
		}
		isActive = true
		Log.d("lifecycle", "onStart()")
		super.onStart()
	}

	override fun onPause()
	{
		Log.d("lifecycle", "onPause()")
		super.onPause()
		if(isServiceRunning(NotificationService::class.java))
		{
		}
		isActive = false
	}

	//КОД ДОБАВЛЕН ДЛЯ ТЕСТА НА САМСУНГАХ
	/*@OptIn(DelicateCoroutinesApi::class)
	override fun onStop()
	{
		GlobalScope.launch {
			delay(600)
			stopService(serviceIntent)
			val manager: NotificationManager = getSystemService(NotificationManager::class.java)
			manager.cancel(NOTIFICATION_ID)
		}
		super.onStop()
	}*/

	@OptIn(DelicateCoroutinesApi::class)
	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		val vis = getSharedPreferences(WE_WERE_HERE_BEFORE, MODE_PRIVATE)
		val editorVis = vis.edit()
		editorVis.putBoolean(WE_WERE_HERE_BEFORE, false)
		editorVis.apply()
		GlobalScope.launch {
		}
		if(countExitNavigation >= 1)
		{
			this@MainActivity.finishAffinity()
		}
		super.onDestroy()
	}

	@Suppress("DEPRECATION")
	private fun isServiceRunning(serviceClass: Class<*>): Boolean
	{
		val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
		for(service in manager.getRunningServices(Int.MAX_VALUE))
		{
			if(serviceClass.name == service.service.className)
			{
				return true
			}
		}
		return false
	}

	companion object
	{
		val jobForNotifications = Job()
		var countExitNavigation = 0
		const val NOTIFICATION_ID = 1337
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
