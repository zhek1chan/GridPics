package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.IntSize
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
	var orientationWasChanged = mutableStateOf(false)
	var mutableIsThemeBlackState = mutableStateOf(false)
	var cofConnectedWithOrientation = mutableFloatStateOf(0f)
	var cofConnectedWithOrientationForExit = mutableFloatStateOf(0f)
	var isSharedImage = mutableStateOf(false)
	var isImageToShareOrDelete = mutableStateOf(false)
	var pairOfPivotsXandY = mutableStateOf(Pair(0.1f, 0.1f))
	private var gridQuantity = mutableIntStateOf(0)
	private var gridInPortraitQ = 3
	private var screenWidth = 0
	private var screenHeight = 0
	private var densityOfScreen = 0f
	private var barsSize = Pair(0f, 0f)
	private var urlForCalculation = ""
	private var maxVisibleElements = 18
	private var listOfPositions = mutableListOf<Pair<Float, Float>>()
	private var mapOfColumns = mutableMapOf<String, Int>()
	private var cutouts = Pair(0f, 0f)
	private var sizeOfPic = IntSize.Zero
	private var isOrientationPortrait = false

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
		isOrientationPortrait = isPortrait
		val state = picturesUiState
		orientationWasChanged.value = true
		val densityOfScreen = densityOfScreen
		val barsSize = barsSize
		val top = barsSize.first
		val bottom = barsSize.second
		val cofConnectedWithOrientation = cofConnectedWithOrientation
		val cofConnectedWithOrientationForExit = cofConnectedWithOrientationForExit
		postPivotsXandY(Pair(12345f, 12345f))
		if(isPortrait)
		{
			val sizeOfPicLocal = sizeOfPic.width
			Log.d("size check", "${sizeOfPicLocal / screenWidth.toFloat()}")
			val k = screenWidth
			screenWidth = screenHeight
			screenHeight = k
			if(sizeOfPicLocal != 0)
			{
				cofConnectedWithOrientation.floatValue = sizeOfPicLocal.toFloat() / screenWidth.toFloat() + 0.03f
				cofConnectedWithOrientationForExit.floatValue = sizeOfPicLocal.toFloat() / screenWidth.toFloat() + 0.03f
			}
		}
		else
		{
			val sizeOfPicLocal = sizeOfPic.height
			val k = screenHeight
			screenHeight = screenWidth
			screenWidth = k
			if(sizeOfPicLocal != 0)
			{
				val screenHeight = screenHeight
				cofConnectedWithOrientation.floatValue = sizeOfPicLocal / (screenHeight + (-60 - top - bottom) * densityOfScreen) - 0.057f
				cofConnectedWithOrientationForExit.floatValue = sizeOfPicLocal / (screenHeight + (-60 - top - bottom) * densityOfScreen) - 0.057f
			}
		}
		Log.d("pupu", "${cofConnectedWithOrientation.floatValue}   ${cofConnectedWithOrientationForExit.floatValue}")
		calculatePosition(null)
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
		Log.d("proverka", "Posted pivots $pairOfPivots")
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
		gridQuantity.intValue = gridNum
		screenHeight = height
		screenWidth = width
		densityOfScreen = density
	}

	fun updateGridSpan(newSpan: Int)
	{
		Log.d("calculator1", "updated grid num")
		gridQuantity.intValue = newSpan
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
			Log.d("I was called", "I was called")
			if(list.size - list.indexOf(url) < 18)
			{
				clickOnPicture(list.size - 1, 0)
			}
			else
			{
				clickOnPicture(list.indexOf(url), 0)
			}
		}
		val gridQuantity = gridQuantity.intValue
		//вычисляем позицию в формате таблицы
		val mapOfColumns = mapOfColumns
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
		val cutoutsLocal = cutouts
		val list = picturesUiState.value.picturesUrl
		var gridQuantity = gridQuantity.intValue
		val densityOfScreen = densityOfScreen
		Log.d("cutouts", "${cutoutsLocal.first}")
		var column = mapOfColumns[url]!!
		if(!needsRecalculation)
		{
			if(screenWidth < screenHeight)
			{
				Log.d("pop", "y = $y")
				if(y < 0.1)
				{
					y += 0.04f
				}
				else if(y < 0.3)
				{
					y += 0.084f
				}
				else
				{
					y *= 1.37f
				}
				postPivotsXandY(Pair(x * 1.4f, y))
				Log.d("proverka x, y", "$x, $y, portrait")
			}
			else
			{
				if(cutoutsLocal.first != 0f && cutoutsLocal.second == 0f)
				{
					x = if(column.toDouble() < ceil(gridQuantity.toDouble() / 2))
					{
						1.4f * x - 0.02f * (gridQuantity - column)
					}
					else if(column.toDouble() == ceil(gridQuantity.toDouble() / 2))
					{
						1.4f * x - 0.02f * column
					}
					else
					{
						1.4f * x - 0.04f * (gridQuantity - column + 1) + (gridQuantity - column) * 0.03f - 0.04f
					}
					Log.d("proverka7", "$x, cutout sleva")
				}
				else if(cutoutsLocal.second != 0f)
				{
					x = if(column.toDouble() < ceil(gridQuantity.toDouble() / 2))
					{
						1.4f * x - 0.02f * (gridQuantity - column) - 0.07f
					}
					else if(column.toDouble() == ceil(gridQuantity.toDouble() / 2))
					{
						1.4f * x - 0.02f * column - 0.09f
					}
					else
					{
						1.4f * x - 0.04f * (gridQuantity - column + 1) + (gridQuantity - column) * 0.03f - 0.11f
					}
					Log.d("watafak", "${ceil(gridQuantity.toDouble() / 2)}")
					Log.d("proverka7", "gridQuantity $gridQuantity")
					Log.d("proverka7", "column $column")
					Log.d("proverka7", "$x, cutout sprava")
				}
				else
				{
					x = if(column.toDouble() < ceil(gridQuantity.toDouble() / 2))
					{
						1.4f * x - 0.02f * (gridQuantity - column) - 0.08f
					}
					else if(column.toDouble() == ceil(gridQuantity.toDouble() / 2))
					{
						1.4f * x - 0.02f * column - 0.07f
					}
					else
					{
						1.4f * x - 0.04f * (gridQuantity - column + 1) + (gridQuantity - column) * 0.03f - 0.075f
					}
				}
				Log.d("proverka y", "$y")
				if(setYToDefault)
				{
					y = listOfPositions[0].second / screenHeight + 0.25f
				}
				else if(y <= 0.3f)
				{
					y += 0.23f
				}
				else
				{
					y *= 2f + 0.15f
				}
				Log.d("proverka", "Pair $x , $y")
				postPivotsXandY(Pair(x, y))
			}
		}
		else
		{
			val size = list.size
			Log.d("proverka", " pr cutouts  = $cutoutsLocal")
			if(screenWidth < screenHeight)
			{
				val gridInPortraitQ = gridInPortraitQ
				column = (list.indexOf(url) + 1) % gridInPortraitQ
				if(column == 0)
				{
					column = gridInPortraitQ
				}
				calculatePosition(url)
				Log.d("proverka", "column perevorot $column")
				val xPortrait =
					when(column)
					{
						gridInPortraitQ ->
						{
							0.975f
						}
						1 ->
						{
							0.1f
						}
						else ->
						{
							1f / gridInPortraitQ * column - 0.08f * (gridInPortraitQ - column + 1)
						}
					}
				val maxVisibleElements = maxVisibleElements
				var nY = 0.0121f
				if(size - list.indexOf(url) < maxVisibleElements)
				{
					Log.d("proverka", "rabotaem?")
					val nList = list.subList(size - maxVisibleElements - 1, size - 1)
					Log.d("proverka", "nlist = ${nList.size}")
					var line = nList.indexOf(url).toFloat() / gridInPortraitQ - 1
					Log.d("proverka", "line = $line")
					if(line <= -1f)
					{
						line = 6.1f
					}
					nY = 0.2f / 4f * line

				}
				postPivotsXandY(Pair(xPortrait, nY))
				Log.d("proverka density", "$densityOfScreen")
				Log.d("proverka x, y", "$xPortrait, $nY, portrait, column = $column")
			}
			else
			{
				gridQuantity = (screenWidth / densityOfScreen / 110).toInt()
				Log.d("proverka gridNum", "$gridQuantity")
				if(cutoutsLocal.first != 0f)
				{
					x = if(column == 1)
					{
						-0.1f
					}
					else if(column < gridQuantity / 2)
					{
						0.15f * (column - 1) + column * 0.01f - 0.06f
					}
					else if(column > gridQuantity / 2)
					{
						0.2f * (column - 1) + column * 0.01f - 0.1f
					}
					else
					{
						0.15f * (column - 1) + column * 0.01f
					}
					Log.d("proverka", "$x, cutout sleva")
					Log.d("proverka column", "$column")
				}
				else if(cutoutsLocal.second != 0f)
				{
					x = if(column == 1)
					{
						-0.1f - 0.08f
					}
					else if(column < gridQuantity / 2)
					{
						0.15f * (column - 1) + column * 0.01f - 0.06f - 0.08f
					}
					else if(column > gridQuantity / 2)
					{
						0.2f * (column - 1) + column * 0.01f - 0.1f - 0.08f
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
					x = if(column == 1)
					{
						-0.13f
					}
					else if(column < gridQuantity / 2)
					{
						0.15f * (column - 1) + column * 0.01f - 0.06f
					}
					else if(column > gridQuantity / 2)
					{
						0.2f * (column - 1) + column * 0.01f - 0.1f + 0.15f
					}
					else
					{
						0.15f * (column - 1) + column * 0.01f
					}
					Log.d("pppppppp", "column = $column")
					Log.d("pppppppp", "x = $x")
					Log.d("pppppppp", "cutouta net ili ih dva")
				}
				Log.d("proverka", "Pair $x , 2.09f")
				val yT = if(size - list.indexOf(url) < gridQuantity)
				{
					0.2766f * 2
				}
				else
				{
					0.2766f
				}
				postPivotsXandY(Pair(x, yT))
			}
		}
	}

	fun calculateListPosition(url: String)
	{
		val list = picturesUiState.value.picturesUrl
		val column = mapOfColumns[url]
		val listOfPositions = listOfPositions
		if(column != null && listOfPositions.size > 0)
		{
			val rightY = listOfPositions[0]
			val positionInPx = listOfPositions[column - 1]
			if(url != urlForCalculation)
			{
				val screenHeight = screenHeight
				val screenWidth = screenWidth
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

	fun postCutouts(left: Float, right: Float, needsCheckOnWasChanged: Boolean)
	{
		val newCuts = Pair(left, right)
		if(needsCheckOnWasChanged)
		{
			if(left == right && left != 600f)
			{
				calculatePosition(null)
			}
			else if(cutouts != newCuts)
			{
				cutouts = newCuts
				calculatePosition(null)
			}
		}
		else
		{
			cutouts = newCuts
		}
	}

	fun postSizeOfPic(size: IntSize, maxVisibleElementsNum: Int)
	{
		sizeOfPic = size
		val cofConnectedWithOrientation = cofConnectedWithOrientation
		val cofConnectedWithOrientationForExit = cofConnectedWithOrientationForExit
		if (cofConnectedWithOrientationForExit.floatValue == 0f) {
		val barsSize = barsSize
		val top = barsSize.first
		val bottom = barsSize.second
		val screenWidth = screenWidth
		val screenHeight = screenHeight
		val densityOfScreen = densityOfScreen
		if(screenWidth < screenHeight)
		{
			val sizeOfPicLocal = size.width
			maxVisibleElements = maxVisibleElementsNum
			gridInPortraitQ = gridQuantity.intValue
			if(sizeOfPicLocal != 0)
			{
				cofConnectedWithOrientation.floatValue = sizeOfPicLocal / screenWidth.toFloat()
				cofConnectedWithOrientationForExit.floatValue = sizeOfPicLocal / screenWidth.toFloat()
			}
		}
		else
		{
			val sizeOfPicLocal = size.height
			cofConnectedWithOrientation.floatValue = sizeOfPicLocal / (screenHeight + (-60 - top - bottom) * densityOfScreen) + 0.047f
			cofConnectedWithOrientationForExit.floatValue = sizeOfPicLocal / (screenHeight + (-60 - top - bottom) * densityOfScreen) + 0.047f
		}
		Log.d("pupu2", "${cofConnectedWithOrientation.floatValue} ${cofConnectedWithOrientationForExit.floatValue}")
	}
		}

	fun getGridSpan(): MutableState<Int>
	{
		return gridQuantity
	}

	fun postBars(top: Float, bottom: Float)
	{
		barsSize = Pair(top, bottom)
	}
}