package com.example.gridpics.ui.details

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel: ViewModel()
{
	private val imageFlow = MutableStateFlow<Pair<String, Bitmap?>>(Pair(DEFAULT_STRING_VALUE, null))
	val uiStateFlow = mutableStateOf(DetailsScreenUiState(isMultiWindowed = false, barsAreVisible = false))
	fun observeUrlFlow() = imageFlow
	fun postNewPic(url: String, bitmap: Bitmap?)
	{
		viewModelScope.launch {
			val pair = Pair(url, bitmap)
			if(imageFlow.value != pair)
			{
				imageFlow.emit(pair)
			}
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