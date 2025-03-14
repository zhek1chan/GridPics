package com.example.gridpics.ui.details

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.domain.model.PicturesDataForNotification
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	private val imageFlow =
		MutableStateFlow(PicturesDataForNotification(null, null, false))
	val uiState =
		mutableStateOf(DetailsScreenUiState(isMultiWindowed = false, barsAreVisible = true, isSharedImage = false, picturesUrl = mutableListOf(), currentPicture = "", wasSharedFromNotification = false, wasDeletedFromNotification = false))
	private val job = Job()
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String?, bitmap: Bitmap?)
	{
		viewModelScope.launch {
			job.cancelChildren()
			val showButtons = url != null
			imageFlow.emit(PicturesDataForNotification(url, bitmap, showButtons))
		}
	}

	fun postImageBitmap(url: String, stringFromRes: String)
	{
		Log.d("Description posted", "desc was posted")
		viewModelScope.launch {
			Log.d("description job is active", "${job.isActive}")
			job.cancelChildren()
			imageFlow.emit(PicturesDataForNotification(stringFromRes, null, false))
			val bitmap = interactor.getPictureBitmap(url, job)
			if(uiState.value.isSharedImage)
			{
				imageFlow.emit(PicturesDataForNotification(url, bitmap, false))
			}
			else
			{
				imageFlow.emit(PicturesDataForNotification(url, bitmap, true))
			}
		}
	}

	fun changeMultiWindowState(isMultiWindowed: Boolean)
	{
		val uiState = uiState
		uiState.value = uiState.value.copy(isMultiWindowed = isMultiWindowed)
	}

	fun changeVisabilityState(visible: Boolean)
	{
		val state = uiState
		state.value = state.value.copy(barsAreVisible = visible)
		Log.d("barsaaa", "bars are visible? = $visible")
	}

	fun isSharedImage(isShared: Boolean)
	{
		val state = uiState
		state.value = state.value.copy(isSharedImage = isShared)
		Log.d("case shared", "posted isShared state $isShared")
	}

	fun firstSetOfListState(list: List<String>)
	{
		val state = uiState
		state.value = state.value.copy(picturesUrl = list)
	}

	fun postCurrentPicture(url: String)
	{
		val state = uiState
		state.value = state.value.copy(currentPicture = url)
	}

	fun postCorrectList()
	{
		Log.d("index list", "correct list was posted")
		val value = uiState.value
		val pictures = value.picturesUrl
		if(value.isSharedImage)
		{
			createListForScreen(pictures, value.currentPicture)
		}
		else
		{
			createListForScreen(pictures, null)
		}
	}

	private fun createListForScreen(list: List<String>, url: String?)
	{
		val state = uiState
		val size = list.size
		val listForState = if(url != null)
		{
			val sendList = mutableListOf<String>()
			for(i in 0 ..< size)
			{
				if(list[i] != url)
				{
					sendList.add(list[i])
				}
			}
			sendList.add(0, url)
			sendList
		}
		else
		{
			list
		}
		state.value = state.value.copy(picturesUrl = listForState)
		Log.d("index list", "create list for screen was called")
	}

	fun setWasSharedFromNotification(case: Boolean)
	{
		val state = uiState
		state.value = state.value.copy(wasSharedFromNotification = case)
	}

	fun setWasDeletedFromNotification(case: Boolean)
	{
		val state = uiState
		state.value = state.value.copy(wasDeletedFromNotification = case)
	}

	fun deleteCurrentPicture(url: String): List<String>
	{
		val state = uiState
		val list = state.value.picturesUrl as MutableList
		list.remove(url)
		Log.d("test111", "$list")
		state.value = state.value.copy(picturesUrl = list)
		return list
	}
}