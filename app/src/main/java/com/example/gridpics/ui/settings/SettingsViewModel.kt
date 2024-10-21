package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
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
	private val blackTheme = MutableStateFlow(false)
	fun observeTheme(): Flow<Boolean> = blackTheme
	fun changeTheme(context: Context)
	{
		viewModelScope.launch(Dispatchers.Main) {
			if(!blackTheme.value)
			{
				saveState(context, blackTheme.value)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
			}
			else
			{
				saveState(context, blackTheme.value)
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			}
			blackTheme.value = !blackTheme.value
		}
	}

	private fun saveState(context: Context, whiteOrBlack: Boolean) {
		val sharedPreferences = context.getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE)
		val editor = sharedPreferences.edit()
		editor.putBoolean(THEME_SP_KEY, whiteOrBlack)
		editor.apply()
	}

	fun postValue(context: Context, b: Boolean)
	{
		blackTheme.value = b
		changeTheme(context)
	}
}