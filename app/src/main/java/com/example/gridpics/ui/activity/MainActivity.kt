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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gridpics.R
import com.example.gridpics.ui.details.DetailsScreen
import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesScreen
import com.example.gridpics.ui.pictures.PicturesState
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.services.MainNotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.settings.ThemePick
import com.example.gridpics.ui.state.UiStateDataClass
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
	private var description: Pair<String, String?> = Pair(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE)
	private var mainNotificationService = MainNotificationService()
	private var mBound: Boolean = false
	private var changedTheme = false
	private var currentPictureSP = ""
	private var imagesStringUrlsSP: String? = null
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
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		// currentPictureSP - Здесь мы получаем картинку, которая была выбранна пользователем при переходе на экран деталей,
		// чтобы при смене темы и пересоздании MainActivity не было ошибки/потери значений.
		currentPictureSP = sharedPreferences.getString(PICTURE, NULL_STRING)!! //100% won't be null as valu
		val connectionLocal = connection
		//serviceIntentForNotification
		val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
		serviceIntentLocal.putExtra(DESCRIPTION_NAMING, description.toList().last().toString())
		// Здесь мы получаем значение выбранной темы раннее, чтобы приложение сразу её выставило
		themePick = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		settingsViewModel.changeTheme(themePick)
		// imagesStringUrlsSP - Здесь происходит получение всех кэшированных картинок,точнее их url,
		// чтобы их можно было "достать" из кэша и отобразить с помощью библиотеки Coil
		imagesStringUrlsSP = sharedPreferences.getString(SHARED_PREFS_PICTURES, null)
		// Здесь мы проверяем менялась ли тема при прошлой жизни Activity, если да, то не создавать новое уведомление
		changedTheme = getSharedPreferences(JUST_CHANGED_THEME, MODE_PRIVATE).getBoolean(JUST_CHANGED_THEME, false)
		val picVM = picturesViewModel
		val detailsVM = detailsViewModel
		val lifeCycScope = lifecycleScope
		Log.d("theme", "just changed theme? ")
		//Start showing notification
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !changedTheme)
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

		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white))
		)
		val uiState: MutableState<UiStateDataClass> = mutableStateOf(UiStateDataClass(isMultiWindowed = false, barsAreVisible = true))
		picVM.getPics()
		lifeCycScope.launch {
			picVM.observeCurrentImg().collectLatest {
				currentPictureSP = it
			}
		}
		val window = window
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		lifeCycScope.launch {
			picVM.observeUiState().collectLatest {
				uiState.value = it
				if(!it.barsAreVisible)
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

		lifeCycScope.launch {
			picVM.observeBackNav().collectLatest {
				if(it)
				{
					stopService(serviceIntent)
					this@MainActivity.finishAffinity()
				}
			}
		}

		val picturesScreenState: MutableState<PicturesState> = mutableStateOf(PicturesState.NothingFound)
		lifeCycScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				picVM.observePicturesFlow().collectLatest {
					picturesScreenState.value = it!!
				}
			}
		}

		lifeCycScope.launch {
			detailsVM.observeUrlFlow().collectLatest {
				if(ContextCompat.checkSelfPermission(
						this@MainActivity,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED)
				{
					Log.d("checkma keys", "${it.keys}")
					if(it.keys.isNotEmpty())
					{
						val e = it.keys.toList().last()
						description = Pair(e, it[e])
						mainNotificationService.getValues(description)
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

		val editorSharedPrefs = sharedPreferences.edit()
		editorSharedPrefs.putBoolean(JUST_CHANGED_THEME, false)
		editorSharedPrefs.putInt(THEME_SHARED_PREFERENCE, themePick)
		editorSharedPrefs.apply()

		serviceIntent = serviceIntentLocal

		setContent {
			ComposeTheme {
				val navController = rememberNavController()
				NavigationSetup(navController = navController, uiState = uiState, picturesScreenState = picturesScreenState)
			}
		}
	}

	@Composable
	fun NavigationSetup(navController: NavHostController, uiState: MutableState<UiStateDataClass>, picturesScreenState: MutableState<PicturesState>)
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
						postState = { picVM.postState() },
						state = picturesScreenState,
						clearErrors = { picVM.clearErrors() },
						postPositiveState = { picVM.postPositiveVisabilityState() },
						postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE) },
						sharedPrefsPictures = imagesStringUrlsSP,
						clearedCache = changedTheme,
						currentPicture = { url -> picVM.clickOnPicture(url) },
						isValidUrl = { url -> picVM.isValidUrl(url) }
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
						state = uiState,
						removeSpecialError = { str -> picVM.removeSpecialError(str) },
						postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE) },
						changeVisabilityState = { picVM.changeVisabilityState() },
						postUrl = { str, p -> detVM.postNewPic(str, p) },
						postPositiveState = { picVM.postPositiveVisabilityState() },
						pictures = imagesStringUrlsSP,
						pic = currentPictureSP,
						isValidUrl = { url -> picVM.isValidUrl(url) },
						convertPicture = { bitmap: Bitmap -> detVM.convertPictureToString(bitmap) }
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
			picturesViewModel.changeMultiWindowState(true)
		}
		else if(!isInMultiWindowMode)
		{
			picturesViewModel.changeMultiWindowState(false)
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
		super.onRestart()
	}

	override fun onResume()
	{
		Log.d("service", "is connected to Activity?: $mBound")
		if(ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS,
			) == PackageManager.PERMISSION_GRANTED)
		{
			val newIntent = serviceIntent
			val connectionLocal = connection
			mainNotificationService.getValues(description)
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				startService(newIntent)
				bindService(newIntent, connectionLocal, Context.BIND_AUTO_CREATE)
				Log.d("service", "connection $connectionLocal")
			}
			else
			{
				startService(newIntent)
				bindService(newIntent, connectionLocal, Context.BIND_AUTO_CREATE)
			}
			countExitNavigation++
		}
		Log.d("lifecycle", "onResume()")
		super.onResume()
	}

	override fun onPause()
	{
		Log.d("lifecycle", "onPause()")
		if(mBound)
		{
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
		const val DESCRIPTION_NAMING = "description"
		const val SHARED_PREFERENCE_GRIDPICS = "SHARED_PREFERENCE_GRIDPICS"
		const val DEFAULT_STRING_VALUE = "default"
		const val HTTP_ERROR = "HTTP error: 404"
	}
}
