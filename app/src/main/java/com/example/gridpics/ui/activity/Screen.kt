package com.example.gridpics.ui.activity

sealed class Screen(val route: String)
{
	data object Home: Screen("home_screen")
	data object Settings: Screen("settings_screen")
	data object Details: Screen("details_screen")
}