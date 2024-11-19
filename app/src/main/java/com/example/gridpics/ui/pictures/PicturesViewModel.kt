package com.example.gridpics.ui.pictures

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	private var isPaused = false
	private val stateLiveData = MutableLiveData<PictureState>()
	private val errorsList: MutableList<String> = mutableListOf()
	private val backNav = MutableStateFlow(false)
	fun observeBackNav(): Flow<Boolean> = backNav
	fun observeState(): LiveData<PictureState> = stateLiveData
	fun getPics()
	{
		viewModelScope.launch {
			interactor.getPics().collect { news ->
				when(news)
				{
					is Resource.Data -> stateLiveData.postValue(PictureState.SearchIsOk(news.value))
					is Resource.ConnectionError ->
					{
						stateLiveData.postValue(PictureState.ConnectionError)
						withContext(Dispatchers.IO) {
						}
					}
					is Resource.NotFound -> stateLiveData.postValue(PictureState.NothingFound)
				}
			}
		}
	}

	fun postState(s: String)
	{
		stateLiveData.postValue(PictureState.Loaded(s))
	}

	fun newState()
	{
		stateLiveData.postValue(PictureState.NothingFound)
	}

	fun resume()
	{
		isPaused = true
	}

	fun addError(s: String)
	{
		if (!errorsList.contains(s))
		{
			errorsList.add(s)
		}
	}

	fun checkOnErrorExists(s: String): Boolean
	{
		return errorsList.contains(s)
	}

	fun removeSpecialError(s: String)
	{
		if(errorsList.contains(s))
		{
			errorsList.remove(s)
		}
	}

	fun clearErrors()
	{
		errorsList.clear()
	}

	fun backNavButtonPress(b: Boolean)
	{
		backNav.value = b
	}
}