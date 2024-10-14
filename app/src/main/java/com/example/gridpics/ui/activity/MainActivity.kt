package com.example.gridpics.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gridpics.R
import com.example.gridpics.ui.details.DetailsScreen
import com.example.gridpics.ui.pictures.PicturesScreen
import com.example.gridpics.ui.settings.SettingsScreen
import com.example.gridpics.ui.themes.ComposeTheme

class MainActivity: AppCompatActivity()
{
	@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(R.style.Theme_GridPics)
		installSplashScreen()
		super.onCreate(savedInstanceState)
		val sharedPref = getPreferences(Context.MODE_PRIVATE)
		val changedTheme =
			sharedPref.getString(getString(R.string.changed_theme), null).toString()
		val uiMode = resources.configuration.uiMode
		val nightMask = Configuration.UI_MODE_NIGHT_MASK
		if((Configuration.UI_MODE_NIGHT_NO == uiMode and nightMask) && (changedTheme == BLACK))
		{
			changeTheme()
		}
		else if((Configuration.UI_MODE_NIGHT_YES == uiMode and nightMask) && (changedTheme == WHITE))
		{
			changeTheme()
		}

		setContent {
			ComposeTheme {
				val navController = rememberNavController()

				Scaffold(
					bottomBar = { BottomNavigationBar(navController) }
				) {
					NavigationSetup(navController = navController)
				}
			}
		}
	}

	private fun changeTheme()
	{
		val darkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
		val isDarkModeOn = darkMode == Configuration.UI_MODE_NIGHT_YES
		if(isDarkModeOn)
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
		}
		else
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
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
		AnimatedVisibility(visible = bottomBarState.value) {
			ComposeTheme {
				BottomNavigation(backgroundColor = MaterialTheme.colorScheme.onSecondary) {
					val navBackStackEntry by navController.currentBackStackEntryAsState()
					val currentRoute = navBackStackEntry?.destination?.route
					items.forEach { item ->
						BottomNavigationItem(
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
	}

	@Composable
	fun NavigationSetup(navController: NavHostController)
	{
		NavHost(navController, startDestination = BottomNavItem.Home.route) {
			composable(BottomNavItem.Home.route) {
				PicturesScreen(navController)
			}
			composable(BottomNavItem.Settings.route) {
				SettingsScreen()
			}
			composable(Screen.Details.route) {
				DetailsScreen(navController)
			}
		}
	}

	companion object
	{
		const val PICTURES = "PICTURES_SHARED_PREFS"
		const val WHITE = "white"
		const val BLACK = "black"
		const val PIC = "PIC"
	}
}
