package com.example.gridpics.domain.interactor

import com.example.gridpics.domain.ThemeSettings

class SettingsInteractorImpl(private var themeSettings: ThemeSettings): SettingsInteractor
{
	var isDarkTheme = true
	override fun isAppThemeDark(): Boolean
	{
		isDarkTheme = themeSettings.lookAtTheme()
		return isDarkTheme
	}

	override fun changeThemeSettings(): Boolean
	{
		isDarkTheme = themeSettings.appThemeSwitch()
		return isDarkTheme
	}
}