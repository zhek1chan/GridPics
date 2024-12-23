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
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var themePick: Int = ThemePick.FOLLOW_SYSTEM.intValue
	private var mainNotificationService: MainNotificationService? = null
	private var newIntentFlag = true
	private lateinit var navigation: NavHostController
	private val connection = object: ServiceConnection
	{
		override fun onServiceConnected(className: ComponentName, service: IBinder)
		{
			val binder = service as MainNotificationService.ServiceBinder
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
		picVM.changeOrientation(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		// Здесь происходит получение всех кэшированных картинок,точнее их url,
		// чтобы их можно было "достать" из кэша и отобразить с помощью библиотеки Coil
		val picturesFromSP = sharedPreferences.getString(SHARED_PREFS_PICTURES, null)
		picVM.postSavedUrls(urls = picturesFromSP, caseEmptySharedPreferenceOnFirstLaunch = (picturesFromSP == null))
		// Здесь мы получаем значение выбранной темы раннее, чтобы приложение сразу её выставило
		val theme = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		changeTheme(theme)
		val blackColor = getColor(R.color.black)
		val whiteColor = getColor(R.color.white)
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(whiteColor, blackColor),
			navigationBarStyle = SystemBarStyle.auto(whiteColor, blackColor)
		)
		lifeCycScope.launch {
			detVM.observeUrlFlow().collect {
				if(ContextCompat.checkSelfPermission(
						this@MainActivity,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED)
				{
					Log.d("service", "data $it")
					mainNotificationService?.putValues(it)
				}
			}
		}
		themePick = theme
		//реализация фичи - поделиться картинкой в приложение
		setContent {
			val navController = rememberNavController()
			LaunchedEffect(Unit) {
				navigation = navController
				newIntentFlag = true
				postValuesFromIntent(intent, picturesFromSP, picVM)
			}
			ComposeTheme {
				NavigationSetup(navController = navController)
			}
		}
	}

	@Composable
	fun NavigationSetup(navController: NavHostController)
	{
		val picVM = picturesViewModel
		val detVM = detailsViewModel
		val picState = picVM.picturesUiState
		NavHost(
			navController = navController,
			startDestination = BottomNavItem.Home.route,
			enterTransition = {
				EnterTransition.None
			},
			exitTransition = {
				ExitTransition.None
			}
		)
		{
			composable(BottomNavItem.Home.route) {
				PicturesScreen(
					navController = navController,
					postPressOnBackButton = { handleBackButtonPressFromPicturesScreen() },
					checkIfExists = { str -> picVM.checkOnErrorExists(str) },
					addError = { str -> picVM.addError(str) },
					postState = { useLoadedState, urls -> picVM.postState(useLoadedState, urls) },
					state = picState,
					clearErrors = { picVM.clearErrors() },
					postPositiveState = { detVM.changeVisabilityState(true) },
					currentPicture = { url, index, offset ->
						picVM.clickOnPicture(url, index, offset)
						detVM.isSharedImage(false)
					},
					isValidUrl = { url -> picVM.isValidUrl(url) },
					postSavedUrls = { urls ->
						if(!detVM.uiState.value.isSharedImage)
						{
							picVM.postSavedUrls(urls = urls, caseEmptySharedPreferenceOnFirstLaunch = false)
						}
						else
						{
							Unit
						}
					},
					saveToSharedPrefs = { urls -> saveToSharedPrefs(urls) }
				)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(
					navController = navController,
					option = themePick,
					changeTheme = { int -> changeTheme(int) },
					isScreenInPortraitState = picState,
					clearImageCache = { saveToSharedPrefs("") },
					postStartOfPager = { picVM.clickOnPicture("", 0, 0) }
				)
			}
			composable(Screen.Details.route) {
				DetailsScreen(
					navController = navController,
					checkIfExists = { str -> picVM.checkOnErrorExists(str) },
					addError = { str -> picVM.addError(str) },
					state = detVM.uiState,
					removeSpecialError = { str -> picVM.removeSpecialError(str) },
					postUrl = { url, bitmap -> detVM.postNewPic(url, bitmap) },
					postPositiveState = { detVM.changeVisabilityState(true) },
					picturesScreenState = picState,
					isValidUrl = { url -> picVM.isValidUrl(url) },
					changeBarsVisability = { visability -> changeBarsVisability(visability, true) },
					postNewBitmap = { url -> detVM.postImageBitmap(url) },
					saveCurrentPictureUrl = { url -> picVM.saveCurrentPictureUrl(url) },
					postFalseToSharedImageState = { detVM.isSharedImage(false) },
					removeUrl = { url -> picVM.removeUrlFromSavedUrls(url) },
					saveToSharedPrefs = { urls ->
						saveToSharedPrefs(urls)
						picVM.clearUsedIntentValue()
					},
					clearPrevIntent = { picVM.clearUsedIntentValue() },
					changeAddedState = { wasAdded -> detVM.changeAddedState(wasAdded) },
				)
			}
		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray,
	)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if(requestCode == RESULT_SUCCESS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
		{
			startMainService()
		}
	}

	override fun onRestart()
	{
		Log.d("lifecycle", "onRestart()")
		val picVM = picturesViewModel
		val pic = picVM.picturesUiState.value.currentPicture
		caseSharedImageExit { picVM.restoreDeletedUrl(pic) }
		super.onRestart()
	}

	override fun onResume()
	{
		val value = detailsViewModel.uiState.value.barsAreVisible
		if(!value)
		{
			changeBarsVisability(visible = false, fromDetailsScreen = false)
			Log.d("bars", "change visability to false")
		}
		if(mainNotificationService == null)
		{
			Log.d("service", "starting service from onResume()")
			startMainService()
		}
		Log.d("lifecycle", "onResume()")
		super.onResume()
	}

	private fun handleBackButtonPressFromPicturesScreen()
	{
		Log.d("callback", "callback was called")
		mainNotificationService?.stopSelf()
		this.finish()
	}

	override fun onPause()
	{
		picturesViewModel.onPauseWasCalled = true
		Log.d("lifecycle", "onPause()")
		super.onPause()
	}

	override fun onStop()
	{
		if(mainNotificationService != null)
		{
			unbindMainService()
		}
		newIntentFlag = false
		val picVM = picturesViewModel
		caseSharedImageExit { picVM.removeUrlFromSavedUrls(picVM.picturesUiState.value.currentPicture) }
		Log.d("lifecycle", "onStop()")
		super.onStop()
	}

	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		super.onDestroy()
	}

	private fun changeBarsVisability(visible: Boolean, fromDetailsScreen: Boolean)
	{
		val detVM = detailsViewModel
		val window = window
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		if(visible)
		{
			controller.show(WindowInsetsCompat.Type.systemBars())
		}
		else
		{
			controller.hide(WindowInsetsCompat.Type.systemBars())
		}
		if(fromDetailsScreen)
		{
			detVM.changeVisabilityState(visible)
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		detailsViewModel.changeMultiWindowState(isInMultiWindowMode || isInPictureInPictureMode)
		val orientation = newConfig.orientation
		val picVM = picturesViewModel
		picVM.changeOrientation(orientation != Configuration.ORIENTATION_LANDSCAPE)
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

	private fun startMainService()
	{
		if(mainNotificationService == null)
		{
			val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
			val connectionLocal = connection
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
				else if(!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) && !picturesViewModel.onPauseWasCalled)
				{
					requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), RESULT_SUCCESS)
				}
			}
			else
			{
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				{
					startForegroundService(serviceIntentLocal)
				}
				else
				{
					startService(serviceIntentLocal)
				}
				bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
			}
		}
	}

	private fun unbindMainService()
	{
		if(mainNotificationService != null)
		{
			Log.d("service", "unBind was called in main")
			unbindService(connection)
			mainNotificationService = null
		}
	}

	override fun onNewIntent(intent: Intent?)
	{
		super.onNewIntent(intent)
		newIntentFlag = true
		intent?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		getValuesFromIntent(intent)
		setIntent(intent)
	}

	private fun getValuesFromIntent(intent: Intent?)
	{
		Log.d("service", "newIntent was called")
		val picVM = picturesViewModel
		val urls = picVM.picturesUiState.value.picturesUrl
		postValuesFromIntent(intent, urls, picVM)
	}

	private fun postValuesFromIntent(intent: Intent?, picUrls: String?, picVM: PicturesViewModel)
	{
		val action = intent?.action
		if(intent != null && action == Intent.ACTION_SEND && TEXT_PLAIN == intent.type)
		{
			newIntentFlag = true
			val usedIntentValue = picVM.usedValueFromIntent
			var urls = picUrls ?: ""
			val nav = navigation
			val detVM = detailsViewModel
			val uiStateValue = detVM.uiState.value
			val sharedValue = intent.getStringExtra(Intent.EXTRA_TEXT)
			if(!sharedValue.isNullOrEmpty())
			{
				val cacheIsEmpty = urls.isEmpty()
				if(!cacheIsEmpty)
				{
					if(uiStateValue.isSharedImage && uiStateValue.wasAddedAfterSharing != true)
					{
						picVM.putPreviousPictureCorrectly(usedIntentValue)
						urls = picVM.picturesUiState.value.picturesUrl
					}
					if(urls.contains(sharedValue))
					{
						picVM.urlWasAlreadyInSP(sharedValue, urls)
					}
				}
				detVM.changeAddedState(null)
				picVM.postSavedUrls(urls = "$sharedValue\n$urls", caseEmptySharedPreferenceOnFirstLaunch = cacheIsEmpty)
				picVM.saveCurrentPictureUrl(sharedValue)
				Log.d("we changed current picture", "$sharedValue")
				detVM.isSharedImage(true)
				picVM.postUsedIntent(sharedValue)
				nav.navigate(Screen.Details.route)
			}
			else
			{
				val oldString = intent.getStringExtra(SAVED_URL_FROM_SCREEN_DETAILS)
				Log.d("OldString get", "$oldString")
				if(!oldString.isNullOrEmpty() && urls.contains(oldString) && oldString != usedIntentValue)
				{
					Log.d("OldString get22", "$oldString")
					picVM.clickOnPicture(oldString, 0, 0)
					nav.navigate(Screen.Details.route)
					picVM.postUsedIntent(oldString)
				}
			}
		}
	}

	private fun caseSharedImageExit(shouldDo: () -> Unit)
	{
		if(detailsViewModel.uiState.value.isSharedImage && !newIntentFlag)
		{
			shouldDo()
		}
	}

	private fun saveToSharedPrefs(picturesUrl: String)
	{
		val sharedPreferencesPictures = this.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		val editorPictures = sharedPreferencesPictures.edit()
		editorPictures.putString(SHARED_PREFS_PICTURES, picturesUrl)
		editorPictures.apply()
	}

	companion object
	{
		const val RESULT_SUCCESS = 100
		const val LENGTH_OF_PICTURE = 110
		const val TEXT_PLAIN = "text/plain"
		const val NOTIFICATION_ID = 1337
		const val SHARED_PREFS_PICTURES = "SHARED_PREFS_PICTURES"
		const val THEME_SHARED_PREFERENCE = "THEME_SHARED_PREFERENCE"
		const val CHANNEL_NOTIFICATIONS_ID = "GRID_PICS_CHANEL_NOTIFICATIONS_ID"
		const val SHARED_PREFERENCE_GRIDPICS = "SHARED_PREFERENCE_GRIDPICS"
		const val DEFAULT_STRING_VALUE = "default"
		const val HTTP_ERROR = "HTTP error: 404"
		const val SAVED_URL_FROM_SCREEN_DETAILS = "SAVED_URL_FROM_SCREEN_DETAILS"
	}
}
