package com.example.gridpics.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel: ViewModel()
{
	private val visabilityFlow = MutableStateFlow(false)
	fun observeVisabilityFlow(): Flow<Boolean> = visabilityFlow
	private val urlFlow = MutableStateFlow("default")
	fun observeUrlFlow(): Flow<String> = urlFlow
	private val bitmapFlow = MutableStateFlow("default")
	fun observeBitmapFlow(): Flow<String> = bitmapFlow
	fun changeVisabilityState()
	{
		visabilityFlow.value = !visabilityFlow.value
	}

	private val stateMultiWindowFlow = MutableStateFlow(false)
	fun observeMultiWindowFlow(): Flow<Boolean> = stateMultiWindowFlow
	fun postState(b: Boolean)
	{
		stateMultiWindowFlow.value = b
	}

	fun postUrl(s: String, b: String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			urlFlow.emit(s)
			bitmapFlow.emit(b)
		}
	}

	fun postNegativeVisabilityState()
	{
		visabilityFlow.value = false
	}
}