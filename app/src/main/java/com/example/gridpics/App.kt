package com.example.gridpics

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.bitmapFactoryMaxParallelism
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.addLastModifiedToFileCacheKey
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.request.transitionFactory
import coil3.serviceLoaderEnabled
import coil3.size.Precision
import coil3.transition.Transition
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
				.fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(15))
				.interceptorCoroutineContext(Dispatchers.IO.limitedParallelism(5))
				.decoderCoroutineContext(Dispatchers.IO.limitedParallelism(15))
				.coroutineContext(Dispatchers.IO.limitedParallelism(5))
				.serviceLoaderEnabled(true)
				.addLastModifiedToFileCacheKey(false)
				.bitmapFactoryMaxParallelism(30)
				.precision(Precision.INEXACT)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.crossfade(false)
				.transitionFactory(Transition.Factory.NONE)
				.allowRgb565(true)
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

