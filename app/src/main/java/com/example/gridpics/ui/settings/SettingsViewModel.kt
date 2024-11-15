package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
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
	private val syncWithSys = MutableStateFlow(false)
	fun observeThemeSync(): Flow<Boolean> = syncWithSys
	private fun changeTheme(context: Context, changedByUser: Boolean)
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

	private fun pressOnSyncWithSys(context: Context)
	{
		saveState(context, darkIsActive = false, changedByUser = false)
		blackTheme.value = false
		if(syncWithSys.value)
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
		}
		else
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
		}
		syncWithSys.value = !syncWithSys.value
	}

	fun saveState(context: Context, darkIsActive: Boolean, changedByUser: Boolean)
	{
		if(changedByUser)
		{
			val sharedPreferences = context.getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE)
			val editor = sharedPreferences.edit()
			editor.putBoolean(THEME_SP_KEY, darkIsActive)
			editor.apply()
		}
		else
		{
			val sharedPreferences = context.getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE)
			val editor = sharedPreferences.edit()
			editor.putBoolean(THEME_SP_KEY, false)
			editor.apply()
		}
		val sP = context.getSharedPreferences(USER_CHANGED_THEME_BY_BUTTON, MODE_PRIVATE)
		val editorSecond = sP.edit()
		editorSecond.putBoolean(USER_CHANGED_THEME_BY_BUTTON, changedByUser)
		editorSecond.apply()
	}

	fun postValueSync(context: Context, b: Boolean, usePressFunc: Boolean)
	{
		syncWithSys.value = !b
		Log.d("theme", "posted follow sys colors: $b")
		if(usePressFunc)
		{
			pressOnSyncWithSys(context)
		}
	}

	fun postValue(context: Context, b: Boolean, changedByUser: Boolean, useChangeFunc: Boolean)
	{
		blackTheme.value = b
		Log.d("theme", "posted black theme: $b")
		if(useChangeFunc)
		{
			changeTheme(context, changedByUser)
		}
	}
}