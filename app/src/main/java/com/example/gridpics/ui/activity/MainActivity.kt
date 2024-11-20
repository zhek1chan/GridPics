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
import com.example.gridpics.ui.state.BarsVisabilityState
import com.example.gridpics.ui.state.MultiWindowState
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private var isActive: Boolean = false
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var picturesSharedPrefs: String? = null
	private var themePick: Int = 2
	private var serviceIntent = Intent()
	private var description = DEFAULT_STRING_VALUE
	private var job = jobForNotification
	private var bitmapString = ""
	private var state = mutableStateOf<PictureState>(PictureState.NothingFound)
	private var multiWindowState = mutableStateOf<MultiWindowState>(MultiWindowState.NotMultiWindow)
	private var barsState = mutableStateOf<BarsVisabilityState>(BarsVisabilityState.IsVisible)
	private var mainNotificationService = MainNotificationService()
	private var mBound: Boolean = false
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
		val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		//serviceIntentForNotification
		serviceIntent = Intent(this, MainNotificationService::class.java)
		serviceIntent.putExtra(DESCRIPTION_NAMING, description)
		//get theme pic
		themePick = sharedPreferences.getInt(THEME_SHARED_PREFERENCE, 2)
		setTheme()
		//check if theme was changed and activity recreated because of it
		val justChangedTheme = if(themePick == 2)
		{
			getSharedPreferences(JUST_CHANGED_THEME, MODE_PRIVATE).getBoolean(JUST_CHANGED_THEME, false)
		}
		else
		{
			isDarkTheme(this)
		}
		val darkThemeIsActive = isDarkTheme(this).toString()
		val previousTheme = sharedPreferences.getString(IS_BLACK_THEME, darkThemeIsActive)
		Log.d("theme", "just changed theme? $justChangedTheme")
		//Start showing notification
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !justChangedTheme && (previousTheme == darkThemeIsActive))
		{
			if(
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED
			)
			{
				startForegroundService(serviceIntent)
				bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
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
				startForegroundService(serviceIntent)
				bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
			}
			else
			{
				startService(serviceIntent)
				bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
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

		picturesSharedPrefs = sharedPreferences.getString(SHARED_PREFS_PICTURES, null)
		CoroutineScope(Dispatchers.Default).launch {
			detailsViewModel.observeBitmapFlow().collectLatest { b ->
				bitmapString = b
			}
		}

		lifecycleScope.launch(Dispatchers.IO) {
			detailsViewModel.observeUrlFlow().collectLatest {
				if(ContextCompat.checkSelfPermission(
						this@MainActivity,
						Manifest.permission.POST_NOTIFICATIONS,
					) == PackageManager.PERMISSION_GRANTED)
				{
					if(description != it || it == DEFAULT_STRING_VALUE) //for optimization
					{
						Log.d("description in main", description)
						val newIntent = serviceIntent
						newIntent.putExtra(DESCRIPTION_NAMING, it)
						delay(400)
						Log.d("wtf in activity", bitmapString)
						newIntent.putExtra(PICTURE_BITMAP, bitmapString)
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
						{
							startForegroundService(newIntent)
							bindService(newIntent, connection, Context.BIND_AUTO_CREATE)
						}
						else
						{
							startService(newIntent)
							bindService(newIntent, connection, Context.BIND_AUTO_CREATE)
						}
						description = it
					}
				}
			}
		}
		val editorSharedPrefs = sharedPreferences.edit()
		editorSharedPrefs.putBoolean(JUST_CHANGED_THEME, false)
		editorSharedPrefs.putString(IS_BLACK_THEME, isDarkTheme(this).toString())
		editorSharedPrefs.apply()

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
			composable(BottomNavItem.Home.route) {
				PicturesScreen(
					navController = navController,
					viewModelPictures = picturesViewModel,
					postPressOnBackButton = { picturesViewModel.backNavButtonPress(true) },
					checkIfExists = { str -> picturesViewModel.checkOnErrorExists(str) },
					addError = { str -> picturesViewModel.addError(str) },
					getPics = { picturesViewModel.getPics() },
					postState = { str -> picturesViewModel.postState(str) },
					state = state.value,
					clearErrors = { picturesViewModel.clearErrors() },
					postPositiveState = { detailsViewModel.postPositiveVisabilityState() },
					postDefaultUrl = { detailsViewModel.postUrl(DEFAULT_STRING_VALUE, "") }
				)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(
					navController,
					themePick,
					postDefaultUrl = { detailsViewModel.postUrl(DEFAULT_STRING_VALUE, "") },
					changeFromSettings = { ctx -> settingsViewModel.changeFromSettings(ctx) },
					changeTheme = { ctx, int -> settingsViewModel.changeTheme(ctx, int) },
				)
			}
			composable(Screen.Details.route) {
				DetailsScreen(
					nc = navController,
					checkIfExists = { str -> picturesViewModel.checkOnErrorExists(str) },
					addError = { str -> picturesViewModel.addError(str) },
					state = barsState.value,
					removeSpecialError = { str -> picturesViewModel.removeSpecialError(str) },
					postDefaultUrl = { detailsViewModel.postUrl(DEFAULT_STRING_VALUE, "") },
					changeVisabilityState = { detailsViewModel.changeVisabilityState() },
					postUrl = { str, p -> detailsViewModel.postUrl(str, p) },
					postPositiveState = { detailsViewModel.postPositiveVisabilityState() },
					multiWindowState = multiWindowState.value
				)
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
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				{
					startForegroundService(serviceIntent)
					bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
				}
				else
				{
					startService(serviceIntent)
					bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
				}
			}
		}
	}

	private fun checkMultiWindowMode()
	{
		if(isInMultiWindowMode || isInPictureInPictureMode)
		{
			multiWindowState.value = MultiWindowState.MultiWindow
		}
		else
		{
			multiWindowState.value = MultiWindowState.NotMultiWindow
		}
	}

	override fun onRestart()
	{
		job.cancelChildren()
		Log.d("lifecycle", "onRestart()")
		super.onRestart()
	}

	override fun onResume()
	{
		if(ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS,
			) == PackageManager.PERMISSION_GRANTED)
		{
			val newIntent = serviceIntent
			newIntent.putExtra(DESCRIPTION_NAMING, description)
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				startForegroundService(newIntent)
				bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
				Log.d("description after pause", description)
			}
			else
			{
				startService(newIntent)
				bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
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
			mBound = false
		}
		countExitNavigation++
		isActive = false
		super.onPause()
	}

	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		val vis = getSharedPreferences(WE_WERE_HERE_BEFORE, MODE_PRIVATE)
		val editorVis = vis.edit()
		editorVis.putBoolean(WE_WERE_HERE_BEFORE, false)
		editorVis.apply()
		super.onDestroy()
	}

	private fun isDarkTheme(context: Context): Boolean
	{
		return context.resources.configuration.uiMode and
			Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
	}

	private fun setTheme()
	{
		settingsViewModel.changeTheme(this, themePick)
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
		const val PICTURE_BITMAP = "picture_bitmap"
		const val SHARED_PREFERENCE_GRIDPICS = "SHARED_PREFERENCE_GRIDPICS"
		const val DEFAULT_STRING_VALUE = "default"
		const val HTTP_ERROR = "HTTP error: 404"
	}
}
