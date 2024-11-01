package com.example.gridpics

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import com.example.gridpics.data.network.getUnsafeOkHttpClient
import com.example.gridpics.di.dataModule
import com.example.gridpics.di.domainModule
import com.example.gridpics.di.viewModelModule
import com.example.gridpics.domain.interactor.SettingsInteractor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class App: Application(), KoinComponent
{
	override fun onCreate()
	{
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
		SingletonImageLoader.setSafe {
			ImageLoader.Builder(this)
				.allowHardware(true)
				.networkCachePolicy(CachePolicy.ENABLED)
				.memoryCachePolicy(CachePolicy.ENABLED)
				.diskCachePolicy(CachePolicy.ENABLED)
				.diskCache {
					DiskCache.Builder()
						.directory(this.cacheDir.resolve("image_cache"))
						.build()
				}
				.build()
		}
		//Check android vers
		client = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
		{
			getUnsafeOkHttpClient()
		}
		else
		{
			OkHttpClient()
		}
	}

	private fun switchTheme(darkThemeIsEnabled: Boolean)
	{
		if(darkThemeIsEnabled)
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
		}
		else
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
		}
	}

	companion object
	{
		lateinit var instance: App
		lateinit var client: OkHttpClient
	}
}

