package com.example.gridpics.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailsViewModel: ViewModel()
{
	private val stateLiveData = MutableLiveData(false)
	fun observeState(): LiveData<Boolean> = stateLiveData
	fun changeState()
	{
		viewModelScope.launch(Dispatchers.IO) {
			if(stateLiveData.value == true)
			{
				stateLiveData.postValue(false)
			}
			else
			{
				stateLiveData.postValue(true)
			}
		}
	}
}