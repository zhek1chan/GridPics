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
import kotlin.math.ceil

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.SearchIsOk(mutableListOf()), mutableListOf(), 0, 0, true, ThemePick.FOLLOW_SYSTEM, emptyList()))
	private val errorsMap: MutableMap<String, String> = mutableMapOf()
	private var pairOfPivotsXandY = Pair(0.1f, 0.1f)
	private var gridQuantity = 0
	private var screenWidth = 0
	private var screenHeight = 0
	private var densityOfScreen = 0f
	private var topBarPadding = 0
	private var leftInsets = 0
	private var picHeight = 0
	private var picWidth = 0

	init
	{
		Log.d("lifecycle", "vm is recreated")
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data ->
					{
						val savedUrls = flow.value.picturesUrl
						val deletedUrls = flow.value.deletedUrls
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
		if(!map.contains(url))
		{
			map[url] = message
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
		state.value = state.value.copy(index = index, offset = offset)
	}

	fun changeOrientation(isPortrait: Boolean)
	{
		val state = picturesUiState
		if(isPortrait)
		{
			val k = screenWidth
			screenWidth = screenHeight
			screenHeight = k
		}
		else
		{
			val k = screenHeight
			screenHeight = screenWidth
			screenWidth = k
		}
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

	private fun postPivotsXandY(pairOfPivots: Pair<Float, Float>)
	{
		pairOfPivotsXandY = pairOfPivots
	}

	fun getPivotsXandY(): Pair<Float, Float>
	{
		return pairOfPivotsXandY
	}

	private fun compareAndCombineLists(list1: List<String>, list2: List<String>): List<String>
	{
		return list1.filterNot { it in list2 } // Фильтруем первый список, оставляя только элементы, которых нет во втором
	}

	fun postGridCountAndParamsOfScreen(cutoutInsetsOfScreen: Int, topBarSize: Int)
	{
		leftInsets = cutoutInsetsOfScreen + 26
		Log.d("calculator1", "left insets $leftInsets")
		topBarPadding = topBarSize + 60
		Log.d("calculator1", "status bar insets $topBarPadding")
	}

	fun postPicParams(height: Int, width: Int){
		picWidth = width
		picHeight = height
		Log.d("AHAHA","AHAHA")
	}

	fun postParamsOfScreen(gridNum: Int, width: Int, height: Int, density: Float)
	{
		gridQuantity = gridNum
		screenHeight = height
		screenWidth = width
		densityOfScreen = density
	}

	fun updateGridSpan(newSpan: Int)
	{
		Log.d("calculator1", "updated grid num")
		gridQuantity = newSpan
	}

	fun calculatePosition(url: String)
	{
		val list = picturesUiState.value.picturesUrl
		val gridQuantity = gridQuantity
		val index = list.indexOf(url) + 1
		//вычисляем позицию в формате таблицы
		var column: Int
		val line: Float = ceil(index.toFloat() / gridQuantity.toFloat())
		column = index % gridQuantity
		if(column == 0)
		{
			column = gridQuantity
		}
		Log.d("calculator0", "line = $line, column = $column")
		calculatePixelPosition(line.toInt(), column, leftInsets, topBarPadding)
	}

	private fun calculatePixelPosition(
		line: Int,
		column: Int,
		leftPadding: Int,
		topPadding: Int,
	)
	{
		// Преобразуем dp в пиксели
		// Вычисляем координаты левого верхнего угла ячейки в пикселях
		Log.d("AHAHA", "${((picWidth + 20) * column)/screenWidth.toFloat()}")
		val x = ((picWidth + 10) * column)/screenWidth.toFloat()
		val y = ((-topPadding + line.toFloat() * 110) * densityOfScreen) / screenHeight.toFloat()
		//по идее нужно разделить значения на размеры экрана
		Log.d("AHAHA 222", "x = $x, y = $y")
		postPivotsXandY(Pair(x, y))
	}
}