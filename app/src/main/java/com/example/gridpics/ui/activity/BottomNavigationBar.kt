package com.example.gridpics.ui.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

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
	val bottomBarState = remember(Unit) { mutableStateOf(true) }
	val navBackStackEntry by navController.currentBackStackEntryAsState()
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
			bottomBarState.value = false
		}
	}
	AnimatedVisibility(
		visible = bottomBarState.value, enter = EnterTransition.None, exit = ExitTransition.None
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
							popUpTo(navController.graph.findStartDestination().id) {
								inclusive = false
								saveState = true
							}
							// Avoid multiple copies of the same destination when
							// reselecting the same item
							restoreState = true
							launchSingleTop = true
							// Restore state when reselecting a previously selected item
						}
					}
				)
			}
		}
	}
}