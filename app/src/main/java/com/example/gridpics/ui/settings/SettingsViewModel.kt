package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SP_KEY
import com.example.gridpics.ui.activity.MainActivity.Companion.USER_CHANGED_THEME_BY_BUTTON
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsViewModel: ViewModel()
{
	private val blackTheme = MutableStateFlow(false)
	fun observeTheme(): Flow<Boolean> = blackTheme
	fun changeTheme(context: Context, changedByUser: Boolean)
	{
		if(!blackTheme.value)
		{
			saveState(context, blackTheme.value, changedByUser)
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
		}
		else
		{
			saveState(context, blackTheme.value, changedByUser)
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
		}
		blackTheme.value = !blackTheme.value
	}

	private fun saveState(context: Context, whiteOrBlack: Boolean, changedByUser: Boolean)
	{
		val sharedPreferences = context.getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE)
		val editor = sharedPreferences.edit()
		editor.putBoolean(THEME_SP_KEY, whiteOrBlack)
		editor.apply()
		if(changedByUser)
		{
			val sP = context.getSharedPreferences(USER_CHANGED_THEME_BY_BUTTON, MODE_PRIVATE)
			val editorSecond = sP.edit()
			editorSecond.putBoolean(USER_CHANGED_THEME_BY_BUTTON, true)
			editorSecond.apply()
		}
	}

	fun postValue(context: Context, b: Boolean, changedByUser: Boolean)
	{
		blackTheme.value = b
		changeTheme(context, changedByUser)
	}
}