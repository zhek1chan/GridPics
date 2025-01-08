package com.example.gridpics.ui.details

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	private val imageFlow =
		MutableStateFlow<Pair<String, Bitmap?>?>(null)
	val uiState =
		mutableStateOf(DetailsScreenUiState(isMultiWindowed = false, barsAreVisible = true, isSharedImage = false, picturesUrl = mutableListOf(), currentPicture = ""))
	private var job = Job()
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String, bitmap: Bitmap?)
	{
		viewModelScope.launch {
			val jobButNotSteveJobs = job
			jobButNotSteveJobs.cancelChildren()
			job = this as CompletableJob
			imageFlow.emit(Pair(url, bitmap))
		}
	}

	fun postImageBitmap(url: String)
	{
		Log.d("Description posted", "desc was posted")
		job.cancelChildren()
		job = viewModelScope.launch {
			Log.d("description job is active", "${job.isActive}")
			val bitmap = interactor.getPictureBitmap(url, job)
			imageFlow.emit(Pair(url, bitmap))
		} as CompletableJob
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
		var sendList = mutableListOf<String>()
		val size = list.size
		if(url != null)
		{
			for(i in 0 ..< size)
			{
				if(list[i] != url)
				{
					sendList.add(list[i])
				}
			}
			sendList.add(0, url)
		}
		else
		{
			sendList = list.toMutableList()
		}
		state.value = state.value.copy(picturesUrl = sendList)
		Log.d("index list", "create list for screen was called")
	}
}