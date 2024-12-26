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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var themePick: Int = ThemePick.FOLLOW_SYSTEM.intValue
	private var mainNotificationService: MainNotificationService? = null
	private var newIntentFlag = true
	private var navigation: NavHostController? = null
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
		picVM.postSavedUrls(urls = picturesFromSP)
		// Здесь мы получаем значение выбранной через настройки приложения темы раннее, чтобы приложение сразу её выставило
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
		setContent {
			val navController = rememberNavController()
			LaunchedEffect(Unit) {
				navigation = navController
				newIntentFlag = true
				if(picVM.usedValueFromIntent.isEmpty())
				{
					postValuesFromIntent(intent, picturesFromSP, picVM)
				}
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
							picVM.postSavedUrls(urls = urls)
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
					option = picVM.themeState,
					changeTheme = { int -> changeTheme(int) },
					isScreenInPortraitState = picState,
					clearImageCache = {
						saveToSharedPrefs("")
						picVM.clearErrors()
					},
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
					postFalseToSharedImageState = { url ->
						picVM.addPictureToUrls(url)
						detVM.isSharedImage(false)
						picVM.clearUsedIntentValue()
					},
					removeUrl = { url ->
						picVM.removeUrlFromSavedUrls(url)
						detVM.changeAddedState(null)
					},
					saveToSharedPrefs = { urls ->
						saveToSharedPrefs(urls)
					},
					changeAddedState = { wasAdded -> detVM.changeAddedState(wasAdded) },
					postIsFirstPage = { isFirstPage -> picVM.postIsFirstPage(isFirstPage) }
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
		picturesViewModel.postOnPauseWasCalled(true)
		Log.d("lifecycle", "onPause()")
		super.onPause()
	}

	override fun onStop()
	{
		if(mainNotificationService != null)
		{
			unbindMainService()
		}
		Log.d("lifecycle", "onStop()")
		super.onStop()
	}

	override fun onDestroy()
	{
		intent.replaceExtras(Bundle())
		intent.action = ""
		intent.data = null
		intent.flags = 0
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
		val picVM = picturesViewModel
		when(option)
		{
			ThemePick.LIGHT_THEME.intValue ->
			{
				picVM.postThemePick(ThemePick.LIGHT_THEME)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			}
			ThemePick.DARK_THEME.intValue ->
			{
				picVM.postThemePick(ThemePick.DARK_THEME)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
			}
			ThemePick.FOLLOW_SYSTEM.intValue ->
			{
				picVM.postThemePick(ThemePick.FOLLOW_SYSTEM)
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
				else if(!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) && !picturesViewModel.getOnPauseWasCalled())
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
		//реализация фичи - поделиться картинкой в приложение
		val action = intent?.action
		if(intent != null && action == Intent.ACTION_SEND && TEXT_PLAIN == intent.type)
		{
			newIntentFlag = true
			val usedIntentValue = picVM.getUsedIntentValue()
			val urls = picUrls ?: ""
			val nav = navigation
			val detVM = detailsViewModel
			//val uiStateValue = detVM.uiState.value
			//val isSharedImage = uiStateValue.isSharedImage
			val sharedValue = intent.getStringExtra(Intent.EXTRA_TEXT)
			val picState = picVM.picturesUiState.value
			picVM.removeUrlFromSavedUrls(picState.currentPicture)
			if(!sharedValue.isNullOrEmpty())
			{
				val cacheIsEmpty = urls.isEmpty()
				if(!cacheIsEmpty)
				{
					if(urls.contains(sharedValue))
					{
						picVM.urlWasAlreadyInSP(sharedValue, urls)
					}
					else
					{
						picVM.clearIndex()
					}
				}
				picVM.postSavedUrls(urls)
				detVM.changeAddedState(null)
				picVM.saveCurrentPictureUrl(sharedValue)
				detVM.isSharedImage(true)
				picVM.postUsedIntent(sharedValue)
				navToDetailsAfterNewIntent(nav)
			}
			else
			{
				val oldString = intent.getStringExtra(SAVED_URL_FROM_SCREEN_DETAILS)
				if(!oldString.isNullOrEmpty() && urls.contains(oldString) && oldString != usedIntentValue)
				{
					picVM.clickOnPicture(oldString, 0, 0)
					navToDetailsAfterNewIntent(nav)
					picVM.postUsedIntent(oldString)
				}
			}
		}
	}

	private fun navToDetailsAfterNewIntent(nav: NavHostController?)
	{
		var navv = nav
		if(navv == null)
		{
			lifecycleScope.launch(Dispatchers.IO) {
				while(navv == null)
				{
					delay(100)
					navv = navigation
				}
				runOnUiThread {
					navv?.navigate(Screen.Details.route)
				}
			}
		}
		else
		{
			navv?.navigate(Screen.Details.route)
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
