package com.example.gridpics.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
import com.example.gridpics.ui.state.BarsVisabilityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel: ViewModel()
{
	private val visabilityFlow = MutableStateFlow<BarsVisabilityState>(BarsVisabilityState.IsVisible)
	fun observeVisabilityFlow(): Flow<BarsVisabilityState> = visabilityFlow
	private val urlFlow = MutableStateFlow(DEFAULT_STRING_VALUE)
	fun observeUrlFlow(): Flow<String> = urlFlow
	private val bitmapFlow = MutableStateFlow(DEFAULT_STRING_VALUE)
	fun observeBitmapFlow(): Flow<String> = bitmapFlow
	fun changeVisabilityState()
	{
		if(visabilityFlow.value == BarsVisabilityState.NotVisible)
		{
			visabilityFlow.value = BarsVisabilityState.IsVisible
		}
		else
		{
			visabilityFlow.value = BarsVisabilityState.NotVisible
		}
	}

	fun postUrl(s: String, b: String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			urlFlow.value = s
			bitmapFlow.value = b
		}
	}

	fun postPositiveVisabilityState()
	{
		visabilityFlow.value = BarsVisabilityState.IsVisible
	}
}