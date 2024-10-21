package com.example.gridpics.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel: ViewModel()
{
	private val stateLiveData = MutableStateFlow(false)
	fun observeState(): Flow<Boolean> = stateLiveData
	fun changeState()
	{
		viewModelScope.launch(Dispatchers.IO) {
			stateLiveData.value = !stateLiveData.value
		}
	}
}