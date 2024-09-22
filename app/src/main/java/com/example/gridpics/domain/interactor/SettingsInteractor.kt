package com.example.gridpics.domain.interactor

interface SettingsInteractor {
    fun isAppThemeDark(): Boolean
    fun changeThemeSettings(): Boolean
}