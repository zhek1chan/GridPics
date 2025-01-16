package com.example.gridpics.ui.activity

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun BottomNavigationBar(
	navController: NavController,
)
{
	val items = remember {
		listOf(
			BottomNavItem.Home,
			BottomNavItem.Settings
		)
	}
	val bottomBarState = remember { (mutableStateOf(true)) }
	val navBackStackEntry by navController.currentBackStackEntryAsState()
	val scope = rememberCoroutineScope()
	val route = navBackStackEntry?.destination?.route
	when(route)
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
			bottomBarState.value = true
			scope.launch {
				delay(400)
				bottomBarState.value = false
			}
		}
	}
	AnimatedVisibility(
		visible = bottomBarState.value, enter = slideInVertically(initialOffsetY = { it }), exit = ExitTransition.None
	) {
		NavigationBar(windowInsets = WindowInsets.navigationBars, containerColor = MaterialTheme.colorScheme.background) {
			items.forEach { item ->
				NavigationBarItem(
					colors = NavigationBarItemColors(MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.onPrimary, Color.Gray, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.onPrimary,
						MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.onPrimary),
					icon = {
						Icon(
							imageVector = item.icon,
							contentDescription = stringResource(id = item.titleResId)
						)
					},
					label = { Text(text = stringResource(id = item.titleResId)) },
					selected = route == item.route,
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

							anim {
								enter = 0
								exit = 0
								popEnter = 0
								popExit = 0
							}
						}
					}
				)
			}
		}
	}
}