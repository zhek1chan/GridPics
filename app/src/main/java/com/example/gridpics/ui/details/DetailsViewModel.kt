package com.example.gridpics.ui.details

import androidx.lifecycle.ViewModel
import com.example.gridpics.ui.state.BarsVisabilityState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DetailsViewModel: ViewModel()
{
	private val visabilityFlow = MutableStateFlow<BarsVisabilityState>(BarsVisabilityState.NotVisible)
	fun observeVisabilityFlow(): Flow<BarsVisabilityState> = visabilityFlow
	private val imageFlow = MutableStateFlow(mapOf<String, String>())
	fun observeUrlFlow() = imageFlow
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

	fun postNewPic(url: String, bitmapString: String)
	{
		imageFlow.value = imageFlow.value.plus(Pair(url, bitmapString))
	}

	fun postPositiveVisabilityState()
	{
		visabilityFlow.value = BarsVisabilityState.IsVisible
	}
}