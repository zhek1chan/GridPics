package com.example.gridpics.ui.pictures

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	private var isPaused = false
	private val stateLiveData = MutableLiveData<PictureState>()
	private val errorsList: MutableList<String> = mutableListOf()
	private val backNav = MutableStateFlow(false)
	private val currentImg = MutableStateFlow("")
	fun observeCurrentImg(): Flow<String> = currentImg
	fun observeBackNav(): Flow<Boolean> = backNav
	fun observeState(): LiveData<PictureState> = stateLiveData
	fun getPics()
	{
		viewModelScope.launch {
			interactor.getPics().collect { news ->
				when(news)
				{
					is Resource.Data -> stateLiveData.postValue(PictureState.SearchIsOk(news.value))
					is Resource.ConnectionError -> stateLiveData.postValue(PictureState.ConnectionError)
					is Resource.NotFound -> stateLiveData.postValue(PictureState.NothingFound)
				}
			}
		}
	}

	fun postState(urls: String)
	{
		stateLiveData.postValue(PictureState.Loaded(urls))
	}

	fun newState()
	{
		stateLiveData.postValue(PictureState.NothingFound)
	}

	fun resume()
	{
		isPaused = true
	}

	fun addError(url: String)
	{
		if(!errorsList.contains(url))
		{
			errorsList.add(url)
		}
	}

	fun checkOnErrorExists(url: String): Boolean
	{
		return errorsList.contains(url)
	}

	fun removeSpecialError(url: String)
	{
		if(errorsList.contains(url))
		{
			errorsList.remove(url)
		}
	}

	fun clearErrors()
	{
		errorsList.clear()
	}

	fun backNavButtonPress(pressed: Boolean)
	{
		backNav.value = pressed
	}

	fun clickOnPicture(url: String)
	{
		currentImg.value = url
	}
}