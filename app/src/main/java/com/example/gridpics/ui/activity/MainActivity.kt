package com.example.gridpics.ui.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets.Type.statusBars
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
		enableEdgeToEdge()
		val activityWindow = window
		activityWindow.navigationBarColor = getColor(R.color.black)
		activityWindow.statusBarColor = getColor(R.color.light_grey)
		val changedTheme = getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE).getBoolean(THEME_SP_KEY, true)
		if(!changedTheme)
		{
			settingsViewModel.postValue(this, false)
		}
		else
			settingsViewModel.postValue(this, true)


		val controller = window.insetsController
		this.lifecycleScope.launch {
			detailsViewModel.observeState().collectLatest {
				if(it)
				{
					controller?.hide(WindowInsetsCompat.Type.statusBars())
					controller?.hide(WindowInsetsCompat.Type.navigationBars())
				}
				else
				{
					controller?.show(WindowInsetsCompat.Type.statusBars())
					controller?.show(WindowInsetsCompat.Type.navigationBars())
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

	companion object
	{
		const val PICTURES = "PICTURES_SHARED_PREFS"
		const val PIC = "PIC"
		const val THEME_SP_KEY = "THEME_SHARED_PREFS"
	}
}
