package com.example.gridpics.ui.activity

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gridpics.R

sealed class BottomNavItem(
	val route: String,
	@StringRes val titleResId: Int,
	val icon: ImageVector,
)
{
	data object Home: BottomNavItem(
		route = Screen.Home.route,
		titleResId = R.string.main,
		icon = Icons.Default.Home
	)

	data object Settings: BottomNavItem(
		route = Screen.Settings.route,
		titleResId = R.string.settings,
		icon = Icons.Default.Settings
	)
}