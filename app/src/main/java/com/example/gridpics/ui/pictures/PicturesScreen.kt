@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.example.gridpics.ui.pictures

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.example.gridpics.R
import com.example.gridpics.ui.activity.BottomNavigationBar
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import com.example.gridpics.ui.placeholder.NoInternetScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.PicturesScreen(
	navController: NavController,
	postPressOnBackButton: () -> Unit,
	getErrorMessageFromErrorsList: (String) -> String?,
	addError: (String, String) -> Unit,
	state: MutableState<PicturesScreenUiState>,
	removeCurrentError: (String) -> Unit,
	postVisibleBarsState: () -> Unit,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (List<String>) -> Unit,
	saveToSharedPrefs: (List<String>) -> Unit,
	pictureSizeInDp: () -> MutableState<Int>,
	postMaxVisibleLinesNum: (Int) -> Unit,
	animatedVisibilityScope: AnimatedVisibilityScope,
	picWasLoadedFromMediaPicker: (Uri) -> Unit,
	isMultiWindowed: Boolean,
	animationIsRunning: MutableState<Boolean>,
	picWasLoadedButAlreadyWasInTheApp: (Uri) -> Unit,
	swapPictures: (String, String) -> Unit,
	deletePictures: (List<String>) -> Unit,
	listState: LazyGridState,
	cancelAllCheckedPics: () -> Unit,
	getPrevClickedItem: () -> String,
	dispose: MutableState<Boolean>,
	getGridNum: () -> Int,
)
{
	DisposableEffect(Unit) {
		dispose.value = false
		onDispose {
			dispose.value = true
		}
	}
	LaunchedEffect(Unit) {
		postVisibleBarsState()
	}
	val calculatedGridSpan = pictureSizeInDp()
	val sysBars = WindowInsets.systemBarsIgnoringVisibility
	val cutouts = WindowInsets.displayCutout
	val value = state.value
	val windowInsets = cutouts.union(sysBars)
	val context = LocalContext.current
	val selectedList = remember { mutableListOf<String>() }
	val showSwapButton = remember { mutableStateOf(false) }
	val showDeleteButton = remember { mutableStateOf(false) }
	val buttonWasPressed = remember { mutableStateOf(false) }
	val isClicked = remember { mutableStateOf(false) }
	val animatedAlpha: Float by animateFloatAsState(if(!isClicked.value) 1f else 0f, label = "alpha")
	Scaffold(
		contentWindowInsets = windowInsets,
		floatingActionButton = {
			val paddingTopValue = if(isMultiWindowed)
			{
				40.dp
			}
			else
			{
				0.dp
			}
			val rippleConfig = remember {
				RippleConfiguration(
					color = Color.LightGray,
					rippleAlpha = RippleAlpha(
						draggedAlpha = 0.1f,
						focusedAlpha = 0f,
						hoveredAlpha = 0.5f,
						pressedAlpha = 0.6f)
				)
			}
			CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
				Box(modifier = Modifier
					.wrapContentSize()
					.padding(top = paddingTopValue, end = windowInsets.asPaddingValues().calculateRightPadding(LayoutDirection.Ltr))
				) {
					AnimatedVisibility(visible = showSwapButton.value) {
						Button(
							contentPadding = PaddingValues(0.dp),
							modifier = Modifier
								.size(65.dp, 65.dp)
								.align(Alignment.CenterStart),
							colors = ButtonColors(
								contentColor = Color.White,
								containerColor = Color.Blue,
								disabledContainerColor = Color.Blue,
								disabledContentColor = Color.White),
							border = BorderStroke(3.dp, Color.LightGray),
							onClick = {
								swapPictures(selectedList[0], selectedList[1])
								buttonWasPressed.value = true
							}
						) {
							Icon(
								modifier = Modifier.size(35.dp, 35.dp),
								painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.compare_arrows_24)),
								contentDescription = "SwapIcon",
							)
						}
					}
					AnimatedVisibility(visible = showDeleteButton.value) {
						Button(
							contentPadding = PaddingValues(0.dp),
							modifier = Modifier
								.padding(top = 50.dp, start = 25.dp)
								.size(50.dp, 50.dp)
								.align(Alignment.TopEnd),
							colors = ButtonColors(
								contentColor = Color.White,
								containerColor = Color.Red,
								disabledContainerColor = Color.Red,
								disabledContentColor = Color.White),
							border = BorderStroke(3.dp, Color.LightGray),
							onClick = {
								deletePictures(selectedList)
								buttonWasPressed.value = true
							}
						) {
							Icon(
								modifier = Modifier.size(25.dp, 25.dp),
								painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_delete)),
								contentDescription = "DeleteIcon"
							)
						}
					}
				}
			}
		},
		topBar = {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.wrapContentHeight()
					.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
					.alpha(animatedAlpha)
					.padding(windowInsets.asPaddingValues())
			) {
				val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia(), onResult = { uri ->
					if(uri != null)
					{
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
						{
							context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
						}
						if(value.picturesUrl.contains(uri.toString()))
						{
							picWasLoadedButAlreadyWasInTheApp(uri)
						}
						else
						{
							picWasLoadedFromMediaPicker(uri)
						}
					}
				})

				Box(Modifier
					.fillMaxWidth()
					.padding(0.dp, 16.dp, 0.dp, 0.dp)) {
					Text(
						modifier = Modifier
							.padding(start = 16.dp)
							.wrapContentSize(),
						textAlign = TextAlign.Center,
						text = stringResource(R.string.gridpics),
						fontSize = 21.sp,
						color = MaterialTheme.colorScheme.onPrimary,
					)
					Button(
						border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
						contentPadding = PaddingValues(0.dp),
						modifier = Modifier
							.size(80.dp, 40.dp)
							.align(Alignment.TopEnd)
							.padding(bottom = 15.dp, end = 16.dp),
						onClick = {
							try
							{
								launcher.launch(PickVisualMediaRequest(
									mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
								))
							}
							catch(e: Exception)
							{
								Log.d("runtime error", "${e.message}, ${e.printStackTrace()}")
								Toast.makeText(context, context.getString(R.string.error_was_catched, e.message), Toast.LENGTH_SHORT).show()
							}
						},
					) {
						val ic = rememberVectorPainter(Icons.Default.AddCircle)
						Icon(
							painter = ic,
							tint = Color.White,
							contentDescription = "AddIcon",
							modifier = Modifier.size(25.dp, 25.dp)
						)
					}
					HorizontalDivider(
						modifier = Modifier
							.align(Alignment.BottomCenter)
							.fillMaxWidth()
							.alpha(0.12f)
							.padding(bottom = 2.dp),
						color = MaterialTheme.colorScheme.onPrimary,
						thickness = 1.5.dp
					)
				}
			}
		},
		bottomBar = {
			Box(modifier = Modifier
				.renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)
				.alpha(animatedAlpha)
			) {
				BottomNavigationBar(navController, state)
			}
		},
		content = { padding ->
			Box(
				modifier = Modifier
					.padding(
						start = padding.calculateStartPadding(LayoutDirection.Ltr),
						top = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding() + 55.dp,
						end = padding.calculateEndPadding(LayoutDirection.Rtl),
						bottom = padding.calculateBottomPadding()
					)
					.fillMaxSize()
			) {
				val urls = value.picturesUrl
				val loadingState = value.loadingState
				val offset = value.offset
				val index = value.index
				ShowList(
					imagesUrlsSP = urls,
					getErrorMessageFromErrorsList = getErrorMessageFromErrorsList,
					addError = addError,
					state = loadingState,
					removeCurrentError = removeCurrentError,
					currentPicture = currentPicture,
					isValidUrl = isValidUrl,
					postSavedUrls = postSavedUrls,
					saveToSharedPrefs = saveToSharedPrefs,
					offset = offset,
					index = index,
					pictureSizeInDp = calculatedGridSpan,
					postMaxVisibleLinesNum = postMaxVisibleLinesNum,
					animatedVisibilityScope = animatedVisibilityScope,
					isMultiWindowed = isMultiWindowed,
					animationIsRunning = animationIsRunning,
					selectedList = selectedList,
					showSwapButton = showSwapButton,
					showDeleteButton = showDeleteButton,
					buttonWasPressed = buttonWasPressed,
					listState = listState,
					postPressOnBackButton = postPressOnBackButton,
					cancelAllCheckedPics = cancelAllCheckedPics,
					getPrevClickedItem = getPrevClickedItem,
					isClicked = isClicked,
					getGridNum = getGridNum
				)
			}
		}
	)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedTransitionScope.ItemsCard(
	item: String,
	getErrorMessageFromErrorsList: (String) -> String?,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	lazyState: LazyGridState,
	addError: (String, String) -> Unit,
	animatedVisibilityScope: AnimatedVisibilityScope,
	list: List<String>,
	getGridNum: () -> Int,
	isMultiWindowed: Boolean,
	selectedCounter: MutableIntState,
	selectedList: MutableList<String>,
	removeCurrentError: (String) -> Unit,
	isClicked: MutableState<Boolean>,
	currentClickedItem: MutableState<String>,
	getPrevClickedItem: () -> String,
	animationIsRunning: MutableState<Boolean>,
)
{
	var isError by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val openAlertDialog = remember { mutableStateOf(false) }
	val errorMessage = remember { mutableStateOf("") }
	var placeholder = R.drawable.loading
	var data = item
	val errorMessageFromErrorsList = getErrorMessageFromErrorsList(item)
	if(errorMessageFromErrorsList != null)
	{
		data = ""
		placeholder = R.drawable.error
		isError = true
		errorMessage.value = errorMessageFromErrorsList
	}
	if(item.contains(".gif") && errorMessageFromErrorsList == null)
	{
		placeholder = R.drawable.empty
		LaunchedEffect(Unit) {
			delay(150)

			placeholder = R.drawable.loading
		}
	}
	val headers = remember {
		NetworkHeaders.Builder()
			.set("Cache-Control", "max-age=604800, must-revalidate, stale-while-revalidate=86400")
			.build()
	}
	val imgRequest = remember(item) {
		ImageRequest.Builder(context)
			.data(data)
			.httpHeaders(headers)
			.placeholder(placeholder)
			.error(R.drawable.error)
			.placeholderMemoryCacheKey(data)
			.memoryCacheKey(item)
			.diskCacheKey(item).defaults(ImageRequest.Defaults.DEFAULT)
			.build()
	}
	Log.d("recompose", "picture $item")
	val imageIsSelected = remember(item) { mutableStateOf(selectedList.contains(item)) }
	val prevClickedItem = getPrevClickedItem()
	Log.d("0", prevClickedItem)
	//логика настройки модификатора у картинки, чтобы можно было отменять анимацию по клику на другую картинку или ту же самую и
	//запускать другую анимацию
	AsyncImage(
		model = (imgRequest),
		filterQuality = FilterQuality.Low,
		contentDescription = item,
		modifier = Modifier
			.sharedElement(
				state = rememberSharedContentState(
					key = list.indexOf(item)
				),
				animatedVisibilityScope = animatedVisibilityScope
			)
			.fillMaxSize()
			.padding(8.dp)
			.aspectRatio(1f)
			.combinedClickable(
				enabled = !animationIsRunning.value || prevClickedItem != item,
				onClick = {
					if(selectedCounter.intValue == 0)
					{
						if(!isClicked.value)
						{
							if(isError)
							{
								openAlertDialog.value = true
							}
							else
							{
								currentClickedItem.value = item
								isClicked.value = true
								Log.d("current", item)
								//логика для подлистывания списка, если какая-то картинка скрыта интерфейсом, но при этом была нажата
								val firstVisibleIndex = lazyState.firstVisibleItemIndex
								val offsetOfList = lazyState.firstVisibleItemScrollOffset
								val lazyLayoutInfo = lazyState.layoutInfo
								val visibleItemsNum = lazyLayoutInfo.visibleItemsInfo.size
								val gridNum = getGridNum()
								if(isMultiWindowed)
								{
									currentPicture(item, list.indexOf(item), 0)
								}
								else if(offsetOfList != 0 && list.indexOf(item) < firstVisibleIndex + gridNum)
								{
									Log.d("check listScroll", "firstVisibleIndex")
									currentPicture(item, firstVisibleIndex, 0)
								}
								else if(
									(list.indexOf(item) - firstVisibleIndex >= visibleItemsNum - gridNum
										&& list.indexOf(item) < firstVisibleIndex + visibleItemsNum)
									&& (lazyLayoutInfo.totalItemsCount - list.indexOf(item) > visibleItemsNum))
								{
									Log.d("check listScroll", "firstVisible +gridnum")
									currentPicture(item, firstVisibleIndex + gridNum, offsetOfList)
								}
								else
								{
									Log.d("check listScroll", "just click $firstVisibleIndex, $offsetOfList")
									currentPicture(item, firstVisibleIndex, offsetOfList)
								}
								openAlertDialog.value = false
							}
						}
					}
					else
					{
						onLongPictureClick(
							imageIsSelected = imageIsSelected,
							selectedCounter = selectedCounter,
							selectedList = selectedList,
							item = item
						)
					}
				},
				onLongClick = {
					onLongPictureClick(
						imageIsSelected = imageIsSelected,
						selectedCounter = selectedCounter,
						selectedList = selectedList,
						item = item
					)
				}
			)
			.clip(RoundedCornerShape(8.dp))
			.fillMaxSize(),
		contentScale = ContentScale.Fit,
		onError = {
			isError = true
			addError(item, it.result.throwable.message.toString())
		},
		onSuccess = {
			isError = false
		}
	)
	AnimatedVisibility(visible = imageIsSelected.value) {
		Box(modifier = Modifier
			.clip(RoundedCornerShape(30.dp))
			.background(MaterialTheme.colorScheme.primary)) {
			Icon(
				modifier = Modifier
					.size(40.dp, 40.dp)
					.clip(RoundedCornerShape(8.dp))
					.align(Alignment.CenterStart),
				painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.icon_check)),
				contentDescription = "CheckIcon",
				tint = Color.White
			)
		}
	}

	if(openAlertDialog.value)
	{
		if(isValidUrl(item))
		{
			val reloadString = stringResource(R.string.reload)
			AlertDialogMain(
				onDismissRequest = { openAlertDialog.value = false },
				onConfirmation =
				{
					removeCurrentError(item)
					openAlertDialog.value = false
					println("Confirmation registered")
					Toast.makeText(context, reloadString, Toast.LENGTH_LONG).show()
				},
				dialogTitle = stringResource(R.string.error_ocurred_loading_img),
				dialogText = stringResource(R.string.error_double_dot) + errorMessage.value + stringResource(R.string.question_retry_again),
				icon = Icons.Default.Warning,
				textButtonCancel = stringResource(R.string.cancel),
				textButtonConfirm = stringResource(R.string.confirm))
		}
		else
		{
			AlertDialogSecondary(
				onDismissRequest = { openAlertDialog.value = false },
				onConfirmation =
				{
					openAlertDialog.value = false
				},
				dialogTitle = stringResource(R.string.error_ocurred_loading_img), dialogText = stringResource(R.string.link_is_not_valid), icon = Icons.Default.Warning)
		}
	}
}

@Composable
fun SharedTransitionScope.ShowList(
	imagesUrlsSP: List<String>?,
	getErrorMessageFromErrorsList: (String) -> String?,
	addError: (String, String) -> Unit,
	state: PicturesState,
	removeCurrentError: (String) -> Unit,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (List<String>) -> Unit,
	saveToSharedPrefs: (List<String>) -> Unit,
	offset: Int,
	index: Int,
	pictureSizeInDp: MutableState<Int>,
	postMaxVisibleLinesNum: (Int) -> Unit,
	animatedVisibilityScope: AnimatedVisibilityScope,
	isMultiWindowed: Boolean,
	animationIsRunning: MutableState<Boolean>,
	selectedList: MutableList<String>,
	showDeleteButton: MutableState<Boolean>,
	showSwapButton: MutableState<Boolean>,
	buttonWasPressed: MutableState<Boolean>,
	listState: LazyGridState,
	postPressOnBackButton: () -> Unit,
	cancelAllCheckedPics: () -> Unit,
	getPrevClickedItem: () -> String,
	isClicked: MutableState<Boolean>,
	getGridNum: () -> Int,
)
{
	Log.d("PicturesScreen", "From cache? ${!imagesUrlsSP.isNullOrEmpty()}")
	val context = LocalContext.current
	Log.d("We got:", "$imagesUrlsSP")
	LaunchedEffect(Unit) {
		if(animationIsRunning.value)
		{
			val animatorScale = Settings.Global.getFloat(
				context.contentResolver,
				Settings.Global.ANIMATOR_DURATION_SCALE,
				1f
			)
			delay((animatedVisibilityScope.transition.totalDurationNanos.toFloat() * animatorScale / 1000000).toLong()) //перевод в милисекунды
			animationIsRunning.value = false
		}
	}
	val selectedCounter = remember { mutableIntStateOf(0) }
	val currentClickedItem = remember { mutableStateOf("") }
	if(buttonWasPressed.value)
	{
		selectedCounter.intValue = 0
		selectedList.removeAll(selectedList)
		buttonWasPressed.value = false
	}
	showDeleteButton.value = selectedCounter.intValue > 0
	showSwapButton.value = selectedCounter.intValue == 2
	LaunchedEffect(Unit) {
		animationIsRunning.value = true
		val animatorScale = Settings.Global.getFloat(
			context.contentResolver,
			Settings.Global.ANIMATOR_DURATION_SCALE,
			1f
		)
		delay((animatedVisibilityScope.transition.totalDurationNanos.toFloat() * animatorScale / 1000000).toLong()) //перевод в милисекунды
		animationIsRunning.value = false
	}
		if(imagesUrlsSP.isNullOrEmpty())
		{
			when(state)
			{
				is PicturesState.SearchIsOk ->
				{
					Log.d("Now state is", "Search Is Ok")
					val list = state.data
					LaunchedEffect(Unit) {
						saveToSharedPrefs(list)
					}
					LazyVerticalGrid(
						horizontalArrangement = Arrangement.SpaceAround,
						state = listState,
						modifier = Modifier
							.fillMaxSize(),
						userScrollEnabled = !animationIsRunning.value,
						columns = GridCells.FixedSize(pictureSizeInDp.value.dp)) {
						Log.d("PicturesFragment", "$imagesUrlsSP")
						items(items = list) {
							ItemsCard(
								item = it,
								getErrorMessageFromErrorsList = getErrorMessageFromErrorsList,
								currentPicture = currentPicture,
								isValidUrl = isValidUrl,
								lazyState = listState,
								addError = addError,
								animatedVisibilityScope = animatedVisibilityScope,
								list = list,
								getGridNum = getGridNum,
								isMultiWindowed = isMultiWindowed,
								selectedCounter = selectedCounter,
								selectedList = selectedList,
								removeCurrentError = removeCurrentError,
								isClicked = isClicked,
								currentClickedItem = currentClickedItem,
								getPrevClickedItem = getPrevClickedItem,
								animationIsRunning = animationIsRunning
							)
						}
					}
				}
				is PicturesState.ConnectionError ->
				{
					Log.d("Net", "No internet")
					Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
						NoInternetScreen()
						val gradientColor = remember { listOf(Color.Green, Color.Yellow) }
						GradientButton(
							gradientColors = gradientColor,
							cornerRadius = 16.dp,
							nameButton = stringResource(R.string.try_again),
							roundedCornerShape = RoundedCornerShape(topStart = 30.dp, bottomEnd = 30.dp),
							removeCurrentError = removeCurrentError,
							url = ""
						)
					}
				}
				is PicturesState.NothingFound -> Unit
			}
		}
		else
		{
			Log.d("Now state is", "Loaded from sp")
			LaunchedEffect(Unit) {
				saveToSharedPrefs(imagesUrlsSP)
				postSavedUrls(imagesUrlsSP)
			}
			LazyVerticalGrid(
				horizontalArrangement = Arrangement.SpaceAround,
				state = listState,
				modifier = Modifier
					.fillMaxSize(),
				userScrollEnabled = !animationIsRunning.value,
				columns = GridCells.FixedSize(pictureSizeInDp.value.dp)) {
				Log.d("PicturesFragment", "$imagesUrlsSP")
				items(items = imagesUrlsSP) {
					ItemsCard(
						item = it,
						getErrorMessageFromErrorsList = getErrorMessageFromErrorsList,
						currentPicture = currentPicture,
						isValidUrl = isValidUrl,
						lazyState = listState,
						addError = addError,
						animatedVisibilityScope = animatedVisibilityScope,
						list = imagesUrlsSP,
						getGridNum = getGridNum,
						isMultiWindowed = isMultiWindowed,
						selectedCounter = selectedCounter,
						selectedList = selectedList,
						removeCurrentError = removeCurrentError,
						isClicked = isClicked,
						currentClickedItem = currentClickedItem,
						getPrevClickedItem = getPrevClickedItem,
						animationIsRunning = animationIsRunning
					)
				}
			}
		}

		LaunchedEffect(Unit) {
			postMaxVisibleLinesNum(listState.layoutInfo.visibleItemsInfo.size)
			Log.d("check listScroll", "scrolled $index, $offset")
			listState.scrollToItem(index, offset)
		}
		BackHandler {
			if(selectedList.size > 0)
			{
				buttonWasPressed.value = true
				cancelAllCheckedPics()
			}
			else
			{
				postPressOnBackButton()
			}
		}
}

@Composable
fun GradientButton(
	gradientColors: List<Color>,
	cornerRadius: Dp,
	nameButton: String,
	roundedCornerShape: RoundedCornerShape,
	removeCurrentError: (String) -> Unit,
	url: String,
)
{
	Button(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 32.dp, end = 32.dp),
		onClick = {
			removeCurrentError(url)
		},
		colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
		shape = RoundedCornerShape(cornerRadius)
	)
	{
		Box(modifier = Modifier
			.fillMaxWidth()
			.background(
				brush = Brush.horizontalGradient(colors = gradientColors),
				shape = roundedCornerShape
			)
			.clip(roundedCornerShape)
			.background(
				brush = Brush.linearGradient(colors = gradientColors),
				shape = RoundedCornerShape(cornerRadius)
			)
			.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center)
		{
			Text(
				text = nameButton,
				fontSize = 20.sp,
				color = Color.White
			)
		}
	}
}

@Composable
fun AlertDialogMain(
	onDismissRequest: () -> Unit,
	onConfirmation: () -> Unit,
	dialogTitle: String,
	dialogText: String?,
	icon: ImageVector,
	textButtonCancel: String,
	textButtonConfirm: String,
)
{
	AlertDialog(
		icon = {
			Icon(icon, contentDescription = "Example Icon")
		}, title = {
			Text(text = dialogTitle)
		}, text = {
			dialogText?.let { Text(text = it) }
		}, onDismissRequest = {
			onDismissRequest()
		}, confirmButton = {
			Button(colors = ButtonColors(Color.Black, Color.White, Color.Black, Color.White), onClick = {
				onConfirmation()
			}) {
				Text(textButtonConfirm)
			}
		}, dismissButton = {
			Button(
				colors = ButtonColors(MaterialTheme.colorScheme.onError, Color.White, Color.Black, Color.White),
				onClick = {
					onDismissRequest()
				},
			) {
				Text(textButtonCancel)
			}
		})
}

@Composable
fun AlertDialogSecondary(
	onDismissRequest: () -> Unit,
	onConfirmation: () -> Unit,
	dialogTitle: String,
	dialogText: String,
	icon: ImageVector,
)
{
	AlertDialog(icon = {
		Icon(icon, contentDescription = "Example Icon")
	}, title = {
		Text(text = dialogTitle)
	}, text = {
		Text(text = dialogText)
	}, onDismissRequest = {
		onDismissRequest()
	}, confirmButton = {
		Button(colors = ButtonColors(Color.Black, Color.White, Color.Black, Color.White), onClick = {
			onConfirmation()
		}) {
			Text(stringResource(R.string.okey))
		}
	})
}

fun onLongPictureClick(
	imageIsSelected: MutableState<Boolean>,
	selectedCounter: MutableIntState,
	selectedList: MutableList<String>,
	item: String
)
{
	imageIsSelected.value = !imageIsSelected.value
	if(imageIsSelected.value)
	{
		selectedCounter.intValue += 1
		selectedList.add(item)
	}
	else
	{
		selectedCounter.intValue -= 1
		selectedList.remove(item)
	}
}
