package com.example.gridpics.ui.details

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class DetailsViewModel: ViewModel()
{
	private val imageFlow = MutableStateFlow(mapOf<String, String>())
	val uiStateFlow = mutableStateOf(DetailsScreenUiState(isMultiWindowed = false, barsAreVisible = false))
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String, bitmapString: String)
	{
		viewModelScope.launch {
			imageFlow.emit(mapOf(Pair(url, bitmapString)))
		}
	}

	fun changeVisabilityState()
	{
		viewModelScope.launch {
			if(!uiStateFlow.value.barsAreVisible)
			{
				uiStateFlow.value = uiStateFlow.value.copy(barsAreVisible = true)
			}
			else
			{
				uiStateFlow.value = uiStateFlow.value.copy(barsAreVisible = false)
			}
		}
	}

	fun changeMultiWindowState(isMultiWindowed: Boolean)
	{
		viewModelScope.launch {
			if(!isMultiWindowed)
			{
				uiStateFlow.value = uiStateFlow.value.copy(isMultiWindowed = true)
			}
			else
			{
				uiStateFlow.value = uiStateFlow.value.copy(isMultiWindowed = false)
			}
		}
	}

	fun postPositiveVisabilityState()
	{
		viewModelScope.launch {
			uiStateFlow.value = uiStateFlow.value.copy(barsAreVisible = true)
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