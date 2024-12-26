package com.example.gridpics.ui.details

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
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
		MutableStateFlow<Pair<String, Bitmap?>>(Pair(DEFAULT_STRING_VALUE, null))
	val uiState =
		mutableStateOf(DetailsScreenUiState(isMultiWindowed = false, barsAreVisible = true, isSharedImage = false))
	private val job = Job()
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String, bitmap: Bitmap?)
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
			val jobButNotSteveJobs = job
			jobButNotSteveJobs.cancelChildren()
			val bitmap = interactor.getPictureBitmap(url, jobButNotSteveJobs)
			imageFlow.emit(Pair(url, bitmap))
		}
	}

	fun changeMultiWindowState(isMultiWindowed: Boolean)
	{
		val uiState = uiState
		viewModelScope.launch {
			uiState.value = uiState.value.copy(isMultiWindowed = isMultiWindowed)
		}
	}

	fun changeVisabilityState(visible: Boolean)
	{
		val state = uiState
		viewModelScope.launch {
			state.value = state.value.copy(barsAreVisible = visible)
			Log.d("barsaaa", "bars are visible? = $visible")
		}
	}

	fun isSharedImage(isShared: Boolean)
	{
		val state = uiState
		viewModelScope.launch {
			state.value = state.value.copy(isSharedImage = isShared)
			Log.d("case shared", "posted isShared state")
		}
	}
}