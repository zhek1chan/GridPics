package com.example.gridpics

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.gridpics.di.dataModule
import com.example.gridpics.di.domainModule
import com.example.gridpics.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class App : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(
                domainModule,
                dataModule,
                viewModelModule
            )
        }
        instance = this
        /*val settingsInteractor = getKoin().get<SettingsInteractor>()
        switchTheme(settingsInteractor.isAppThemeDark())*/
    }

    private fun switchTheme(darkThemeIsEnabled: Boolean) {
        if (darkThemeIsEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    companion object {
        lateinit var instance: App
    }
}

