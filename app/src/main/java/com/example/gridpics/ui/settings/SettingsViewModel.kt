package com.example.gridpics.ui.settings

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel

class SettingsViewModel: ViewModel()
{
	var justChangedTheme = false
	fun changeTheme(option: Int)
	{
		Log.d("theme option", "theme option: $option")
		when(option)
		{
			ThemePick.LIGHT_THEME.intValue ->
			{
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			}
			ThemePick.DARK_THEME.intValue ->
			{
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
			}
			ThemePick.FOLLOW_SYSTEM.intValue ->
			{
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
			}
		}
	}
}