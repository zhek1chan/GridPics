package com.example.gridpics.ui.pictures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	private val picturesUiState = MutableStateFlow(PicturesScreenUiState(PicturesState.NothingFound, false, ""))
	private val errorsList: MutableList<String> = mutableListOf()
	private val backNav = MutableStateFlow(false)
	private val currentImg = MutableStateFlow("")
	fun observePicturesFlow(): Flow<PicturesScreenUiState> = picturesUiState
	fun observeCurrentImg(): Flow<String> = currentImg
	fun observeBackNav(): Flow<Boolean> = backNav
	fun getPics()
	{
		viewModelScope.launch {
			interactor.getPics().collect { news ->
				when(news)
				{
					is Resource.Data -> picturesUiState.value = PicturesScreenUiState(PicturesState.SearchIsOk(news.value), picturesUiState.value.clearedCache, picturesUiState.value.picturesUrl)
					is Resource.ConnectionError -> picturesUiState.value = PicturesScreenUiState(PicturesState.ConnectionError, picturesUiState.value.clearedCache, picturesUiState.value.picturesUrl)
					is Resource.NotFound -> picturesUiState.value = PicturesScreenUiState(PicturesState.NothingFound, picturesUiState.value.clearedCache, picturesUiState.value.picturesUrl)
				}
			}
		}
	}

	fun postState(urls: String)
	{
		viewModelScope.launch {
			picturesUiState.emit(PicturesScreenUiState(PicturesState.Loaded(urls), picturesUiState.value.clearedCache, picturesUiState.value.picturesUrl))
		}
	}

	fun postSavedUrls(urls: String)
	{
		viewModelScope.launch {
			picturesUiState.emit(PicturesScreenUiState(picturesUiState.value.loadingState, picturesUiState.value.clearedCache, urls))
		}
	}

	fun postCacheWasCleared(cacheWasCleared: Boolean)
	{
		viewModelScope.launch {
			picturesUiState.emit(PicturesScreenUiState(picturesUiState.value.loadingState, cacheWasCleared, picturesUiState.value.picturesUrl))
		}
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

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}
}