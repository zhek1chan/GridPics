package com.example.gridpics.ui.activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gridpics.R
import com.example.gridpics.ui.details.DetailsScreen
import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesScreen
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.settings.SettingsViewModel
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	private val settingsViewModel by viewModel<SettingsViewModel>()
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		super.onCreate(savedInstanceState)
		if(Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU)
		{
			if(
				ContextCompat.checkSelfPermission(
					this,
					Manifest.permission.POST_NOTIFICATIONS,
				) == PackageManager.PERMISSION_GRANTED
			)
			{
				createNotificationChannel()
				showNotification()
			}
			else
			{
				ActivityCompat.requestPermissions(
					this,
					arrayOf(Manifest.permission.POST_NOTIFICATIONS),
					100
				)
			}
		} else {
			createNotificationChannel()
			showNotification()
		}
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.light(getColor(R.color.white), getColor(R.color.black)),
			navigationBarStyle = SystemBarStyle.auto(getColor(R.color.white), getColor(R.color.black))
		)
		val changedTheme = getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE).getBoolean(THEME_SP_KEY, true)
		if(!changedTheme)
		{
			settingsViewModel.postValue(this, false)
		}
		else
		{
			settingsViewModel.postValue(this, true)
		}
		val controller = WindowCompat.getInsetsController(window, window.decorView)
		lifecycleScope.launch {
			detailsViewModel.observeState().collectLatest {
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

		setContent {
			ComposeTheme {
				val navController = rememberNavController()
				Scaffold(modifier = Modifier
					.fillMaxWidth(),
					bottomBar = { BottomNavigationBar(navController) },
					content = { padding ->
						Column(
							modifier = Modifier
								.padding(padding)
								.consumeWindowInsets(padding)
								.fillMaxSize()) {
							NavigationSetup(navController = navController)
						}
					}
				)
			}
		}
	}

	@Composable
	fun BottomNavigationBar(
		navController: NavController,
	)
	{
		val items = listOf(
			BottomNavItem.Home,
			BottomNavItem.Settings
		)
		val bottomBarState = remember { (mutableStateOf(true)) }
		val navBackStackEntry by navController.currentBackStackEntryAsState()
		when(navBackStackEntry?.destination?.route)
		{
			BottomNavItem.Home.route ->
			{
				// Show BottomBar and TopBar
				bottomBarState.value = true
			}
			BottomNavItem.Settings.route ->
			{
				// Show BottomBar and TopBar
				bottomBarState.value = true
			}
			Screen.Details.route ->
			{
				// Hide BottomBar and TopBar
				bottomBarState.value = false
			}
		}
		AnimatedVisibility(
			visible = bottomBarState.value, enter = slideInVertically(initialOffsetY = { it }), exit = ExitTransition.None
		) {
			NavigationBar(windowInsets = WindowInsets.navigationBars, containerColor = Color.Black) {
				val currentRoute = navBackStackEntry?.destination?.route
				items.forEach { item ->
					NavigationBarItem(
						colors = NavigationBarItemColors(Color.White, Color.White, Color.Gray, Color.White, Color.White, Color.White, Color.White),
						icon = {
							Icon(
								imageVector = item.icon,
								contentDescription = stringResource(id = item.titleResId)
							)
						},
						label = { Text(text = stringResource(id = item.titleResId)) },
						selected = currentRoute == item.route,
						onClick = {
							navController.navigate(item.route) {
								// Pop up to the start destination of the graph to
								// avoid building up a large stack of destinations
								// on the back stack as users select items
								navController.graph.startDestinationRoute?.let { route ->
									popUpTo(route) {
										saveState = true
									}
								}
								// Avoid multiple copies of the same destination when re-selecting the same item
								launchSingleTop = true
								// Restore state when re-selecting a previously selected item
								restoreState = true
							}
						}
					)
				}
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
				PicturesScreen(navController)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen(settingsViewModel)
			}
			composable(Screen.Details.route) {
				DetailsScreen(navController, detailsViewModel)
			}
		}
	}

	private fun createNotificationChannel()
	{
		val name = "My Notification Channel"
		val description = "Channel for my notification"
		// Создаем канал уведомлений (для Android O и выше)
		if(Build.VERSION.SDK_INT >= VERSION_CODES.O)
		{
			val importance = NotificationManager.IMPORTANCE_HIGH
			val channel = NotificationChannel(CHANNEL_ID, name, importance)
			channel.description = description
			val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
			notificationManager.createNotificationChannel(channel)
		}
	}

	private fun showNotification()
	{
		val intent = Intent(this, MainActivity::class.java)
		intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
		val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
		val builder = NotificationCompat.Builder(this, CHANNEL_ID)
			.setSmallIcon(R.mipmap.ic_launcher)
			.setColor(getColor(R.color.green))
			.setContentTitle("GridPics")
			.setContentText("Вы видите это уведомление, потому что приложение активно")
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setContentIntent(pendingIntent)
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(1, builder.build())
	}

	override fun onRestart()
	{
		showNotification()
		createNotificationChannel()
		super.onRestart()
	}

	override fun onStop()
	{
		this.lifecycleScope.launch {
			delay(2000)
			cancelAllNotifications()
		}
		super.onStop()
	}



	override fun onDestroy()
	{
		cancelAllNotifications()
		super.onDestroy()
	}

	private fun cancelAllNotifications()
	{
		val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.cancelAll()
		// Или, если вы хотите отменить только для конкретного канала:
		if(Build.VERSION.SDK_INT >= VERSION_CODES.O)
		{
			notificationManager.cancel(CHANNEL_ID, 0) // Отменяет все уведомления для указанного канала
		}
	}

	companion object
	{
		const val PICTURES = "PICTURES_SHARED_PREFS"
		const val PIC = "PIC"
		const val THEME_SP_KEY = "THEME_SHARED_PREFS"
		const val CHANNEL_ID = "GRID_PICS_CHANEL_ID"
	}
}
