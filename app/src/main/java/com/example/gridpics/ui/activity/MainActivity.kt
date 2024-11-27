package com.example.gridpics.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.example.gridpics.ui.services.MainNotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.settings.ThemePick
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var themePick: Int = ThemePick.FOLLOW_SYSTEM.intValue
	private var serviceIntent = Intent()
	private var mainNotificationService = MainNotificationService()
	private var mBound: Boolean = false
	private var currentPictureSP = ""
	private val connection = object: ServiceConnection
	{
		override fun onServiceConnected(className: ComponentName, service: IBinder)
		{
			val binder = service as MainNotificationService.NetworkServiceBinder
			mainNotificationService = binder.get()
			mBound = true
		}

		override fun onServiceDisconnected(arg0: ComponentName)
		{
			mBound = false
		}

		override fun onBindingDied(name: ComponentName?)
		{
			mBound = false
			super.onBindingDied(name)
		}
	}

	@SuppressLint("UseCompatLoadingForDrawables")
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		Log.d("lifecycle", "onCreate()")
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		val picVM = picturesViewModel
		val lifeCycScope = lifecycleScope
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		// currentPictureSP - Здесь мы получаем картинку, которая была выбранна пользователем при переходе на экран деталей,
		// чтобы при смене темы и пересоздании MainActivity не было ошибки/потери значений.
		currentPictureSP = sharedPreferences.getString(PICTURE, NULL_STRING)!! //100% won't be null as valu
		//serviceIntentForNotification
		val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
		// Здесь мы получаем значение выбранной темы раннее, чтобы приложение сразу её выставило
		themePick = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		settingsViewModel.changeTheme(themePick)

		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white))
		)
		// Здесь происходит получение всех кэшированных картинок,точнее их url,
		// чтобы их можно было "достать" из кэша и отобразить с помощью библиотеки Coil
		picVM.postSavedUrls(sharedPreferences.getString(SHARED_PREFS_PICTURES, null))
		// Здесь мы проверяем менялась ли тема при прошлой жизни Activity, если да, то не создавать новое уведомление
		picVM.postCacheWasCleared(getSharedPreferences(JUST_CHANGED_THEME, MODE_PRIVATE).getBoolean(JUST_CHANGED_THEME, false))
		Log.d("theme", "just changed theme? ")

		picVM.getPics()
		lifeCycScope.launch {
			picVM.observeCurrentImg().collectLatest {
				currentPictureSP = it
			}
		}

		lifeCycScope.launch {
			picVM.observeBackNav().collectLatest {
				if(it)
				{
					stopService(serviceIntent)
					this@MainActivity.finishAffinity()
				}
			}
		}
		val editorSharedPrefs = sharedPreferences.edit()
		editorSharedPrefs.putBoolean(JUST_CHANGED_THEME, false)
		editorSharedPrefs.putInt(THEME_SHARED_PREFERENCE, themePick)
		editorSharedPrefs.apply()

		serviceIntent = serviceIntentLocal

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
				ComposeTheme {
					PicturesScreen(
						navController = navController,
						postPressOnBackButton = { picVM.backNavButtonPress(true) },
						checkIfExists = { str -> picVM.checkOnErrorExists(str) },
						addError = { str -> picVM.addError(str) },
						getPics = { picVM.getPics() },
						postState = { urls -> picVM.postState(urls) },
						state = picVM.picturesUiState,
						clearErrors = { picVM.clearErrors() },
						postPositiveState = { detVM.postPositiveVisabilityState() },
						postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE) },
						currentPicture = { url -> picVM.clickOnPicture(url) },
						isValidUrl = { url -> picVM.isValidUrl(url) },
						postSavedUrls = { urls -> picVM.postSavedUrls(urls) }
					)
				}
			}
			composable(BottomNavItem.Settings.route) {
				ComposeTheme {
					SettingsScreen(
						navController,
						themePick,
						postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE) },
						changeTheme = { int -> settingsViewModel.changeTheme(int) },
					)
				}
			}
			composable(Screen.Details.route) {
				ComposeTheme {
					DetailsScreen(
						navController = navController,
						checkIfExists = { str -> picVM.checkOnErrorExists(str) },
						addError = { str -> picVM.addError(str) },
						state = detVM.uiStateFlow,
						removeSpecialError = { str -> picVM.removeSpecialError(str) },
						postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE) },
						changeVisabilityState = { detVM.changeVisabilityState() },
						postUrl = { str, p -> detVM.postNewPic(str, p) },
						postPositiveState = { detVM.postPositiveVisabilityState() },
						picturesScreenState = picVM.picturesUiState,
						pic = currentPictureSP,
						isValidUrl = { url -> picVM.isValidUrl(url) },
						convertPicture = { bitmap: Bitmap -> detVM.convertPictureToString(bitmap) },
						window = window
					)
				}
			}
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		if(isInMultiWindowMode || isInPictureInPictureMode)
		{
			detailsViewModel.changeMultiWindowState(true)
		}
		else if(!isInMultiWindowMode)
		{
			detailsViewModel.changeMultiWindowState(false)
		}
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

	override fun onRestart()
	{
		Log.d("lifecycle", "onRestart()")
		mBound = false
		super.onRestart()
	}

	override fun onResume()
	{
		val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
		val connectionLocal = connection
		if(!mBound)
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
					ActivityCompat.requestPermissions(
						this,
						arrayOf(Manifest.permission.POST_NOTIFICATIONS),
						100
					)
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
			lifecycleScope.launch {
				detailsViewModel.observeUrlFlow().collectLatest {
					if(ContextCompat.checkSelfPermission(
							this@MainActivity,
							Manifest.permission.POST_NOTIFICATIONS,
						) == PackageManager.PERMISSION_GRANTED)
					{
						if(mBound)
						{
							mainNotificationService.putValues(it)
						}
					}
				}
			}
		}
		Log.d("service", "is connected to Activity?: $mBound")
		countExitNavigation++
		Log.d("lifecycle", "onResume()")
		super.onResume()
	}

	override fun onPause()
	{
		Log.d("lifecycle", "onPause()")
		if(mBound)
		{
			Log.d("Hello", "This method wont work .-.")
			unbindService(connection)
		}
		countExitNavigation++
		super.onPause()
	}

	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		super.onDestroy()
	}

	companion object
	{
		var countExitNavigation = 0
		const val NOTIFICATION_ID = 1337
		const val CACHE = "CACHE_CACHE"
		const val SHARED_PREFS_PICTURES = "SHARED_PREFS_PICTURES"
		const val PICTURE = "PICTURE"
		const val THEME_SHARED_PREFERENCE = "THEME_SHARED_PREFERENCE"
		const val CHANNEL_NOTIFICATIONS_ID = "GRID_PICS_CHANEL_NOTIFICATIONS_ID"
		const val NULL_STRING = "NULL_STRING"
		const val JUST_CHANGED_THEME = "JUST_CHANGED_THEME"
		const val SHARED_PREFERENCE_GRIDPICS = "SHARED_PREFERENCE_GRIDPICS"
		const val DEFAULT_STRING_VALUE = "default"
		const val HTTP_ERROR = "HTTP error: 404"
	}
}
