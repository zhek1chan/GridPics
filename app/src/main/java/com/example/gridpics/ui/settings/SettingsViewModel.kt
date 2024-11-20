package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.ui.activity.MainActivity.Companion.JUST_CHANGED_THEME
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFERENCE_GRIDPICS
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SHARED_PREFERENCE
import kotlinx.coroutines.launch

class SettingsViewModel: ViewModel()
{
	fun changeTheme(context: Context, option: Int)
	{
		Log.d("theme option", "theme option: $option")
		when(option)
		{
			0 ->
			{
				viewModelScope.launch {
					saveThemeState(context, 0)
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
				}
			}
			1 ->
			{
				viewModelScope.launch {
					saveThemeState(context, 1)
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				}
			}
			2 ->
			{
				viewModelScope.launch {
					saveThemeState(context, 2)
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
				}
			}
		}
	}

	fun changeFromSettings(context: Context)
	{
		val sharedPreferencesForDialog = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		val editorForDialog = sharedPreferencesForDialog.edit()
		editorForDialog.putBoolean(JUST_CHANGED_THEME, true)
		editorForDialog.apply()
	}

	private fun saveThemeState(context: Context, i: Int)
	{
		val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		val editor = sharedPreferences.edit()
		editor.putInt(THEME_SHARED_PREFERENCE, i)
		editor.apply()
	}
}