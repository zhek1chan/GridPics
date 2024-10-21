package com.example.gridpics

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.example.gridpics.data.network.getUnsafeOkHttpClient
import com.example.gridpics.di.dataModule
import com.example.gridpics.di.domainModule
import com.example.gridpics.di.viewModelModule
import com.example.gridpics.domain.interactor.SettingsInteractor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class App: Application(), KoinComponent, ImageLoaderFactory
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

	override fun newImageLoader(): ImageLoader
	{
		return ImageLoader(this).newBuilder()
			.memoryCachePolicy(CachePolicy.ENABLED)
			.memoryCache {
				MemoryCache.Builder(this)
					.maxSizePercent(0.1)
					.strongReferencesEnabled(true)
					.build()
			}
			.diskCachePolicy(CachePolicy.ENABLED)
			.diskCache {
				DiskCache.Builder()
					.maxSizePercent(0.03)
					.directory(cacheDir)
					.build()
			}
			.logger(DebugLogger())
			.build()
	}

	companion object
	{
		lateinit var instance: App
		lateinit var client: OkHttpClient
	}
}

