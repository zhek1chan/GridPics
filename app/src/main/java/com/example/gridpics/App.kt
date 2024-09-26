package com.example.gridpics

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.gridpics.di.dataModule
import com.example.gridpics.di.domainModule
import com.example.gridpics.di.viewModelModule
import com.example.gridpics.domain.interactor.SettingsInteractor
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
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
        val settingsInteractor = getKoin().get<SettingsInteractor>()
        switchTheme(settingsInteractor.isAppThemeDark())

        val builder = Picasso.Builder(this)
        builder.downloader(OkHttp3Downloader(this, Long.MAX_VALUE))
        val built = builder.build()
        built.setIndicatorsEnabled(true)
        built.isLoggingEnabled = true
        Picasso.setSingletonInstance(built)
        Picasso.get().setIndicatorsEnabled(false)
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

