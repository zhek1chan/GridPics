package com.example.gridpics.ui.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

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