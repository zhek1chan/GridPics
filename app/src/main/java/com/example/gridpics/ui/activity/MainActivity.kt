package com.example.gridpics.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
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
import com.example.gridpics.ui.services.MainNotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

@OptIn(DelicateCoroutinesApi::class)
class MainActivity: AppCompatActivity()
{
	private var isActive: Boolean = false
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var picturesSharedPrefs: String? = null
	private var themePick: Int = 2
	private var serviceIntent = Intent()
	private var description = "default"
	private var job = jobForNotifications
	private var scope = GlobalScope
	private var bitmapString = ""
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		Log.d("lifecycle", "onCreate()")
		setTheme(R.style.Theme_GridPics)

		installSplashScreen()
		themePick = getSharedPreferences(THEME_SHARED_PREFERENCE, MODE_PRIVATE).getInt(THEME_SHARED_PREFERENCE, 2)
		val justChangedTheme = if(themePick == 2)
		{
			getSharedPreferences(JUST_CHANGED_THEME, MODE_PRIVATE).getBoolean(JUST_CHANGED_THEME, false)
		}
		else
		{
			isDarkTheme(this)
		}

		serviceIntent = Intent(this, MainNotificationService::class.java)
		serviceIntent.putExtra("description", description)
		val previousTheme = getSharedPreferences(IS_BLACK_THEME, MODE_PRIVATE).getString(IS_BLACK_THEME, isDarkTheme(this).toString())
		Log.d("theme", "just changed theme? $justChangedTheme")

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !justChangedTheme && (previousTheme == isDarkTheme(this).toString()))
		{
			if(
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED
			)
			{
				startForegroundService(serviceIntent)
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
			}
			else
			{
				startService(serviceIntent)
			}
		}

		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white))
		)
		//theme pick
		settingsViewModel.changeTheme(this, themePick)
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		CoroutineScope(Dispatchers.Main).launch {
			detailsViewModel.observeVisabilityFlow().collectLatest {
				if(it)
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

		CoroutineScope(Dispatchers.Main).launch {
			picturesViewModel.observeBackNav().collectLatest {
				if(it)
				{
					stopService(serviceIntent)
					this@MainActivity.finishAffinity()
				}
			}
		}

		picturesSharedPrefs = this.getSharedPreferences(SHARED_PREFS_PICTURES, MODE_PRIVATE).getString(SHARED_PREFS_PICTURES, null)
		CoroutineScope(Dispatchers.IO).launch {
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
					if(description != it || it == "default") //for optimization
					{
						Log.d("description in main", description)
						val newIntent = serviceIntent
						newIntent.putExtra("description", it)
						delay(350)
						Log.d("wtf in activity", bitmapString)
						newIntent.putExtra("picture_bitmap", bitmapString)
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
						{
							startForegroundService(newIntent)
						}
						else
						{
							startService(newIntent)
						}
						description = it
					}
				}
			}
		}
		val sharedPreferencesForDialog = this@MainActivity.getSharedPreferences(JUST_CHANGED_THEME, MODE_PRIVATE)
		val editorForDialog = sharedPreferencesForDialog.edit()
		editorForDialog.putBoolean(JUST_CHANGED_THEME, false)
		editorForDialog.apply()
		val sharedPreferencesForCheck = this.getSharedPreferences(IS_BLACK_THEME, MODE_PRIVATE)
		val editorForCheck = sharedPreferencesForCheck.edit()
		editorForCheck.putString(IS_BLACK_THEME, isDarkTheme(this).toString())
		editorForCheck.apply()

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
				PicturesScreen(navController, picturesViewModel, detailsViewModel)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(settingsViewModel, navController, detailsViewModel, themePick)
			}
			composable(Screen.Details.route) {
				DetailsScreen(navController, detailsViewModel, picturesViewModel)
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
				}
				else
				{
					startService(serviceIntent)
				}
			}
		}
	}

	private fun checkMultiWindowMode()
	{
		if(isInMultiWindowMode || isInPictureInPictureMode)
		{
			detailsViewModel.postState(true)
		}
		else
		{
			detailsViewModel.postState(false)
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
			newIntent.putExtra("description", description)
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			{
				startForegroundService(newIntent)
				Log.d("description after pause", description)
			}
			else
			{
				startService(newIntent)
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
		stopNotificationCoroutine()
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

	private fun stopNotificationCoroutine()
	{
		scope.launch(Dispatchers.IO + job) {
			Log.d("service", "stopNotificationCoroutine has been started")
			for(i in 0 .. 10)
			{
				delay(200)
				if(isActive)
				{
					cancel()
				}
				else if(i == 10)
				{
					stopService(serviceIntent)
					Log.d("service", "service was stopped")
				}
			}
		}
	}

	private fun isDarkTheme(context: Context): Boolean
	{
		return context.resources.configuration.uiMode and
			Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
	}

	companion object
	{
		val jobForNotifications = Job()
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
	}
}
