package com.example.gridpics.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import com.example.gridpics.App
import com.example.gridpics.domain.ThemeSettings

class ThemeSettingsImpl(
    private val application: Application,
    private var themeSharedPrefs: SharedPreferences,
): ThemeSettings
{
	private var appTheme: Boolean = false
	override fun lookAtTheme(): Boolean
	{
		themeSharedPrefs = application.getSharedPreferences(THEME_KEY, Context.MODE_PRIVATE)
		appTheme = themeSharedPrefs.getBoolean(THEME_KEY, !isDarkThemeEnabled())
		return appTheme
	}

	private fun isDarkThemeEnabled(): Boolean
	{
		val applicationContext = App.instance.applicationContext
		val currentMode =
			applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
		return currentMode == Configuration.UI_MODE_NIGHT_YES
	}

	override fun appThemeSwitch(): Boolean
	{
		appTheme = !appTheme
		val editor = themeSharedPrefs.edit()
		editor.putBoolean(THEME_KEY, appTheme)
		editor.apply()
		return appTheme
	}

	companion object
	{
		const val THEME_KEY = "theme"
	}
}