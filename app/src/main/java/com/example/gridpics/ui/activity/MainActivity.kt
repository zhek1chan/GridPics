package com.example.gridpics.ui.activity

import android.Manifest
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
import com.example.gridpics.ui.services.NotificationService
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	private val picturesViewModel by viewModel<PicturesViewModel>()
	private var picturesSharedPrefs: String? = null
	private var changedTheme: Boolean? = null
	private var serviceIntent = Intent()
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		Log.d("lifecycle", "onCreate()")
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		serviceIntent = Intent(this, NotificationService::class.java)

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			if(
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED
			)
			{
				startService(serviceIntent)
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
			startService(serviceIntent)
		}
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.black), getColor(R.color.white))
		)

		changedTheme = getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE).getBoolean(THEME_SP_KEY, true)
		if(!changedTheme!!)
		{
			settingsViewModel.postValue(this, false)
		}
		else
		{
			settingsViewModel.postValue(this, true)
		}
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		CoroutineScope(Dispatchers.Main).launch {
			detailsViewModel.observeFlow().collectLatest {
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
					this@MainActivity.finish()
				}
			}
		}

		picturesSharedPrefs = this.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, null)
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
		NavHost(navController, startDestination = BottomNavItem.Home.route, enterTransition = {
			EnterTransition.None
		},
			exitTransition = {
				ExitTransition.None
			}) {
			composable(BottomNavItem.Home.route) {
				PicturesScreen(navController, picturesViewModel)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(settingsViewModel, changedTheme!!, navController)
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
				startService(serviceIntent)
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

	override fun onStart()
	{
		Log.d("lifecycle", "onStart()")
		super.onStart()
		if(ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS,
			) == PackageManager.PERMISSION_GRANTED)
		{
			startService(serviceIntent)
		}
	}

	override fun onRestart()
	{
		super.onRestart()
		lifecycleScope.cancel()
		Log.d("lifecycle", "onRestart()")
	}

	override fun onStop()
	{
		Log.d("lifecycle", "onStop()")
		super.onStop()
		lifecycleScope.launch {
			delay(3000)
			stopService(serviceIntent)
		}
	}

	override fun onDestroy()
	{
		Log.d("lifecycle", "onDestroy()")
		super.onDestroy()
		val vis = getSharedPreferences(WE_WERE_HERE_BEFORE, MODE_PRIVATE)
		val editorVis = vis.edit()
		editorVis.putBoolean(WE_WERE_HERE_BEFORE, false)
		editorVis.apply()
		stopService(serviceIntent)
	}

	companion object
	{
		const val NOTIFICATION_ID = 1337
		const val CACHE = "CACHE"
		const val PICTURES = "PICTURES_SHARED_PREFS"
		const val PIC = "PIC"
		const val THEME_SP_KEY = "THEME_SHARED_PREFS"
		const val CHANNEL_ID = "GRID_PICS_CHANEL_ID"
		const val NULL_STRING = "NULL"
		const val TOP_BAR_VISABILITY = "TOP_BAR_VISABILITY"
		const val WE_WERE_HERE_BEFORE = "WE_WERE_HERE_BEFORE"
	}
}
