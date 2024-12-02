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
	private val imageFlow = MutableStateFlow<Pair<String, Bitmap?>>(Pair(DEFAULT_STRING_VALUE, null))
	val uiStateFlow = mutableStateOf(DetailsScreenUiState(isMultiWindowed = false, barsAreVisible = false))
	private val job = Job()
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String, bitmap: Bitmap?)
	{
		job.cancelChildren()
		viewModelScope.launch {
			imageFlow.emit(Pair(url, bitmap))
		}
	}

	fun postImageBitmap(url: String)
	{
		Log.d("Description posted", "desc was posted")
		val jobButNotSteveJobs = job
		jobButNotSteveJobs.cancelChildren()
		viewModelScope.launch {
			val bitmap = interactor.getPictureBitmap(url, jobButNotSteveJobs)
			imageFlow.emit(Pair(url, bitmap))
		}
	}

	fun changeVisabilityState()
	{
		var uiFlow = uiStateFlow.value
		viewModelScope.launch {
			uiFlow = if(!uiFlow.barsAreVisible)
			{
				uiFlow.copy(barsAreVisible = true)
			}
			else
			{
				uiFlow.copy(barsAreVisible = false)
			}
		}
	}

	fun changeMultiWindowState(isMultiWindowed: Boolean)
	{
		val uiState = uiStateFlow
		viewModelScope.launch {
			uiState.value = uiState.value.copy(isMultiWindowed = !isMultiWindowed)
		}
	}

	fun postPositiveVisabilityState()
	{
		val uiState = uiStateFlow
		viewModelScope.launch {
			uiState.value = uiState.value.copy(barsAreVisible = true)
		}
	}
}