package com.example.gridpics.ui.details

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import kotlinx.coroutines.Dispatchers
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
		viewModelScope.launch {
			job.cancelChildren()
			imageFlow.emit(Pair(url, bitmap))
		}
	}

	fun postImageBitmap(url: String)
	{
		viewModelScope.launch(Dispatchers.IO + job) {
			val bitmap = interactor.getPictureBitmap(url, job)
			imageFlow.emit(Pair(url, bitmap))
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
}