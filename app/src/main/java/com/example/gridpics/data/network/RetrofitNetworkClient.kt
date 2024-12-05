package com.example.gridpics.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset

class RetrofitNetworkClient(
	private val context: Context,
	private val api: Api,
): NetworkClient
{
	override suspend fun getPics(): Resource<String>
	{
		var imagesUrls: Resource<String>
		if(!isConnected()) return Resource.ConnectionError(DEVICE_IS_OFFLINE)
		withContext(Dispatchers.IO) {
			imagesUrls = try
			{
				api.getNews().byteStream().use {
					val s = it.readBytes().toString(charset = Charset.defaultCharset())
					Log.d("Retrofit data", s)
					return@use Resource.Data(s)
				}
			}
			catch(ex: IOException)
			{
				Log.e(REQUEST_ERROR, ex.toString())
				Resource.ConnectionError(REQUEST_ERROR)
			}
		}
		return imagesUrls
	}

	private fun isConnected(): Boolean
	{
		val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
		return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
	}

	companion object
	{
		private const val REQUEST_ERROR = "error_400"
		private const val DEVICE_IS_OFFLINE = "you_are_offline"
	}
}