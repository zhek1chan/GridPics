package com.example.gridpics.ui.settings

import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SettingsViewModel: ViewModel()
{
	fun changeTheme(option: Int)
	{
		Log.d("theme option", "theme option: $option")
		when(option)
		{
			ThemePick.LIGHT_THEME.intValue ->
			{
				viewModelScope.launch {
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
				}
			}
			ThemePick.DARK_THEME.intValue ->
			{
				viewModelScope.launch {
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				}
			}
			ThemePick.FOLLOW_SYSTEM.intValue ->
			{
				viewModelScope.launch {
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
				}
			}
		}
	}
}