package com.example.gridpics.ui.activity

import android.Manifest
import android.app.UiModeManager
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
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import coil3.imageLoader
import com.example.gridpics.R
import com.example.gridpics.ui.details.DetailsScreen
import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesScreen
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.service.MainNotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.ThemePick
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var mainNotificationService: MainNotificationService? = null
	private var navigation: NavHostController? = null
	private var themePick: Int = 2
	private var job: Job? = null
	private val connection = object: ServiceConnection
	{
		override fun onServiceConnected(className: ComponentName, service: IBinder)
		{
			val binder = service as MainNotificationService.ServiceBinder
			val mainService = binder.get()
			if(this@MainActivity.isDestroyed)
			{
				unbindService(this)
			}
			else
			{
				val flowValue = detailsViewModel.observeUrlFlow().value
				if(flowValue.url != null)
				{
					flowValue.let { mainService.putValues(it) }
				}
				mainNotificationService = mainService
			}
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
		val resources = resources
		val orientation = resources.configuration.orientation
		picVM.changeOrientation(isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT)
		picVM.orientationWasChanged.value = false
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		// Здесь происходит получение всех кэшированных картинок,точнее их url,
		// чтобы их можно было "достать" из кэша и отобразить с помощью библиотеки Coil
		val picturesFromSP = sharedPreferences.getString(SHARED_PREFS_PICTURES, null)
		val deletedPictures = picVM.convertToListFromString(sharedPreferences.getString(DELETED_LIST, null))
		val listOfUrls = picVM.convertToListFromString(picturesFromSP)
		picVM.postSavedUrls(urls = listOfUrls)
		picVM.postDeletedUrls(urls = deletedPictures)
		if(detVM.uiState.value.picturesUrl.isEmpty())
		{
			detVM.firstSetOfListState(listOfUrls)
		}
		// Здесь мы получаем значение выбранной через настройки приложения темы раннее, чтобы приложение сразу её выставило
		val theme = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		changeTheme(theme)
		themePick = theme

		lifecycleScope.launch {
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
		picVM.updateGridSpan(calculateGridSpan())
		setContent {
			val navController = rememberNavController()
			LaunchedEffect(Unit) {
				navigation = navController
				postValuesFromIntent(intent, listOfUrls, picVM)
			}
			ComposeTheme(isDarkTheme = picVM.mutableIsThemeBlackState.value) {
				NavigationSetup(navController = navController)
			}
		}
	}

	@OptIn(ExperimentalSharedTransitionApi::class)
	@Composable
	fun NavigationSetup(navController: NavHostController)
	{
		val picVM = picturesViewModel
		val detVM = detailsViewModel
		val picState = picVM.picturesUiState
		val detailsState = detVM.uiState
		SharedTransitionLayout {
			NavHost(
				navController = navController,
				startDestination = BottomNavItem.Home.route,
			)
			{
				composable(
					route = BottomNavItem.Home.route,
				) {
					detVM.postNewPic(null, null)
					PicturesScreen(
						navController = navController,
						postPressOnBackButton = { handleBackButtonPressFromPicturesScreen() },
						getErrorMessageFromErrorsList = { str -> picVM.checkOnErrorExists(str) },
						addError = { url, message -> picVM.addError(url, message) },
						state = picState,
						clearErrors = { picVM.clearErrors() },
						postVisibleBarsState = { detVM.changeVisabilityState(true) },
						currentPicture = { url, index, offset ->
							picVM.clickOnPicture(index, offset)
							detVM.postCurrentPicture(url)
							navController.navigate(Screen.Details.route)
						},
						isValidUrl = { url -> picVM.isValidUrl(url) },
						postSavedUrls = { urls ->
							picVM.postSavedUrls(urls = urls)
							detVM.firstSetOfListState(urls)
						},
						saveToSharedPrefs = { urls ->
							saveToSharedPrefs(picVM.convertFromListToString(urls))
						},
						calculateGridSpan = { picVM.getGridSpan() },
						postMaxVisibleLinesNum = { maxVisibleLinesNum -> picVM.postMaxVisibleLinesNum(maxVisibleLinesNum) },
						animatedVisibilityScope = this@composable,
					)
				}
				composable(
					route = BottomNavItem.Settings.route,
					enterTransition = { null },
					exitTransition = { null }
				) {
					SettingsScreen(
						navController = navController,
						option = picState,
						changeTheme = { int -> changeTheme(int) },
						isScreenInPortraitState = picState,
						clearImageCache = {
							val imageLoader = this@MainActivity.imageLoader
							imageLoader.diskCache?.clear()
							imageLoader.memoryCache?.clear()
							picVM.clearErrors()
						},
						postStartOfPager = { picVM.clickOnPicture(0, 0) },
					)
				}
				composable(
					route = Screen.Details.route,
				) {
					DetailsScreen(
						navController = navController,
						getErrorMessageFromErrorsList = { url -> picVM.checkOnErrorExists(url) },
						addError = { url, message -> picVM.addError(url, message) },
						state = detailsState,
						removeError = { str -> picVM.removeSpecialError(str) },
						postUrl = { url, bitmap -> detVM.postNewPic(url, bitmap) },
						isValidUrl = { url -> picVM.isValidUrl(url) },
						changeBarsVisability = { visability -> changeBarsVisability(visability, true) },
						postNewBitmap = { url, strLoadingFromResources -> detVM.postImageBitmap(url, strLoadingFromResources) },
						addPicture = { url ->
							picVM.addPictureToUrls(url)
							picVM.clickOnPicture(0, 0)
							saveToSharedPrefs(picVM.returnStringOfList())
						},
						setImageSharedState = { isShared ->
							detVM.isSharedImage(isShared)
						},
						setCurrentPictureUrl = { url ->
							detVM.postCurrentPicture(url)
							picVM.postCurrentPicture(url)
						},
						share = { url -> share(url) },
						deleteCurrentPicture = { url ->
							deletePicture(url)
							detVM.postNewPic(null, null)
						},
						postWasSharedState = { detVM.setWasSharedFromNotification(false) },
						setFalseToWasDeletedFromNotification = { detVM.setWasDeletedFromNotification(false) },
						animatedVisibilityScope = this@composable
					)
				}
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

	override fun onStart()
	{
		if(mainNotificationService == null)
		{
			Log.d("service", "starting service from onResume()")
			startMainService()
		}
		Log.d("lifecycle", "onStart()")
		super.onStart()
	}

	override fun onResume()
	{
		val value = detailsViewModel.uiState.value.barsAreVisible
		if(!value)
		{
			changeBarsVisability(visible = false, fromDetailsScreen = false)
			Log.d("bars", "change visability to false")
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
		val editor = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE).edit()
		editor.apply()
		val intent = intent
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
		picVM.changeOrientation(orientation == Configuration.ORIENTATION_PORTRAIT)
		picVM.updateGridSpan(calculateGridSpan())
		val followSysTheme = ThemePick.FOLLOW_SYSTEM.intValue
		if(themePick == followSysTheme)
		{
			Log.d("themka pomenyalas", "poshlo-poehalo")
			changeTheme(followSysTheme)
		}
	}

	private fun changeTheme(option: Int)
	{
		Log.d("theme option", "theme option: $option")
		val picVM = picturesViewModel
		var isDarkTheme = false
		when(option)
		{
			ThemePick.LIGHT_THEME.intValue ->
			{
				picVM.postThemePick(ThemePick.LIGHT_THEME)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
				isDarkTheme = false
			}
			ThemePick.DARK_THEME.intValue ->
			{
				picVM.postThemePick(ThemePick.DARK_THEME)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				isDarkTheme = true
			}
			ThemePick.FOLLOW_SYSTEM.intValue ->
			{
				picVM.postThemePick(ThemePick.FOLLOW_SYSTEM)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
				isDarkTheme = isDarkThemeAfterSystemChangedTheme()
			}
		}
		themePick = option
		val blackColor = getColor(R.color.black)
		val whiteColor = getColor(R.color.white)
		picVM.mutableIsThemeBlackState.value = isDarkTheme
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(lightScrim = whiteColor, darkScrim = blackColor, detectDarkMode = { isDarkTheme }),
			navigationBarStyle = SystemBarStyle.auto(lightScrim = whiteColor, darkScrim = blackColor, detectDarkMode = { isDarkTheme })
		)
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
				else if(!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
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
		val picVM = picturesViewModel
		val urls = picVM.picturesUiState.value.picturesUrl
		postValuesFromIntent(intent, urls, picVM)
	}

	private fun postValuesFromIntent(intent: Intent?, picUrls: List<String>, picVM: PicturesViewModel)
	{
		//реализация фичи - поделиться картинкой в приложение
		val action = intent?.action
		if(intent != null && action == Intent.ACTION_SEND && TEXT_PLAIN == intent.type)
		{
			val nav = navigation
			val detVM = detailsViewModel
			val sharedValue = intent.getStringExtra(Intent.EXTRA_TEXT)
			if(sharedValue.isNullOrEmpty())
			{
				val oldString = intent.getStringExtra(SAVED_URL_FROM_SCREEN_DETAILS)
				if(!oldString.isNullOrEmpty() && picUrls.contains(oldString))
				{
					val needsToBeShared = intent.getBooleanExtra(SHOULD_WE_SHARE_THIS, false)
					if(needsToBeShared)
					{
						Log.d("Test111", "SHARE")
						picVM.clickOnPicture(0, 0)
						detVM.setWasSharedFromNotification(true)
						nav?.popBackStack()
					}
					else if(intent.getBooleanExtra(SHOULD_WE_DELETE_THIS, false))
					{
						Log.d("Test111", "DELETE")
						detVM.setWasDeletedFromNotification(true)
						nav?.popBackStack()
					}
					else
					{
						picVM.clickOnPicture(0, 0)
					}
					detVM.postCurrentPicture(oldString)
					navAfterNewIntent(nav)
				}
			}
			else
			{
				if(detVM.uiState.value.isSharedImage)
				{
					detVM.firstSetOfListState(picVM.picturesUiState.value.picturesUrl)
				}
				detVM.isSharedImage(true)
				detVM.postCurrentPicture(sharedValue)
				detVM.postCorrectList()
				picVM.clickOnPicture(0, 0)
				navAfterNewIntent(nav)
			}
		}
	}

	private fun navAfterNewIntent(nav: NavHostController?)
	{
		if(nav == null)
		{
			val jobLocal = job
			if(jobLocal == null || !jobLocal.isActive)
			{
				job = lifecycleScope.launch {
					var navv = nav
					while(navv == null)
					{
						delay(100)
						navv = navigation
					}
					navv.navigate(Screen.Details.route)

					job = null
				}
			}
		}
		else
		{
			nav.navigate(Screen.Details.route)
		}
	}

	private fun saveToSharedPrefs(picturesUrl: String)
	{
		Log.d("saved", "saved to sp all urls")
		val sharedPreferencesPictures = this.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		val editorPictures = sharedPreferencesPictures.edit()
		editorPictures.putString(SHARED_PREFS_PICTURES, picturesUrl)
		editorPictures.apply()
	}

	private fun share(text: String)
	{
		val intent = Intent()
		intent.action = Intent.ACTION_SEND
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
		intent.type = "text/plain"
		intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.you_have_got_share_link_from_gridpics, text))
		val components = arrayOf(ComponentName(this, MainActivity::class.java))
		startActivity(Intent.createChooser(intent, null).putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, components))
	}

	private fun deletePicture(url: String)
	{
		val picVM = picturesViewModel
		val detailsViewModel = detailsViewModel
		val urls = detailsViewModel.deleteCurrentPicture(url)
		saveToSharedPrefs(picVM.convertFromListToString(urls))
		val sharedPreferencesPictures = this.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		val stringOfUrls = sharedPreferencesPictures.getString(DELETED_LIST, "") + "\n" + url
		val editorPictures = sharedPreferencesPictures.edit()
		editorPictures.putString(DELETED_LIST, stringOfUrls)
		editorPictures.apply()
		picVM.postSavedUrls(urls)
		detailsViewModel.setWasDeletedFromNotification(false)
		imageLoader.diskCache?.remove(url)
		Toast.makeText(this@MainActivity, getString(R.string.pic_was_deleted), Toast.LENGTH_SHORT).show()
	}

	private fun isDarkThemeAfterSystemChangedTheme(): Boolean
	{
		val uiModeManager = this.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
		return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
	}

	private fun calculateGridSpan(): Int
	{
		val displayMetrics = this.resources.displayMetrics
		val width = displayMetrics.widthPixels
		val density = displayMetrics.density
		val result = (width / density).toInt() / LENGTH_OF_PICTURE
		return result
	}

	companion object
	{
		const val RESULT_SUCCESS = 100
		const val LENGTH_OF_PICTURE = 110
		const val TEXT_PLAIN = "text/plain"
		const val NOTIFICATION_ID = 1337
		const val SHOULD_WE_SHARE_THIS = "SHOULD_WE_SHARE_THIS"
		const val DELETED_LIST = "DELETED_LIST"
		const val SHOULD_WE_DELETE_THIS = "SHOULD_WE_DELETE_THIS"
		const val SHARED_PREFS_PICTURES = "SHARED_PREFS_PICTURES"
		const val THEME_SHARED_PREFERENCE = "THEME_SHARED_PREFERENCE"
		const val CHANNEL_NOTIFICATIONS_ID = "GRID_PICS_CHANEL_NOTIFICATIONS_ID"
		const val SHARED_PREFERENCE_GRIDPICS = "SHARED_PREFERENCE_GRIDPICS"
		const val HTTP_ERROR = "HTTP error: 404, or bad image"
		const val SAVED_URL_FROM_SCREEN_DETAILS = "SAVED_URL_FROM_SCREEN_DETAILS"
	}
}
