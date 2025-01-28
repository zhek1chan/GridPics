package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
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
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.SearchIsOk(mutableListOf()), mutableListOf(), 0, 0, true, ThemePick.FOLLOW_SYSTEM, emptyList()))
	private val errorsMap: MutableMap<String, String> = mutableMapOf()
	var orientationWasChanged = mutableStateOf(false)
	var mutableIsThemeBlackState = mutableStateOf(false)
	var cofConnectedWithOrientation = mutableFloatStateOf(0f)
	var cofConnectedWithOrientationForExit = mutableFloatStateOf(0f)
	var isSharedImage = mutableStateOf(false)
	var isImageToShareOrDelete = mutableStateOf(false)
	var pairOfPivotsXandY = mutableStateOf(Pair(0.1f, 0.1f))
	private var gridQuantity = 0
	private var screenWidth = 0
	private var screenHeight = 0
	private var densityOfScreen = 0f
	private var urlForCalculation = ""
	private var listOfPositions = mutableListOf<Pair<Float, Float>>()
	private var mapOfColumns = mutableMapOf<String, Int>()
	private var cutouts = Pair(0f, 0f)

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
		val nIndex = if(index >= 0)
		{
			index
		}
		else
		{
			0
		}
		Log.d("checkIndex", "$nIndex")
		state.value = state.value.copy(index = nIndex, offset = offset)
	}

	fun changeOrientation(isPortrait: Boolean)
	{
		val state = picturesUiState

		orientationWasChanged.value = true
		postPivotsXandY(Pair(12345f, 12345f))
		if(isPortrait)
		{
			val k = screenWidth
			screenWidth = screenHeight
			screenHeight = k
			cofConnectedWithOrientation.floatValue = 0.28f
			cofConnectedWithOrientationForExit.floatValue = 0.29f
		}
		else
		{
			val k = screenHeight
			screenHeight = screenWidth
			screenWidth = k
			cofConnectedWithOrientation.floatValue = 0.3f
			cofConnectedWithOrientationForExit.floatValue = 0.3f
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
		pairOfPivotsXandY.value = pairOfPivots
		Log.d("proverka", "Posted pivots")
	}

	fun getPivotsXandY(): Pair<Float, Float>
	{
		return pairOfPivotsXandY.value
	}

	private fun compareAndCombineLists(list1: List<String>, list2: List<String>): List<String>
	{
		return list1.filterNot { it in list2 } // Фильтруем первый список, оставляя только элементы, которых нет во втором
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

	fun calculatePosition(urlOfPic: String?)
	{
		var url = ""
		val list = picturesUiState.value.picturesUrl
		val urlForCalc = urlForCalculation
		if(urlOfPic != null)
		{
			urlForCalculation = urlOfPic
			url = urlOfPic
		}
		else if(urlForCalc.isNotEmpty())
		{
			//смена ориентации
			url = urlForCalc
			clickOnPicture(list.indexOf(url), 0)
		}
		val gridQuantity = gridQuantity
		//вычисляем позицию в формате таблицы
		if(mapOfColumns.isEmpty() || urlOfPic == null)
		{
			for(i in list.indices)
			{
				var column: Int
				column = (i + 1) % gridQuantity
				if(column == 0)
				{
					column = gridQuantity
				}
				Log.d("column", "$column")
				mapOfColumns[list[i]] = column
			}
		}
		Log.d("columns", "$mapOfColumns")
		if(url.isNotEmpty())
		{
			calculatePixelPosition(
				url = url,
				setYToDefault = false,
				needsRecalculation = urlOfPic == null
			)
		}
	}

	private fun calculatePixelPosition(
		url: String,
		setYToDefault: Boolean,
		needsRecalculation: Boolean,
	)
	{
		val listOfPositions = listOfPositions
		val screenWidth = screenWidth
		val screenHeight = screenHeight
		val mapOfColumns = mapOfColumns
		val urls = picturesUiState.value.picturesUrl
		val positionInPx = listOfPositions[urls.indexOf(url)]
		var x = positionInPx.first / screenWidth.toFloat()
		var y = positionInPx.second / screenHeight.toFloat()
		val cutouts = cutouts
		val gridQuantity = gridQuantity
		val densityOfScreen = densityOfScreen
		Log.d("cutouts", "${cutouts.first}")
		val column = mapOfColumns[url]!!
		if(!needsRecalculation)
		{
			if(screenWidth < screenHeight)
			{
				postPivotsXandY(Pair(x * 1.4f, y * 1.37f))
				Log.d("proverka x, y", "$x, $y, portrait")
			}
			else
			{
				if(cutouts.first != 0f)
				{
					val cutsToPivots = cutouts.first * densityOfScreen / screenWidth.toFloat()
					x = ((column - 2) / gridQuantity) + (1.3f + (column - 1) / gridQuantity) * x - 0.046f * (1f - (column) / gridQuantity)

					Log.d("proverka cutsToPivots", "$cutsToPivots")
					Log.d("proverka", "$x, cutout sleva")
				}
				else if(cutouts.second != 0f)
				{
					val cutsToPivots = cutouts.second * densityOfScreen / screenWidth.toFloat()
					x = ((column - 2) / gridQuantity) + (1.3f + (column - 1) / gridQuantity) * x - 0.046f * (1f - (column) / gridQuantity) - 0.08f

					Log.d("proverka", "$x, cutout sprava")
					Log.d("proverka cutsToPivots", "$cutsToPivots")
				}
				else
				{
					x = if(column == gridQuantity)
					{
						-0.13f
					}
					else
					{
						0.8f * x + (column) * 0.1f
					}
				}
				if(setYToDefault)
				{
					y = listOfPositions[0].second / screenHeight + 0.215f
				}
				else if(positionInPx.second <= 26)
				{
					y += 0.215f
				}
				else
				{
					y *= 2.05f
				}
				Log.d("proverka", "Pair $x , $y")
				postPivotsXandY(Pair(x, y))
			}
		}
		else
		{
			Log.d("proverka", " cutouts = $cutouts")
			if(screenWidth < screenHeight)
			{
				val xPortrait = 1f / gridQuantity * column - 0.15f * (gridQuantity - column)
				postPivotsXandY(Pair(xPortrait, 0f))
				Log.d("proverka density", "$densityOfScreen")
				Log.d("proverka x, y", "$xPortrait, 0, portrait, column = $column")
			}
			else
			{
				if(cutouts.first != 0f)
				{
					x = if(column == 1)
					{
						-0.05f
					}
					else
					{
						0.15f * (column - 1) + column * 0.01f
					}
					Log.d("proverka", "$x, cutout sleva")
					Log.d("proverka column", "$column")
				}
				else if(cutouts.second != 0f)
				{
					x = if(column == gridQuantity)
					{
						-0.13f
					}
					else
					{
						0.15f * (column - 1) + column * 0.01f - 0.08f
					}
					Log.d("proverka", "$x, cutout sprava")
					Log.d("proverka column", "$column")
				}
				else
				{
					x = if(column == gridQuantity)
					{
						-0.13f
					}
					else
					{
						((110 * (column + 2))) * densityOfScreen / screenWidth.toFloat() - 0.08f
					}
				}
				Log.d("proverka", "Pair $x , 2.09f")
				postPivotsXandY(Pair(x, 0.24056539f))
			}
		}
	}

	fun calculateListPosition(url: String)
	{
		val list = picturesUiState.value.picturesUrl
		val column = mapOfColumns[url]
		val listOfPositions = listOfPositions
		val rightY = listOfPositions[0]
		val positionInPx = listOfPositions[column!! - 1]
		if(url != urlForCalculation)
		{
			clickOnPicture(list.indexOf(url), 0)
			val x = positionInPx.first / screenWidth.toFloat()
			val y = rightY.second / screenHeight.toFloat()
			if(screenWidth < screenHeight)
			{
				postPivotsXandY(Pair(x * 1.4f, y * 1.37f))
			}
			else
			{
				calculatePixelPosition(
					url = url,
					setYToDefault = true,
					needsRecalculation = false
				)
				clickOnPicture(list.indexOf(url), 0)
			}
		}
	}

	fun postPosition(url: String, position: Pair<Float, Float>)
	{
		val urls = picturesUiState.value.picturesUrl
		val listOfPositions = listOfPositions
		val index = urls.indexOf(url)
		if(position != Pair(0f, 0f))
		{
			listOfPositions.add(index, position)
		}
	}

	fun postCutouts(left: Float, right: Float)
	{
		cutouts = Pair(left, right)
	}
}