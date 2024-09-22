package com.example.gridpics.ui.pictures

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class PicturesViewModel(
    private val interactor: ImagesInteractor
) : ViewModel() {

    private val stateLiveData = MutableLiveData<PictureState>()
    fun observeState(): LiveData<PictureState> = stateLiveData
    fun getPics(context: Context) {
        viewModelScope.launch {
            interactor.getPics().collect { news ->
                when (news) {
                    is Resource.Data -> {
                        scopeSave(news.value, context)
                    }
                    is Resource.ConnectionError -> stateLiveData.postValue(PictureState.ConnectionError)
                    is Resource.NotFound -> stateLiveData.postValue(PictureState.NothingFound)
                }
            }
        }
    }

    fun readFiles(context: Context) {
        val numFileName = "num.txt"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString()
        val fileCheck = File(path, numFileName)
        Log.d("EXISTS?", "${fileCheck.exists()}")
        if (fileCheck.exists()) {
            Log.d("WTF", "FILE EXISTS")
            val file = File(path, numFileName)
            val num = BufferedReader(FileReader(file)).readLine().toInt()
            Log.d("EXISTS? NUM", "$num")
            val list = mutableListOf<File>()
            for (i in 1..<num+1) {
                Log.d("EXISTS? CYCLE", "$i.jpg")
                val fileName = "$i.jpg"
                val jpg = File(path, fileName)
                list.add(jpg)
            }
            stateLiveData.postValue(PictureState.SearchIsOk(list))
        } else {
            getPics(context)
        }
    }

    //для MVVM надо вынести в другой слой
    private fun getResponseCode(urlString: String): Int {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            return connection.responseCode
        } catch (e: MalformedURLException) {
            throw RuntimeException("Invalid URL: $urlString", e)
        } catch (e: IOException) {
            throw RuntimeException("Error connecting to URL: $urlString", e)
        }
    }

    private fun scopeSave(s: String, context: Context) {
        val list = s.split("\n")
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString(), "num.txt")
        file.createNewFile()
        file.writeText(list.size.toString())
        viewModelScope.launch(Dispatchers.IO) {
            for (i in list.indices) {
                if (list[i].endsWith("/404") || !(list[i].contains("https://")) || getResponseCode(
                        list[i]
                    ) == 404
                ) {
                    continue
                }
                saveImage(
                    Glide.with(context).addDefaultRequestListener(object : RequestListener<Any> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Any>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Any?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Any>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                    })
                        .asBitmap()

                        .load(list[i]) // sample image
                        .placeholder(android.R.drawable.progress_indeterminate_horizontal) // need placeholder to avoid issue like glide annotations
                        .error(android.R.drawable.stat_notify_error) // need error to avoid issue like glide annotations
                        .submit()
                        .get(), i, context
                )
            }
        }
    }

    private fun saveImage(image: Bitmap, num: Int, context: Context): String? {
        var savedImagePath: String? = null
        val imageFileName = "$num"
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString()
        )
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.getAbsolutePath()
            try {
                val fOut: OutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath, context)
            //Toast.makeText(this, "IMAGE SAVED", Toast.LENGTH_LONG).show() // to make this working, need to manage coroutine, as this execution is something off the main thread
        }
        return savedImagePath
    }

    private fun galleryAddPic(imagePath: String?, context: Context) {
        imagePath?.let { path ->
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val f = File(path)
            val contentUri: Uri = Uri.fromFile(f)
            mediaScanIntent.data = contentUri
            context.sendBroadcast(mediaScanIntent)
        }
    }
}