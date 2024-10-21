package com.example.gridpics.ui.details

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DetailsViewModel: ViewModel()
{
	private val stateLiveData = MutableStateFlow(false)
	fun observeState(): Flow<Boolean> = stateLiveData
	fun changeState()
	{
		stateLiveData.value = !stateLiveData.value
	}
}