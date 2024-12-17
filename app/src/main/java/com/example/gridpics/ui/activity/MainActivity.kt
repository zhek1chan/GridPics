package com.example.gridpics.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
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
	private var mService: Messenger? = null
	private var serviceIsDead = true
	private var bound: Boolean = false
	private lateinit var navigation: NavHostController
	private val connection = object: ServiceConnection
	{
		override fun onServiceConnected(className: ComponentName, service: IBinder)
		{
			mService = Messenger(service)
			val msg = Message.obtain(null, MESSAGE_SAY_HELLO)
			msg.replyTo = Messenger(object: Handler(Looper.getMainLooper())
			{
				override fun handleMessage(msg: Message)
				{
					when(msg.what)
					{
						SERVICE_IS_DEAD ->
						{
							serviceIsDead = msg.obj as Boolean
						}
					}
				}
			})
			try
			{
				mService?.send(msg)
			}
			catch(e: RemoteException)
			{
				Log.e("MainActivity", "Error sending message: ${e.message}")
			}
			bound = true
		}

		override fun onServiceDisconnected(arg0: ComponentName)
		{
			mService = null
			bound = false
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
		picVM.changeOrientation(this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		// Здесь происходит получение всех кэшированных картинок,точнее их url,
		// чтобы их можно было "достать" из кэша и отобразить с помощью библиотеки Coil
		val picturesFromSP = sharedPreferences.getString(SHARED_PREFS_PICTURES, null)
		picVM.postSavedUrls(urls = picturesFromSP, caseEmptySharedPreferenceOnFirstLaunch = (picturesFromSP == null))
		// Здесь мы получаем значение выбранной темы раннее, чтобы приложение сразу её выставило
		val theme = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		changeTheme(theme)
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(getColor(R.color.white), getColor(R.color.black)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.white), getColor(R.color.black))
		)
		lifeCycScope.launch {
			detVM.observeUrlFlow().collect {
				if(ContextCompat.checkSelfPermission(
						this@MainActivity,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED)
				{
					Log.d("service", "data $it")
					val msg: Message = Message.obtain(null, NOTIFICATION_ID, it)
					mService?.send(msg)
				}
			}
		}
		themePick = theme
		//реализация фичи - поделиться картинкой в приложение
		setContent {
			val navController = rememberNavController()
			LaunchedEffect(Unit) {
				navigation = navController
				val urls = picturesFromSP ?: ""
				postValuesFromIntent(intent, urls, picVM)
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
					postSavedUrls = { urls -> picVM.postSavedUrls(urls = urls, caseEmptySharedPreferenceOnFirstLaunch = false) },
					saveToSharedPrefs = { urls -> saveToSharedPrefs(urls) }
				)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(
					navController = navController,
					option = themePick,
					changeTheme = { int -> changeTheme(int) },
					isScreenInPortraitState = picState
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
					saveToSharedPrefs = { urls -> saveToSharedPrefs(urls) }
				)
			}
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		detailsViewModel.changeMultiWindowState(isInMultiWindowMode || isInPictureInPictureMode)
		requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
		val orientation = newConfig.orientation
		val picVM = picturesViewModel
		picVM.changeOrientation(orientation != Configuration.ORIENTATION_LANDSCAPE)
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
		Log.d("wtf1 resume", picturesViewModel.picturesUiState.value.currentPicture)
		val value = detailsViewModel.uiState.value.barsAreVisible
		if(!value)
		{
			changeBarsVisability(visible = false, fromDetailsScreen = false)
			Log.d("bars", "change visability to false")
		}
		if(!bound)
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
		val msg = Message.obtain(null, KILL_SERVICE)
		mService?.send(msg)
		this@MainActivity.finish()
	}

	override fun onPause()
	{
		Log.d("lifecycle", "onPause()")
		super.onPause()
	}

	override fun onStop()
	{
		Log.d("wtf1 stop", picturesViewModel.picturesUiState.value.currentPicture)
		if(bound)
		{
			unbindMainService()
		}
		Log.d("lifecycle", "onStop()")
		super.onStop()
	}

	override fun onDestroy()
	{
		super.onPause()
		Log.d("wtf1 destroy", picturesViewModel.picturesUiState.value.currentPicture)
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
		if(!bound)
		{
			val connectionLocal = connection
			val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			{
				if(
					ContextCompat.checkSelfPermission(
						this,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED
				)
				{
					launchService(serviceIntentLocal)
					bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
				}
				else if(!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
				{
					requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), RESULT_SUCCESS)
				}
			}
			else
			{
				launchService(serviceIntentLocal)
				bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
			}
			val newMsg: Message = Message.obtain(null, MESSAGE_SAY_HELLO)
			mService?.send(newMsg)
		}
	}

	private fun launchService(serviceIntentLocal: Intent)
	{
		if(serviceIsDead)
		{
			Log.d("message", "recreating service")
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				startForegroundService(serviceIntentLocal)
			}
			else
			{
				startService(serviceIntentLocal)
			}
		}
	}

	private fun unbindMainService()
	{
		if(bound)
		{
			Log.d("service", "unBind was called in main")
			unbindService(connection)
			bound = false
		}
	}

	override fun onNewIntent(intent: Intent?)
	{
		super.onNewIntent(intent)
		getValuesFromIntent(intent)
		setIntent(intent)
	}

	private fun getValuesFromIntent(intent: Intent?)
	{
		val action = intent?.action
		if(action == Intent.ACTION_SEND)
		{
			Log.d("service", "newIntent was called")
			val picVM = picturesViewModel
			val urls = picVM.picturesUiState.value.picturesUrl
			postValuesFromIntent(intent, urls, picVM)
		}
	}

	private fun postValuesFromIntent(intent: Intent?, urls: String, picVM: PicturesViewModel)
	{
		if(intent != null)
		{
			val action = intent.action
			var sharedLinkLocal = ""
			val nav = navigation
			if(action == Intent.ACTION_SEND && TEXT_PLAIN == intent.type && !intent.getStringExtra(Intent.EXTRA_TEXT).isNullOrEmpty())
			{
				intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
					sharedLinkLocal = it
				}
				if(urls.contains(sharedLinkLocal))
				{
					picVM.removeUrlFromSavedUrls(sharedLinkLocal)
				}
				picVM.postSavedUrls(urls = "$sharedLinkLocal\n$urls", caseEmptySharedPreferenceOnFirstLaunch = urls.isEmpty())
				Log.d("shared", "$action")
				picVM.clickOnPicture(sharedLinkLocal, 0, 0)
				detailsViewModel.isSharedImage(true)
				nav.navigate(Screen.Details.route)
			}
			else
			{
				val oldUrl = intent.getStringExtra(WAS_OPENED_SCREEN)
				Log.d("wtf1 intent", "$oldUrl")
				if(!oldUrl.isNullOrEmpty())
				{
					picVM.clickOnPicture(oldUrl, 0, 0)
					nav.navigate(Screen.Details.route)
				}
			}
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
		const val KILL_SERVICE = 2
		const val MESSAGE_SAY_HELLO = 0
		const val SERVICE_IS_DEAD = 1
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
		const val WAS_OPENED_SCREEN = "WAS_OPENED_SCREEN"
	}
}
