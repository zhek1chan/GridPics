package com.example.gridpics

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.bitmapFactoryMaxParallelism
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.addLastModifiedToFileCacheKey
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.serviceLoaderEnabled
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
				.bitmapConfig(Bitmap.Config.RGB_565)
				.crossfade(false)
				.components {
					if (SDK_INT >= 28) {
						add(AnimatedImageDecoder.Factory())
					} else {
						add(GifDecoder.Factory())
					}
				}
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

