package com.example.gridpics.data.network

import android.R.attr.data
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.gridpics.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset


class RetrofitNetworkClient(
    private val context: Context,
    private val api: Api
) : NetworkClient {

    override suspend fun getPics(): Resource<String> {
        var news: Resource<String>
        if (!isConnected()) return Resource.ConnectionError(DEVICE_IS_OFFLINE)
        withContext(Dispatchers.IO) {
            news = try {
                api.getNews().byteStream().use {
                    val s = it.readBytes().toString(charset = Charset.defaultCharset())
                    Log.d("Retrofit data", "$s")
                    return@use Resource.Data(s)
                } ?: Resource.NotFound(NOT_FOUND)
            } catch (ex: IOException) {
                Log.e(REQUEST_ERROR, ex.toString())
                Resource.ConnectionError(REQUEST_ERROR)
            }
        }
        return news
    }


    private fun isConnected(): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI
                ) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> return true
            }
        }
        return false
    }

    companion object {
        private const val REQUEST_ERROR = "error_400"
        private const val NOT_FOUND = "error_404"
        private const val DEVICE_IS_OFFLINE = "you_are_offline"
    }

}