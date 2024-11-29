package com.example.gridpics.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.error
import coil3.request.placeholder
import coil3.toBitmap
import com.example.gridpics.R
import com.example.gridpics.data.network.NetworkClient
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.repository.ImagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ImagesRepositoryImpl(
	private val networkClient: NetworkClient,
): ImagesRepository
{
	override suspend fun getPics(): Flow<Resource<String>> = flow {
		while(true)
		{
			when(val response = networkClient.getPics())
			{
				is Resource.Data ->
				{
					with(response) {
						val data = value
						emit(Resource.Data(data))
					}
					break
				}
				is Resource.NotFound -> {
					emit(Resource.NotFound(response.message))
					break
				}
				is Resource.ConnectionError ->
				{
					emit(Resource.ConnectionError(response.message))
					delay(1000)
				}
			}
		}
	}.flowOn(Dispatchers.IO)

	override suspend fun getPictureBitmap(url: String, context: Context, job: Job): Bitmap? {
		var bitmap: Bitmap? = null
		val imgRequest =
			ImageRequest.Builder(context)
				.data(url)
				.placeholder(R.drawable.loading)
				.error(R.drawable.error)
				.coroutineContext(Dispatchers.IO + job)
				.allowHardware(false)
				.target {
					Log.d("checkMa", "gruzim pic")
					bitmap = it.toBitmap()
				}
				.diskCachePolicy(CachePolicy.ENABLED)
				.diskCacheKey(url)
				.memoryCachePolicy(CachePolicy.ENABLED)
				.build()
		ImageLoader(context).newBuilder().build().enqueue(imgRequest)
		while(bitmap == null){
			delay(300)
		}
		return bitmap
	}
}