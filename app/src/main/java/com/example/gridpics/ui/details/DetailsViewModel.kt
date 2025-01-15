package com.example.gridpics.ui.details

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.domain.interactor.ImagesInteractor
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
		MutableStateFlow<Pair<String?, Bitmap?>?>(null)
	val uiState =
		mutableStateOf(DetailsScreenUiState(isMultiWindowed = false, barsAreVisible = true, isSharedImage = false, picturesUrl = mutableListOf(), currentPicture = "", wasShared = false))
	private val job = Job()
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String?, bitmap: Bitmap?)
	{
		viewModelScope.launch {
			job.cancelChildren()
			imageFlow.emit(Pair(url, bitmap))
		}
	}

	fun postImageBitmap(url: String)
	{
		Log.d("Description posted", "desc was posted")
		viewModelScope.launch {
			Log.d("description job is active", "${job.isActive}")
			job.cancelChildren()
			imageFlow.emit(Pair("Картинка ещё грузится, пожалуйста подождите", null))
			val bitmap = interactor.getPictureBitmap(url, job)
			imageFlow.emit(Pair(url, bitmap))
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

	fun setWasShared(case: Boolean)
	{
		val state = uiState
		state.value = state.value.copy(wasShared = case)
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