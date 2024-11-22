package com.example.gridpics.ui.activity

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
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
import com.example.gridpics.ui.pictures.PictureState
import com.example.gridpics.ui.pictures.PicturesScreen
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.services.MainNotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.settings.ThemePick
import com.example.gridpics.ui.state.BarsVisabilityState
import com.example.gridpics.ui.state.MultiWindowStateTracker
import com.example.gridpics.ui.state.MultiWindowStateTracker.MultiWindowState
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.collections.set

class MainActivity: AppCompatActivity()
{
	private var isActive: Boolean = false
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var themePick: Int = ThemePick.FOLLOW_SYSTEM.intValue
	private var serviceIntent = Intent()
	private var description = mutableMapOf<String, String>()
	private var state = mutableStateOf<PictureState>(PictureState.NothingFound)
	private var multiWindowState = mutableStateOf<MultiWindowState>(MultiWindowState.NotMultiWindow)
	private var barsState = mutableStateOf<BarsVisabilityState>(BarsVisabilityState.IsVisible)
	private var mainNotificationService = MainNotificationService()
	private var mBound: Boolean = false
	private var changedTheme = false
	private var barsVisabilitySP = true
	private var currentPictureSP = ""
	private var checkIfWeWereHereSP = false
	private var imagesStringUrlsSP: String? = null
	private var allConnections: MutableList<ServiceConnection> = mutableListOf()
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
	}

	@SuppressLint("UseCompatLoadingForDrawables")
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		Log.d("lifecycle", "onCreate()")
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()

		description[DEFAULT_STRING_VALUE] = DEFAULT_STRING_VALUE
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		var getCurrentPictureSP = sharedPreferences.getString(PICTURE, NULL_STRING).toString()
		currentPictureSP = getCurrentPictureSP
		barsVisabilitySP = sharedPreferences.getBoolean(TOP_BAR_VISABILITY_SHARED_PREFERENCE, true)
		checkIfWeWereHereSP = sharedPreferences.getBoolean(WE_WERE_HERE_BEFORE, false)
		imagesStringUrlsSP = sharedPreferences.getString(SHARED_PREFS_PICTURES, null)
		val connectionLocal = connection
		//serviceIntentForNotification
		val serviceIntentLocal = Intent(this, MainNotificationService::class.java)
		serviceIntentLocal.putExtra(DESCRIPTION_NAMING, description.toList().last().toString())
		//get theme pic
		themePick = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, ThemePick.FOLLOW_SYSTEM.intValue)
		//check if theme was changed and activity recreated because of it
		changedTheme = getSharedPreferences(JUST_CHANGED_THEME, MODE_PRIVATE).getBoolean(JUST_CHANGED_THEME, false)
		val previousTheme = sharedPreferences.getString(IS_BLACK_THEME, isDarkTheme(this).toString())
		Log.d("theme", "just changed theme? ")
		//Start showing notification
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && setTheme(previousTheme!!) == Pair(false, true))
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

		picturesViewModel.getPics()
		picturesViewModel.observeState().observeForever {
			state.value = it
		}
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		lifecycleScope.launch {
			detailsViewModel.observeVisabilityFlow().collectLatest {
				if(it == BarsVisabilityState.NotVisible)
				{
					controller.hide(WindowInsetsCompat.Type.statusBars())
					controller.hide(WindowInsetsCompat.Type.navigationBars())
					barsState.value = it
				}
				else
				{
					controller.show(WindowInsetsCompat.Type.statusBars())
					controller.show(WindowInsetsCompat.Type.navigationBars())
					barsState.value = it
				}
			}
		}

		lifecycleScope.launch {
			picturesViewModel.observeBackNav().collectLatest {
				if(it)
				{
					stopService(serviceIntent)
					this@MainActivity.finishAffinity()
				}
			}
		}

		lifecycleScope.launch {
			detailsViewModel.observeUrlFlow().collectLatest {
				if(ContextCompat.checkSelfPermission(
						this@MainActivity,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED)
				{
					if(description != it && it.isNotEmpty()) //for optimization
					{
						Log.d("description in main", description.toString())
						serviceIntentLocal.putExtra(DESCRIPTION_NAMING, it.toList().last().toString())
						serviceIntent = serviceIntentLocal
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
						{
							startForegroundService(serviceIntentLocal)
							bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
							allConnections.add(connectionLocal)
						}
						else
						{
							startService(serviceIntentLocal)
							bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
							allConnections.add(connectionLocal)
						}
						description = it as MutableMap<String, String>
					}
				}
			}
		}

		lifecycleScope.launch {
			picturesViewModel.observeCurrentImg().collectLatest {
				getCurrentPictureSP = it
				currentPictureSP = getCurrentPictureSP
			}
		}
		val editorSharedPrefs = sharedPreferences.edit()
		editorSharedPrefs.putBoolean(JUST_CHANGED_THEME, false)
		editorSharedPrefs.putString(IS_BLACK_THEME, isDarkTheme(this).toString())
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
						postState = { str -> picVM.postState(str) },
						state = state,
						clearErrors = { picVM.clearErrors() },
						postPositiveState = { detVM.postPositiveVisabilityState() },
						postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE) },
						resume = { picVM.resume() },
						newState = { picVM.newState() },
						sharedPrefsPictures = imagesStringUrlsSP,
						clearedCache = changedTheme,
						currentPicture = { url -> picVM.clickOnPicture(url) }
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
						state = barsState,
						removeSpecialError = { str -> picVM.removeSpecialError(str) },
						postDefaultUrl = { detVM.postNewPic(DEFAULT_STRING_VALUE, DEFAULT_STRING_VALUE) },
						changeVisabilityState = { detVM.changeVisabilityState() },
						postUrl = { str, p -> detVM.postNewPic(str, p) },
						postPositiveState = { detVM.postPositiveVisabilityState() },
						multiWindowState = multiWindowState,
						pictures = imagesStringUrlsSP,
						pic = currentPictureSP,
						visibility = barsVisabilitySP,
						weWereHere = checkIfWeWereHereSP
					)
				}
			}
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration)
	{
		super.onConfigurationChanged(newConfig)
		val multiWindowStateTracker = MultiWindowStateTracker(this)
		multiWindowStateTracker.multiWindowState.observe(this) { newState ->
			multiWindowState.value = newState
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
					allConnections.add(connectionLocal)
				}
				else
				{
					startService(serviceIntentLocal)
					bindService(serviceIntentLocal, connectionLocal, Context.BIND_AUTO_CREATE)
					allConnections.add(connectionLocal)
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
			newIntent.putExtra(DESCRIPTION_NAMING, description.toList().last().toString())
			serviceIntent = newIntent
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
		isActive = true
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
		allConnections.clear()
		countExitNavigation++
		isActive = false
		val vis = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		val editorVis = vis.edit()
		editorVis.putBoolean(WE_WERE_HERE_BEFORE, false)
		editorVis.apply()
		super.onPause()
	}

	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		super.onDestroy()
	}

	private fun isDarkTheme(context: Context): Boolean
	{
		return context.resources.configuration.uiMode and
			Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
	}

	private fun setTheme(prev: String): Pair<Boolean, Boolean>
	{
		settingsViewModel.changeTheme(themePick)
		val justChangedTheme = if(themePick == ThemePick.FOLLOW_SYSTEM.intValue)
		{
			changedTheme
		}
		else
		{
			isDarkTheme(this)
		}
		val darkThemeIsActive = isDarkTheme(this).toString()
		return Pair(justChangedTheme, prev == darkThemeIsActive)
	}

	companion object
	{
		val jobForNotification = Job()
		var countExitNavigation = 0
		const val NOTIFICATION_ID = 1337
		const val CACHE = "CACHE_CACHE"
		const val SHARED_PREFS_PICTURES = "SHARED_PREFS_PICTURES"
		const val PICTURE = "PICTURE"
		const val THEME_SHARED_PREFERENCE = "THEME_SHARED_PREFERENCE"
		const val CHANNEL_NOTIFICATIONS_ID = "GRID_PICS_CHANEL_NOTIFICATIONS_ID"
		const val NULL_STRING = "NULL_STRING"
		const val IS_BLACK_THEME = "IS_BLACK_THEME"
		const val TOP_BAR_VISABILITY_SHARED_PREFERENCE = "TOP_BAR_VISABILITY_SHARED_PREFERENCE"
		const val WE_WERE_HERE_BEFORE = "WE_WERE_HERE_BEFORE"
		const val JUST_CHANGED_THEME = "JUST_CHANGED_THEME"
		const val DESCRIPTION_NAMING = "description"
		const val SHARED_PREFERENCE_GRIDPICS = "SHARED_PREFERENCE_GRIDPICS"
		const val DEFAULT_STRING_VALUE = "default"
		const val HTTP_ERROR = "HTTP error: 404"
	}
}
