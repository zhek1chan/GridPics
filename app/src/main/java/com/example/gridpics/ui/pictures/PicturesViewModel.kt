package com.example.gridpics.ui.pictures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.state.UiStateDataClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	private val uiStateFlow = MutableStateFlow(UiStateDataClass(isMultiWindowed = false, barsAreVisible = false, pictureScreenState = PicturesState.ConnectionError))
	private val errorsList: MutableList<String> = mutableListOf()
	private val backNav = MutableStateFlow(false)
	var picturesUrls: String = ""
	private val currentImg = MutableStateFlow("")
	fun observeCurrentImg(): Flow<String> = currentImg
	fun observeBackNav(): Flow<Boolean> = backNav
	fun observeUiState(): Flow<UiStateDataClass> = uiStateFlow
	fun getPics()
	{
		viewModelScope.launch {
			interactor.getPics().collect { news ->
				when(news)
				{
					is Resource.Data -> uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, uiStateFlow.value.barsAreVisible, PicturesState.SearchIsOk(news.value)))
					is Resource.ConnectionError -> uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, uiStateFlow.value.barsAreVisible, PicturesState.ConnectionError))
					is Resource.NotFound -> uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, uiStateFlow.value.barsAreVisible, PicturesState.NothingFound))
				}
			}
		}
	}

	fun changeVisabilityState()
	{
		viewModelScope.launch {
			if(!uiStateFlow.value.barsAreVisible)
			{
				uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, true, uiStateFlow.value.pictureScreenState))
			}
			else
			{
				uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, false, uiStateFlow.value.pictureScreenState))
			}
		}
	}

	fun changeMultiWindowState(isMultiWindowed: Boolean)
	{
		viewModelScope.launch {
			if(!isMultiWindowed)
			{
				uiStateFlow.emit(UiStateDataClass(true, uiStateFlow.value.barsAreVisible, uiStateFlow.value.pictureScreenState))
			}
			else
			{
				uiStateFlow.emit(UiStateDataClass(false, uiStateFlow.value.barsAreVisible, uiStateFlow.value.pictureScreenState))
			}
		}
	}

	fun postPositiveVisabilityState()
	{
		viewModelScope.launch {
			uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, true, uiStateFlow.value.pictureScreenState))
		}
	}

	fun postState(urls: String)
	{
		viewModelScope.launch {
			uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, uiStateFlow.value.barsAreVisible, PicturesState.Loaded(urls)))
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

	fun postPictures(urls: String) {
		picturesUrls = urls
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