package com.example.gridpics.ui.pictures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.state.UiStateDataClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	private val uiStateFlow = MutableStateFlow(UiStateDataClass(isMultiWindowed = false, barsAreVisible = false))
	private val errorsList: MutableList<String> = mutableListOf()
	private val backNav = MutableStateFlow(false)
	private val picturesFlow = MutableStateFlow <PicturesState?>(null)
	private val currentImg = MutableStateFlow("")
	fun observePicturesFlow(): Flow<PicturesState?> = picturesFlow
	fun observeCurrentImg(): Flow<String> = currentImg
	fun observeBackNav(): Flow<Boolean> = backNav
	fun observeUiState(): Flow<UiStateDataClass> = uiStateFlow
	fun getPics()
	{
		viewModelScope.launch {
			picturesFlow.drop(1)
			interactor.getPics().collect { news ->
				when(news)
				{
					is Resource.Data -> picturesFlow.emit(PicturesState.SearchIsOk(news.value))
					is Resource.ConnectionError -> picturesFlow.emit(PicturesState.ConnectionError)
					is Resource.NotFound -> picturesFlow.emit(PicturesState.NothingFound)
				}
			}
		}
	}

	fun changeVisabilityState()
	{
		viewModelScope.launch {
			if(!uiStateFlow.value.barsAreVisible)
			{
				uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, true))
			}
			else
			{
				uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, false))
			}
		}
	}

	fun changeMultiWindowState(isMultiWindowed: Boolean)
	{
		viewModelScope.launch {
			if(!isMultiWindowed)
			{
				uiStateFlow.emit(UiStateDataClass(true, uiStateFlow.value.barsAreVisible))
			}
			else
			{
				uiStateFlow.emit(UiStateDataClass(false, uiStateFlow.value.barsAreVisible))
			}
		}
	}

	fun postPositiveVisabilityState()
	{
		viewModelScope.launch {
			uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, true))
		}
	}

	fun postState()
	{
		viewModelScope.launch {
			uiStateFlow.emit(UiStateDataClass(uiStateFlow.value.isMultiWindowed, uiStateFlow.value.barsAreVisible))
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