package com.example.gridpics

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.allowHardware
import com.example.gridpics.di.dataModule
import com.example.gridpics.di.domainModule
import com.example.gridpics.di.viewModelModule
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class App: Application(), KoinComponent, SingletonImageLoader.Factory
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
		SingletonImageLoader.setSafe {
			ImageLoader.Builder(this)
				.allowHardware(false)
				.networkCachePolicy(CachePolicy.ENABLED)
				.memoryCachePolicy(CachePolicy.ENABLED)
				.diskCachePolicy(CachePolicy.ENABLED)
				.fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(15))
				.interceptorCoroutineContext(Dispatchers.IO.limitedParallelism(4))
				.coroutineContext(Dispatchers.IO.limitedParallelism(4))
				.diskCache {
					DiskCache.Builder()
						.directory(this.cacheDir.resolve("image_cache"))
						.build()
				}
				.build()
		}
	}

	override fun newImageLoader(context: Context): ImageLoader
	{
		return ImageLoader.Builder(context)
			.diskCache {
				DiskCache.Builder()
					.directory(context.cacheDir.resolve("image_cache"))
					.build()
			}
			.build()
	}
}

