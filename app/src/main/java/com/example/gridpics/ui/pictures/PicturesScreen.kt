@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.example.gridpics.ui.pictures

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PicturesScreen(
	navController: NavController,
	postPressOnBackButton: () -> Unit,
	getErrorMessageFromErrorsList: (String) -> String?,
	addError: (String, String) -> Unit,
	state: MutableState<PicturesScreenUiState>,
	clearErrors: () -> Unit,
	postVisibleBarsState: () -> Unit,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (List<String>) -> Unit,
	saveToSharedPrefs: (List<String>) -> Unit,
	calculateGridSpan: () -> MutableState<Int>,
	postMaxVisibleLinesNum: (Int) -> Unit,
	animatedVisibilityScope: AnimatedVisibilityScope,
	picWasLoadedFromMediaPicker: (Uri) -> Unit,
)
{
	LaunchedEffect(Unit) {
		postVisibleBarsState()
	}
	BackHandler {
		postPressOnBackButton()
	}
	val calculatedGridSpan = calculateGridSpan()
	val sysBars = WindowInsets.systemBarsIgnoringVisibility
	val cutouts = WindowInsets.displayCutout
	val value = state.value
	val windowInsets = cutouts.union(sysBars)
	val context = LocalContext.current
	Scaffold(
		contentWindowInsets = windowInsets,
		topBar = {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.wrapContentHeight()
					.windowInsetsPadding(windowInsets)
					.padding(16.dp, 0.dp)
			) {
				val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia(), onResult = { uri ->
					if(uri != null)
					{
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
						{
							context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
						}
						picWasLoadedFromMediaPicker(uri)
					}
				})
				Box(Modifier
					.fillMaxWidth()
					.padding(0.dp, 16.dp, 0.dp)) {
					Text(
						modifier = Modifier.wrapContentSize(),
						textAlign = TextAlign.Center,
						text = stringResource(R.string.gridpics),
						fontSize = 21.sp,
						color = MaterialTheme.colorScheme.onPrimary,
					)
					Button(
						modifier = Modifier
							.size(60.dp, 30.dp)
							.align(Alignment.TopEnd),
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
							modifier = Modifier.fillMaxSize())
					}
				}
			}
		},
		bottomBar = { BottomNavigationBar(navController) },
		content = { padding ->
			Box(
				modifier = Modifier
					.padding(padding.calculateStartPadding(LayoutDirection.Ltr), 100.dp, padding.calculateEndPadding(LayoutDirection.Rtl), padding.calculateBottomPadding())
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
					clearErrors = clearErrors,
					currentPicture = currentPicture,
					isValidUrl = isValidUrl,
					postSavedUrls = postSavedUrls,
					saveToSharedPrefs = saveToSharedPrefs,
					offset = offset,
					index = index,
					calculateGridSpan = calculatedGridSpan,
					postMaxVisibleLinesNum = postMaxVisibleLinesNum,
					animatedVisibilityScope = animatedVisibilityScope,
				)
			}
		}
	)
}

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
	gridNum: Int,
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
	Box(Modifier
		.fillMaxSize()
		.aspectRatio(1f)) {
		AsyncImage(
			model = (imgRequest),
			filterQuality = FilterQuality.Low,
			contentDescription = item,
			modifier = Modifier
				.padding(8.dp)
				.sharedElement(
					state = rememberSharedContentState(
						key = list.indexOf(item)
					),
					animatedVisibilityScope = animatedVisibilityScope)
				.clip(RoundedCornerShape(8.dp))
				.fillMaxSize()
				.clickable {
					if(isError)
					{
						openAlertDialog.value = true
					}
					else
					{
						Log.d("current", item)
						//логика для подлистывания списка, если какая-то картинка скрыта интерфейсом, но при этом была нажата
						val firstVisibleIndex = lazyState.firstVisibleItemIndex
						val offsetOfList = lazyState.firstVisibleItemScrollOffset
						val visibleItemsNum = lazyState.layoutInfo.visibleItemsInfo.size
						if(offsetOfList != 0 && list.indexOf(item) < firstVisibleIndex + gridNum)
						{
							Log.d("check listScroll", "firstVisibleIndex")
							currentPicture(item, firstVisibleIndex, 0)
						}
						else if(
							(list.indexOf(item) - firstVisibleIndex >= visibleItemsNum - gridNum
								&& list.indexOf(item) < firstVisibleIndex + visibleItemsNum)
							&& (lazyState.layoutInfo.totalItemsCount - list.indexOf(item) > visibleItemsNum))
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
				},
			contentScale = ContentScale.Fit,
			onError = {
				isError = true
				addError(item, it.result.throwable.message.toString())
			},
			onSuccess = {
				isError = false
			}
		)
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
	clearErrors: () -> Unit,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (List<String>) -> Unit,
	saveToSharedPrefs: (List<String>) -> Unit,
	offset: Int,
	index: Int,
	calculateGridSpan: MutableState<Int>,
	postMaxVisibleLinesNum: (Int) -> Unit,
	animatedVisibilityScope: AnimatedVisibilityScope,
)
{
	Log.d("PicturesScreen", "From cache? ${!imagesUrlsSP.isNullOrEmpty()}")
	Log.d("We got:", "$imagesUrlsSP")
	val listState = remember(LocalConfiguration.current.orientation) { LazyGridState() }
	if(imagesUrlsSP.isNullOrEmpty())
	{
		val context = LocalContext.current
		when(state)
		{
			is PicturesState.SearchIsOk ->
			{
				Log.d("Now state is", "Search Is Ok")
				val list = state.data
				LaunchedEffect(Unit) {
					Toast.makeText(context, R.string.loading_has_been_started, Toast.LENGTH_SHORT).show()
					saveToSharedPrefs(list)
				}
				LazyVerticalGrid(
					horizontalArrangement = Arrangement.Center,
					state = listState,
					modifier = Modifier
						.fillMaxSize(),
					userScrollEnabled = !animatedVisibilityScope.transition.isRunning,
					columns = GridCells.Fixed(count = calculateGridSpan.value)) {
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
							gridNum = calculateGridSpan.value,
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
						clearErrors = clearErrors
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
			horizontalArrangement = Arrangement.Absolute.Center,
			state = listState,
			modifier = Modifier
				.fillMaxSize(),
			userScrollEnabled = !animatedVisibilityScope.transition.isRunning,
			columns = GridCells.Fixed(count = calculateGridSpan.value)) {
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
					gridNum = calculateGridSpan.value,
				)
			}
		}
	}
	LaunchedEffect(Unit) {
		postMaxVisibleLinesNum(listState.layoutInfo.visibleItemsInfo.size)
		Log.d("check listScroll", "scrolled $index, $offset")
		listState.scrollToItem(index, offset)
	}
}

@Composable
fun GradientButton(
	gradientColors: List<Color>,
	cornerRadius: Dp,
	nameButton: String,
	roundedCornerShape: RoundedCornerShape,
	clearErrors: () -> Unit,
)
{
	Button(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 32.dp, end = 32.dp),
		onClick = {
			clearErrors()
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
