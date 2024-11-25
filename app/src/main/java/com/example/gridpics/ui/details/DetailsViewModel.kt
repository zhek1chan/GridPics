package com.example.gridpics.ui.details

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class DetailsViewModel: ViewModel()
{
	private val imageFlow = MutableStateFlow(mapOf<String, String>())
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String, bitmapString: String)
	{
		viewModelScope.launch {
			imageFlow.emit(mapOf(Pair(url, bitmapString)))
		}
	}

	fun convertPictureToString(bitmap: Bitmap): String
	{
		val baos = ByteArrayOutputStream()
		if(bitmap.byteCount > 1024 * 1024)
		{
			bitmap.compress(Bitmap.CompressFormat.JPEG, 3, baos)
		}
		else
		{
			bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
		}
		val b = baos.toByteArray()
		return Base64.encodeToString(b, Base64.DEFAULT)
	}
}