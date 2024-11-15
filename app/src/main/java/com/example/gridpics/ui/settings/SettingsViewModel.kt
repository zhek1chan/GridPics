package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SP_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel: ViewModel()
{
	private val stateFlow = MutableStateFlow(2)
	fun observeFlow(): Flow<Int> = stateFlow
	fun changeTheme(context: Context, option: Int)
	{
		Log.d("theme option", "theme option: $option")
		when(option)
		{
			0 ->
			{
				viewModelScope.launch(Dispatchers.Main) {
					stateFlow.value = 0
					saveThemeState(context, 0)
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
				}
			}
			1 ->
			{
				viewModelScope.launch(Dispatchers.Main) {
					stateFlow.value = 1
					saveThemeState(context, 1)
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				}
			}
			2 ->
			{
				viewModelScope.launch(Dispatchers.Main) {
					stateFlow.value = 2
					saveThemeState(context, 2)
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
				}
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