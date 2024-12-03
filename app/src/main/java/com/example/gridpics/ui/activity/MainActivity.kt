package com.example.gridpics.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
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
import com.example.gridpics.ui.service.MainNotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.ThemePick
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var themePick: Int = ThemePick.FOLLOW_SYSTEM.intValue
	private var serviceIntent = Intent()
	private var mainNotificationService: MainNotificationService? = null
	private val connection = object: ServiceConnection
	{
		override fun onServiceConnected(className: ComponentName, service: IBinder)
		{
			val binder = service as MainNotificationService.NetworkServiceBinder
			val mainService = binder.get()
			val flowValue = detailsViewModel.observeUrlFlow().value
			if(flowValue.first != DEFAULT_STRING_VALUE)
			{
				mainService.putValues(flowValue)
			}
			mainNotificationService = mainService
		}

		override fun onServiceDisconnected(arg0: ComponentName)
		{
			mainNotificationService = null
		}

		override fun onBindingDied(name: ComponentName?)
		{
			mainNotificationService = null
			super.onBindingDied(name)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		Log.d("lifecycle", "onCreate()")
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		val picVM = picturesViewModel
		val detVM = detailsViewModel
		val lifeCycScope = lifecycleScope
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		//serviceIntentForNotification
		val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
		// Здесь мы получаем значение выбранной темы раннее, чтобы приложение сразу её выставило
		val theme = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		changeTheme(theme)
		picVM.postSavedUrls(sharedPreferences.getString(SHARED_PREFS_PICTURES, null))
		picVM.postCacheWasCleared(false)
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(getColor(R.color.white), getColor(R.color.black)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.white), getColor(R.color.black))
		)
		// Здесь происходит получение всех кэшированных картинок,точнее их url,
		// чтобы их можно было "достать" из кэша и отобразить с помощью библиотеки Coil
		picVM.postSavedUrls(sharedPreferences.getString(SHARED_PREFS_PICTURES, null))
		// Здесь мы проверяем менялась ли тема при прошлой жизни Activity, если да, то не создавать новое уведомление
		lifeCycScope.launch {
			detVM.observeUrlFlow().collectLatest {
				if(ContextCompat.checkSelfPermission(
						this@MainActivity,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED)
				{
					mainNotificationService?.putValues(it)
				}
			}
		}

		lifeCycScope.launch {
			picVM.observeBackNav().collectLatest {
				if(it != null && it)
				{
					Log.d("callback", "callback was called")
					if(mainNotificationService != null)
					{
						stopService(serviceIntentLocal)
					}
					this@MainActivity.finishAffinity()
				}
			}
		}

		serviceIntent = serviceIntentLocal
		themePick = theme
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
		NavHost(
			navController,
			startDestination = BottomNavItem.Home.route,
			enterTransition = {
				EnterTransition.None
			},
			exitTransition = {
				ExitTransition.None
			}
		)
		{
			val picVM = picturesViewModel
			val detVM = detailsViewModel
			composable(BottomNavItem.Home.route) {
				PicturesScreen(
					navController = navController,
					postPressOnBackButton = { picVM.backNavButtonPress(true) },
					checkIfExists = { str -> picVM.checkOnErrorExists(str) },
					addError = { str -> picVM.addError(str) },
					postState = { useLoadedState, urls -> picVM.postState(useLoadedState, urls) },
					state = picVM.picturesUiState,
					clearErrors = { picVM.clearErrors() },
					postPositiveState = { detVM.postPositiveVisabilityState() },
					currentPicture = { url -> picVM.clickOnPicture(url) },
					isValidUrl = { url -> picVM.isValidUrl(url) },
					postSavedUrls = { urls -> picVM.postSavedUrls(urls) },
					postDefaultDescription = { url -> detVM.postNewPic(url, null) }
				)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(
					navController = navController,
					option = themePick,
					postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, null) },
					changeTheme = { int -> changeTheme(int) },
					postCacheWasCleared = { cleared -> picVM.postCacheWasCleared(cleared) }
				)
			}
			composable(Screen.Details.route) {
				DetailsScreen(
					navController = navController,
					checkIfExists = { str -> picVM.checkOnErrorExists(str) },
					addError = { str -> picVM.addError(str) },
					state = detVM.uiStateFlow,
					removeSpecialError = { str -> picVM.removeSpecialError(str) },
					changeVisabilityState = { detVM.changeVisabilityState() },
					postUrl = { url, bitmap -> detVM.postNewPic(url, bitmap) },
					postPositiveState = { detVM.postPositiveVisabilityState() },
					picturesScreenState = picVM.picturesUiState,
					updatedCurrentPicture = picVM.currentPicture,
					isValidUrl = { url -> picVM.isValidUrl(url) },
					changeBarsVisability = { visability -> changeBarsVisability(visability) },
					postNewBitmap = { url -> detVM.postImageBitmap(url) }
				)
			}
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		val detVM = detailsViewModel
		if(isInMultiWindowMode || isInPictureInPictureMode)
		{
			detVM.changeMultiWindowState(true)
		}
		else
		{
			detVM.changeMultiWindowState(false)
		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray,
	)
	{
		lifecycleScope.launch {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults)
			if(requestCode == 100)
			{
				if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
				{
					val serviceIntentLocal = serviceIntent
					val connectionLocal = connection
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					{
						startForegroundService(serviceIntentLocal)
						bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
					}
					else
					{
						startService(serviceIntentLocal)
						bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
					}
				}
			}
		}
	}

	override fun onRestart()
	{
		Log.d("lifecycle", "onRestart()")
		super.onRestart()
	}

	override fun onResume()
	{
		val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
		val connectionLocal = connection
		if(mainNotificationService == null)
		{
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			{
				if(
					ContextCompat.checkSelfPermission(
						this,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED
				)
				{
					startForegroundService(serviceIntentLocal)
					bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
				}
				else
				{
					if(!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
					{
						requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
					}
				}
			}
			else
			{
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				{
					startForegroundService(serviceIntentLocal)
					bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
				}
				else
				{
					startService(serviceIntentLocal)
					bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
				}
			}
		}
		Log.d("service", "is connected to Activity?: ${mainNotificationService != null}")
		Log.d("lifecycle", "onResume()")
		super.onResume()
	}

	override fun onPause()
	{
		Log.d("lifecycle", "onPause()")
		if(mainNotificationService != null)
		{
			unbindService(connection)
			mainNotificationService = null
		}
		super.onPause()
	}

	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		super.onDestroy()
	}

	private fun changeBarsVisability(visible: Boolean)
	{
		val detVM = detailsViewModel
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		if(!visible)
		{
			controller.hide(WindowInsetsCompat.Type.statusBars())
			controller.hide(WindowInsetsCompat.Type.navigationBars())
		}
		else
		{
			controller.show(WindowInsetsCompat.Type.statusBars())
			controller.show(WindowInsetsCompat.Type.navigationBars())
			if(detVM.uiStateFlow.value == detVM.uiStateFlow.value.copy(barsAreVisible = false))
			{
				detVM.changeVisabilityState()
			}
		}
	}

	private fun changeTheme(option: Int)
	{
		Log.d("theme option", "theme option: $option")
		when(option)
		{
			ThemePick.LIGHT_THEME.intValue ->
			{
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			}
			ThemePick.DARK_THEME.intValue ->
			{
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
			}
			ThemePick.FOLLOW_SYSTEM.intValue ->
			{
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
			}
		}
	}

	companion object
	{
		const val LENGTH_OF_PICTURE = 110
		const val TEXT_PLAIN = "text/plain"
		const val NOTIFICATION_ID = 1337
		const val SHARED_PREFS_PICTURES = "SHARED_PREFS_PICTURES"
		const val THEME_SHARED_PREFERENCE = "THEME_SHARED_PREFERENCE"
		const val CHANNEL_NOTIFICATIONS_ID = "GRID_PICS_CHANEL_NOTIFICATIONS_ID"
		const val SHARED_PREFERENCE_GRIDPICS = "SHARED_PREFERENCE_GRIDPICS"
		const val DEFAULT_STRING_VALUE = "default"
		const val HTTP_ERROR = "HTTP error: 404"
	}
}
