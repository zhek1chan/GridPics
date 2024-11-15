package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SP_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsViewModel: ViewModel()
{
	private val stateFlow = MutableStateFlow(2)
	fun observeFlow(): Flow<Int> = stateFlow
	fun changeTheme(context: Context, option: Int)
	{
		when(option)
		{
			0 ->
			{
				stateFlow.value = 0
				saveThemeState(context, 0)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			}
			1 ->
			{
				stateFlow.value = 1
				saveThemeState(context, 1)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
			}
			2 ->
			{
				stateFlow.value = 2
				saveThemeState(context, 2)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
			}
		}
	}

	private fun saveThemeState(context: Context, i: Int)
	{
		val sharedPreferences = context.getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE)
		val editor = sharedPreferences.edit()
		editor.putInt(THEME_SP_KEY, i)
		editor.apply()
	}
}