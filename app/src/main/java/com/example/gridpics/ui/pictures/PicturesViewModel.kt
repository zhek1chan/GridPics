package com.example.gridpics.ui.pictures

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.NothingFound, "", 0, 0, ""))
	private val errorsList: MutableList<String> = mutableListOf()

	init
	{
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data -> flow.value = flow.value.copy(loadingState = PicturesState.SearchIsOk(urls.value))
					is Resource.ConnectionError -> flow.value = flow.value.copy(loadingState = PicturesState.ConnectionError)
					is Resource.NotFound -> flow.value = flow.value.copy(loadingState = PicturesState.NothingFound)
				}
			}
		}
	}

	fun postState(useLoadedState: Boolean, urls: String)
	{
		val flow = picturesUiState
		viewModelScope.launch {
			flow.value = if(useLoadedState)
			{
				flow.value.copy(loadingState = PicturesState.Loaded(urls))
			}
			else
			{
				flow.value.copy(loadingState = PicturesState.SearchIsOk(urls))
			}
		}
	}

	fun postSavedUrls(urls: String?)
	{
		val flow = picturesUiState
		viewModelScope.launch {
			flow.value = flow.value.copy(picturesUrl = urls)
		}
	}

	fun addError(url: String)
	{
		val list = errorsList
		if(!list.contains(url))
		{
			list.add(url)
		}
	}

	fun checkOnErrorExists(url: String): Boolean
	{
		val list = errorsList
		return (list.isNotEmpty() && list.contains(url))
	}

	fun removeSpecialError(url: String)
	{
		val list = errorsList
		if(list.contains(url))
		{
			list.remove(url)
		}
	}

	fun clearErrors()
	{
		errorsList.clear()
	}

	fun clickOnPicture(url: String, index: Int, offset: Int)
	{
		val state = picturesUiState
		state.value = state.value.copy(index = index, offset = offset, currentPicture = url)
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}
}