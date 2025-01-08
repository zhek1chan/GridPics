package com.example.gridpics.data.repository

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
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
import kotlinx.coroutines.withContext

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
					emit(Resource.Data(response.value))
					break
				}
				is Resource.NotFound ->
				{
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

	override suspend fun getPictureBitmap(url: String, context: Context, job: Job): Bitmap?
	{
		return withContext(Dispatchers.IO + job) {
			var bitmap: Bitmap? = null
			val imgRequest =
				ImageRequest.Builder(context)
					.data(url)
					.placeholder(R.drawable.loading)
					.error(R.drawable.error)
					.coroutineContext(coroutineContext + job)
					.allowHardware(false)
					.target {
						bitmap = it.toBitmap()
					}
					.diskCacheKey(url)
					.build()
			ImageLoader(context).newBuilder().build().enqueue(imgRequest)
			while(bitmap == null)
			{
				delay(200)
			}

			return@withContext bitmap
		}
	}
}