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
	private var job: Job? = null
	private val connection = object: ServiceConnection
	{
		override fun onServiceConnected(className: ComponentName, service: IBinder)
		{
			val binder = service as MainNotificationService.ServiceBinder
			val mainService = binder.get()
			val flowValue = detailsViewModel.observeUrlFlow().value
			if(flowValue?.first != null)
			{
				flowValue.let { mainService.putValues(it) }
			}
			mainNotificationService = mainService
			Log.d("debug lifecycle", "onServiceConnected() $mainService")
			Log.d("debug lifecycle", "activity onServiceConnected() $this")
		}

		override fun onServiceDisconnected(arg0: ComponentName)
		{
			mainNotificationService = null
			Log.d("debug lifecycle", "activity onServiceDisconnected() $this")
		}

		override fun onBindingDied(name: ComponentName?)
		{
			mainNotificationService = null
		}
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		Log.d("debug lifecycle", "onCreate() $this")
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		val picVM = picturesViewModel
		val detVM = detailsViewModel

		picVM.changeOrientation(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		// Здесь происходит получение всех кэшированных картинок,точнее их url,
		// чтобы их можно было "достать" из кэша и отобразить с помощью библиотеки Coil
		val picturesFromSP = sharedPreferences.getString(SHARED_PREFS_PICTURES, null)
		val listOfUrls = picVM.convertToListFromString(picturesFromSP)
		picVM.postSavedUrls(urls = listOfUrls)
		if(detVM.uiState.value.picturesUrl.isEmpty())
		{
			detVM.firstSetOfListState(listOfUrls)
		}
		// Здесь мы получаем значение выбранной через настройки приложения темы раннее, чтобы приложение сразу её выставило
		val theme = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		changeTheme(theme)
		val blackColor = getColor(R.color.black)
		val whiteColor = getColor(R.color.white)
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(whiteColor, blackColor),
			navigationBarStyle = SystemBarStyle.auto(whiteColor, blackColor)
		)
		lifecycleScope.launch {
			detVM.observeUrlFlow().collect {
				if(ContextCompat.checkSelfPermission(
						this@MainActivity,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED)
				{
					Log.d("service", "data $it")
					it?.let { pair -> mainNotificationService?.putValues(pair) }
				}
			}
		}
		setContent {
			val navController = rememberNavController()
			LaunchedEffect(Unit) {
				navigation = navController
				postValuesFromIntent(intent, listOfUrls, picVM)
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
		)
		{
			composable(BottomNavItem.Home.route) {
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
					},
					isValidUrl = { url -> picVM.isValidUrl(url) },
					postSavedUrls = { urls ->
						picVM.postSavedUrls(urls = urls)
						detVM.firstSetOfListState(urls)
					},
					saveToSharedPrefs = { urls ->
						saveToSharedPrefs(picVM.convertFromListToString(urls))
					}
				)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(
					navController = navController,
					option = picVM.picturesUiState,
					changeTheme = { int -> changeTheme(int) },
					isScreenInPortraitState = picState,
					clearImageCache = {
						val imageLoader = this@MainActivity.imageLoader
						imageLoader.diskCache?.clear()
						imageLoader.memoryCache?.clear()
						picVM.clearErrors()
					},
					postStartOfPager = { picVM.clickOnPicture(0, 0) }
				)
			}
			composable(Screen.Details.route) {
				DetailsScreen(
					navController = navController,
					getErrorMessageFromErrorsList = { url -> picVM.checkOnErrorExists(url) },
					addError = { url, message -> picVM.addError(url, message) },
					state = detVM.uiState,
					removeError = { str -> picVM.removeSpecialError(str) },
					postUrl = { url, bitmap -> detVM.postNewPic(url, bitmap) },
					isValidUrl = { url -> picVM.isValidUrl(url) },
					changeBarsVisability = { visability -> changeBarsVisability(visability, true) },
					postNewBitmap = { url -> detVM.postImageBitmap(url) },
					addPicture = { url ->
						picVM.addPictureToUrls(url)
						saveToSharedPrefs(picVM.returnStringOfList())
						Log.d("fuafahfafafa", "details")
					},
					setImageSharedState = { isShared -> detVM.isSharedImage(isShared) },
					picsUiState = picVM.picturesUiState,
					setCurrentPictureUrl = { url -> detVM.postCurrentPicture(url) },
					share = { url -> share(url) }
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
		Log.d("debug lifecycle", "onRestart() $this")
		super.onRestart()
	}

	override fun onStart()
	{
		if(mainNotificationService == null)
		{
			Log.d("service", "starting service from onResume()")
			startMainService()
		}
		Log.d("debug lifecycle", "onStart $this()")
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
		Log.d("debug lifecycle", "onStop() $this")
		super.onStop()
	}

	override fun onDestroy()
	{
		val intent = intent
		intent.replaceExtras(Bundle())
		intent.action = ""
		intent.data = null
		intent.flags = 0
		Log.d("debug lifecycle", "onDestroy() $this")
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
	}

	private fun changeTheme(option: Int)
	{
		Log.d("debug theme option", "changed theme option: $option")
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
			picturesViewModel.listOfConnections.add(connectionLocal)
		}
	}

	private fun unbindMainService()
	{
		if(mainNotificationService != null)
		{
			Log.d("service", "unBind was called in main")
			val connections = picturesViewModel.listOfConnections
			for(i in 0 .. connections.lastIndex)
			{
				try
				{
					unbindService(connections[i])
				}
				catch(e: Exception)
				{
					Log.d("Error", "Connection was already dead")
				}
			}
			connections.clear()
			Log.d("service", "connection $connection")
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
					picVM.clickOnPicture(0, 0)
					navToDetailsAfterNewIntent(nav)
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
				navToDetailsAfterNewIntent(nav)
			}
		}
	}

	private fun navToDetailsAfterNewIntent(nav: NavHostController?)
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
		const val HTTP_ERROR = "HTTP error: 404, or bad image"
		const val SAVED_URL_FROM_SCREEN_DETAILS = "SAVED_URL_FROM_SCREEN_DETAILS"
	}
}
