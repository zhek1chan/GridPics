package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import com.example.gridpics.ui.settings.ThemePick
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.SearchIsOk(mutableListOf()), mutableListOf(), 0, 0, true, ThemePick.FOLLOW_SYSTEM, emptyList()))
	private val errorsMap: MutableMap<String, String> = mutableMapOf()
	var mutableIsThemeBlackState = mutableStateOf(false)
	private var pictureSizeInDp = mutableIntStateOf(0)
	private var screenWidth = 0
	private var density = 0f
	private var maxVisibleLinesNum = 0

	init
	{
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data ->
					{
						val value = flow.value
						val savedUrls = value.picturesUrl
						val deletedUrls = value.deletedUrls
						val urlsFromNet = convertToListFromString(urls.value)
						val urlsToAdd = if(savedUrls.isNotEmpty())
						{
							compareAndCombineLists(savedUrls, deletedUrls)
						}
						else
						{
							compareAndCombineLists(urlsFromNet, deletedUrls)
						}
						flow.value = flow.value.copy(
							loadingState = PicturesState.SearchIsOk(urlsToAdd),
							picturesUrl = urlsToAdd
						)
					}
					is Resource.ConnectionError ->
						flow.value = flow.value.copy(loadingState = PicturesState.ConnectionError)
					is Resource.NotFound ->
						flow.value = flow.value.copy(loadingState = PicturesState.NothingFound)
				}
			}
		}
	}

	fun addPictureToUrls(pic: String)
	{
		viewModelScope.launch {
			val state = picturesUiState
			val sendList = mutableListOf<String>()
			val list = state.value.picturesUrl
			val size = list.size
			for(i in 0 ..< size)
			{
				if(list[i] != pic)
				{
					sendList.add(list[i])
				}
			}
			sendList.add(0, pic)
			Log.d("checkCheck", "list $list")
			state.value = state.value.copy(picturesUrl = sendList)
		}
	}

	fun postSavedUrls(urls: List<String>)
	{
		val state = picturesUiState
		state.value = state.value.copy(picturesUrl = urls)
	}

	fun addError(url: String, message: String)
	{
		val map = errorsMap
		val msg = if (url.startsWith("content://")) {
			"Нет доступа к просмотру изображения,\nпопробуйте удалить и заново добавить его"
		} else {
			message
		}
		if(!map.contains(url))
		{
			map[url] = msg
		}
	}

	fun checkOnErrorExists(url: String): String?
	{
		val list = errorsMap
		return if(list.contains(url))
		{
			list[url]
		}
		else
		{
			null
		}
	}

	fun removeSpecialError(url: String)
	{
		val map = errorsMap
		if(map.contains(url))
		{
			map.remove(url)
		}
	}

	fun clearErrors()
	{
		errorsMap.clear()
	}

	fun clickOnPicture(index: Int, offset: Int)
	{
		val state = picturesUiState
		val nIndex = if(index >= 0)
		{
			index
		}
		else
		{
			0
		}
		val nOffset = if(index >= 0)
		{
			offset
		}
		else
		{
			0
		}
		Log.d("checkIndex", "$nIndex")
		state.value = state.value.copy(index = nIndex, offset = nOffset)
	}

	fun changeOrientation(isPortrait: Boolean)
	{
		val state = picturesUiState
		state.value = state.value.copy(isPortraitOrientation = isPortrait)
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}

	fun postThemePick(option: ThemePick)
	{
		val state = picturesUiState
		state.value = state.value.copy(themeState = option)
	}

	fun convertToListFromString(string: String?): List<String>
	{
		return string?.split("\n")?.distinct() ?: listOf()
	}

	fun convertFromListToString(list: List<String>): String
	{
		var newString = ""
		val size = list.size
		for(i in 0 ..< size)
		{
			newString += if(i != size - 1)
			{
				list[i] + "\n"
			}
			else
			{
				list[i]
			}
		}
		return newString
	}

	fun returnStringOfList(): String
	{
		return convertFromListToString(picturesUiState.value.picturesUrl)
	}

	fun postDeletedUrls(urls: List<String>)
	{
		val state = picturesUiState
		state.value = state.value.copy(deletedUrls = urls)
	}

	private fun compareAndCombineLists(list1: List<String>, list2: List<String>): List<String>
	{
		return list1.filterNot { it in list2 } // Фильтруем первый список, оставляя только элементы, которых нет во втором
	}

	fun updatePictureSize(newSpan: Int)
	{
		Log.d("calculator1", "updated grid num")
		pictureSizeInDp.intValue = newSpan
	}

	fun getPictureSizeInDp(): MutableState<Int>
	{
		return pictureSizeInDp
	}

	fun postMaxVisibleLinesNum(num: Int)
	{
		maxVisibleLinesNum = num
	}

	fun postCurrentPicture(url: String)
	{
		val value = picturesUiState.value
		val urls = value.picturesUrl
		val index = value.index
		val indexOfCurrentPic = urls.indexOf(url)
		Log.d("proverka", "${(getGridNum())} $density")
		if(abs(indexOfCurrentPic - index) >= (maxVisibleLinesNum - getGridNum()) || (index > indexOfCurrentPic))
		{
			Log.d("check listScroll", "to 0 offset")
			clickOnPicture(indexOfCurrentPic, 0)
		}
	}

	fun swapPictures(fPic: String, sPic: String)
	{
		val state = picturesUiState
		val value = state.value
		val list = value.picturesUrl.toMutableList()
		val fIndex = list.indexOf(fPic)
		val sIndex = list.indexOf(sPic)
		val temp = list[fIndex]
		list[fIndex] = list[sIndex]
		list[sIndex] = temp
		state.value = value.copy(picturesUrl = list)
	}

	fun postWidth(width: Int)
	{
		screenWidth = width
	}

	fun postDensity(dens: Float)
	{
		density = dens
	}

	fun getGridNum(): Int
	{
		return ceil(screenWidth.toFloat() / density / pictureSizeInDp.intValue).toInt()+1
	}
}