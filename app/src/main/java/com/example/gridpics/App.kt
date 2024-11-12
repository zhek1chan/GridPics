package com.example.gridpics

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import com.example.gridpics.ui.activity.MainActivity.Companion.NOTIFICATION_ID
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

		registerActivityLifecycleCallbacks(object: ActivityLifecycleCallbacks
		{
			override fun onActivityCreated(p0: Activity, p1: Bundle?)
			{
			}

			override fun onActivityStarted(p0: Activity)
			{
			}

			override fun onActivityResumed(p0: Activity)
			{
			}

			override fun onActivityPaused(p0: Activity)
			{
			}

			override fun onActivityStopped(p0: Activity)
			{
			}

			override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle)
			{
			}

			override fun onActivityDestroyed(p0: Activity)
			{
				val manager: NotificationManager = getSystemService(NotificationManager::class.java)
				manager.cancel(NOTIFICATION_ID)
				Toast.makeText(baseContext, "pidoras ti", Toast.LENGTH_LONG).show()
			}
		})

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

