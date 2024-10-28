package com.example.gridpics.ui.details

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DetailsViewModel: ViewModel()
{
	private val stateFlow = MutableStateFlow(false)
	fun observeFlow(): Flow<Boolean> = stateFlow
	fun changeState()
	{
		stateFlow.value = !stateFlow.value
	}
}