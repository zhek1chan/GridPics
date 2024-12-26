package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import com.example.gridpics.ui.settings.ThemePick
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.SearchIsOk(""), "", 0, 0, "", true))
	var usedValueFromIntent = ""
	private val errorsList: MutableList<String> = mutableListOf()
	private var index: Int? = null
	private var onPauseWasCalled = false
	private var rememberSharedPictureOnFirstStart = ""
	private var isFirstImage = false
	val themeState = mutableStateOf(ThemePick.FOLLOW_SYSTEM)

	init
	{
		Log.d("lifecycle", "vm is recreated")
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data ->
						flow.value = flow.value.copy(
							loadingState = PicturesState.SearchIsOk(urls.value)
						)
					is Resource.ConnectionError ->
						flow.value = flow.value.copy(loadingState = PicturesState.ConnectionError)
					is Resource.NotFound ->
						flow.value = flow.value.copy(loadingState = PicturesState.NothingFound)
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

	fun addPictureToState()
	{
		val rememberSharedPictureOnFirstStart = rememberSharedPictureOnFirstStart
		val state = picturesUiState
		if(rememberSharedPictureOnFirstStart.isNotEmpty() && state.value.loadingState is PicturesState.SearchIsOk)
		{
			if(state.value.loadingState is PicturesState.SearchIsOk)
				viewModelScope.launch {
					state.value = state.value.copy(picturesUrl = rememberSharedPictureOnFirstStart + (state.value.loadingState as PicturesState.SearchIsOk).data)
				}
		}
	}

	fun addPictureToUrls(pic: String)
	{
		val state = picturesUiState
		viewModelScope.launch {
			Log.d("nka", "${pic.contains("\n")}")
			val sendUrl = if(pic.contains("\n"))
			{
				pic
			}
			else
			{
				pic + "\n"
			}
			state.value = state.value.copy(picturesUrl = sendUrl + state.value.picturesUrl)
		}
	}

	fun postSavedUrls(urls: String?)
	{
		viewModelScope.launch {
			val flow = picturesUiState
			val notNullUrls = urls ?: ""
			/*if(caseEmptySharedPreferenceOnFirstLaunch)
			{
				rememberSharedPictureOnFirstStart = notNullUrls
			}
			else
			{
				rememberSharedPictureOnFirstStart = ""*/
			flow.value = flow.value.copy(picturesUrl = notNullUrls)
			//}
		}
	}

	fun removeUrlFromSavedUrls(url: String)
	{
		Log.d("remove", "removing url $url")
		viewModelScope.launch {
			val urls = picturesUiState.value.picturesUrl
			removeUrlAndPostNewString(urls, url)
			rememberSharedPictureOnFirstStart = ""
		}
	}

	private fun removePrefix(str: String, prefix: String): String =
		if(str.startsWith(prefix)) str.substring(prefix.length) else str

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

	fun changeOrientation(isPortrait: Boolean)
	{
		val state = picturesUiState
		state.value = state.value.copy(isPortraitOrientation = isPortrait)
	}

	fun saveCurrentPictureUrl(url: String)
	{
		viewModelScope.launch {
			val state = picturesUiState
			state.value = state.value.copy(currentPicture = url + "\n")
		}
	}

	/*fun restoreDeletedUrl()
	{
		val state = picturesUiState
		val url = state.value.currentPicture
		if(url.isNotEmpty())
		{
			val list = state.value.picturesUrl.split("\n").toMutableList()
			val index = index
			if(index != null && index < list.size)
			{
				list.add(index, url)
			}

			viewModelScope.launch {
				val newString = createNewString(list)
				state.value = state.value.copy(picturesUrl = newString)
				Log.d("remove2", "added string ${newString.split("\n")[0]}")
			}
		}
	}*/
	fun postUsedIntent(url: String)
	{
		usedValueFromIntent = url
	}

	fun getUsedIntentValue(): String
	{
		return usedValueFromIntent
	}

	fun urlWasAlreadyInSP(url: String, urlsFromSP: String)
	{
		val list = urlsFromSP.split("\n")
		index = list.indexOf(url)
	}

	fun clearIndex()
	{
		index = null
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}

	/*fun putPreviousPictureCorrectly(oldPicture: String)
	{
		val index = index
		if(index != null)
		{
			Log.d("remove", "we have put correctly")
			val state = picturesUiState
			val value = state.value
			val list = value.picturesUrl.split("\n").toSet().toMutableList()
			viewModelScope.launch {
				if(list.contains(oldPicture))
				{
					list.remove(oldPicture)
				}
				list.add(index, oldPicture)
				if(list.contains("\n"))
				{
					list.remove("\n")
				}
				val newString = createNewString(list)
				state.value = state.value.copy(picturesUrl = newString)
			}
		}
	}*/
	fun clearUsedIntentValue()
	{
		usedValueFromIntent = ""
	}

	private fun removeUrlAndPostNewString(urls: String, url: String)
	{
		val state = picturesUiState
		val newString = if(urls.startsWith("$url\n$url\n") && !isFirstImage && index != null)
		{
			Log.d("worked", "worked")
			removePrefix(urls, "$url\n$url\n")
		}
		else if(urls.startsWith("$url\n"))
		{
			removePrefix(urls, "$url\n")
		}
		else
		{
			removePrefix(urls, url)
		}

		state.value = state.value.copy(picturesUrl = newString)
	}

	fun postIsFirstPage(firstPage: Boolean)
	{
		isFirstImage = firstPage
	}

	fun postOnPauseWasCalled(wasCalled: Boolean)
	{
		onPauseWasCalled = wasCalled
	}

	fun getOnPauseWasCalled(): Boolean
	{
		return onPauseWasCalled
	}

	fun postThemePick(option: ThemePick)
	{
		themeState.value = option
	}
}