package com.example.gridpics.ui.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets.Type.statusBars
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
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
import com.example.gridpics.ui.themes.ComposeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

@Suppress("DEPRECATION")
class MainActivity: AppCompatActivity()
{
	private val detailsViewModel by viewModel<DetailsViewModel>()
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		WindowCompat.setDecorFitsSystemWindows(window, false)
		window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
		window.navigationBarColor = resources.getColor(R.color.black)
		window.statusBarColor = resources.getColor(R.color.grey_transparent)
		val sharedPref = getPreferences(Context.MODE_PRIVATE)
		val changedTheme =
			sharedPref.getString((THEME_SP_KEY), null)
		if(changedTheme == BLACK)
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
		}
		else if(changedTheme == WHITE)
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
		}
		detailsViewModel.observeState().observe(this) {
			if(it)
			{
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
				{
					window.insetsController?.hide(statusBars())
				}
				else
				{
					window.setFlags(
						FLAG_FULLSCREEN,
						FLAG_FULLSCREEN
					)
				}
				window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
				window.decorView.systemUiVisibility =
					View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
						View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			}
			else
			{
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
				{
					window.insetsController?.show(statusBars())
				}
				else
				{
					window.clearFlags(FLAG_FULLSCREEN)
				}
				window.decorView.systemUiVisibility =
					View.SYSTEM_UI_FLAG_VISIBLE or
						View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			}
		}

		setContent {
			ComposeTheme {
				val navController = rememberNavController()
				Scaffold(modifier = Modifier
					.fillMaxWidth()
					.width(40.dp),
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
			}, modifier = Modifier.fillMaxSize()) {
			composable(BottomNavItem.Home.route) {
				PicturesScreen(navController)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen()
			}
			composable(Screen.Details.route) {
				DetailsScreen(navController, detailsViewModel)
			}
		}
	}

	companion object
	{
		const val PICTURES = "PICTURES_SHARED_PREFS"
		const val WHITE = "white"
		const val BLACK = "black"
		const val PIC = "PIC"
		const val THEME_SP_KEY = "THEME_SHARED_PREFS"
	}
}
