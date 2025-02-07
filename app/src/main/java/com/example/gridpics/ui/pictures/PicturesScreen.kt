@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.example.gridpics.R
import com.example.gridpics.ui.activity.BottomNavigationBar
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import com.example.gridpics.ui.placeholder.NoInternetScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
	animatedVisibilityScope: AnimatedVisibilityScope,
)
{
	LaunchedEffect(Unit) {
		postVisibleBarsState()
	}
	BackHandler {
		postPressOnBackButton()
	}
	val calculatedGridSpan = calculateGridSpan()
	val statusBars = WindowInsets.statusBarsIgnoringVisibility
	val cutouts = WindowInsets.displayCutout
	val value = state.value
	val windowInsets = cutouts.union(statusBars)
	val conf = LocalConfiguration.current
	val mod = if(value.isPortraitOrientation)
	{
		Modifier.fillMaxWidth()
	}
	else
	{
		Modifier.height(400.dp)
	}
	Scaffold(
		contentWindowInsets = windowInsets,
		topBar = {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.windowInsetsPadding(windowInsets)
					.padding(16.dp, 0.dp)
					.height(60.dp)
			) {
				Text(
					textAlign = TextAlign.Center,
					text = stringResource(R.string.gridpics),
					fontSize = 21.sp,
					color = MaterialTheme.colorScheme.onPrimary
				)
			}
		},
		bottomBar = { BottomNavigationBar(navController) },
		content = { padding ->
			Box(
				modifier = mod
					.padding(padding)
					.consumeWindowInsets(padding)
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
					isPortraitOrientation = conf.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT,
					animatedVisibilityScope = animatedVisibilityScope
				)
			}
		}
	)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("FrequentlyChangedStateReadInComposition")
@Composable
fun SharedTransitionScope.ItemsCard(
	item: String,
	getErrorMessageFromErrorsList: (String) -> String?,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	lazyState: LazyGridState,
	addError: (String, String) -> Unit,
	index: Int,
	scope: CoroutineScope,
	isScreenInPortrait: Boolean,
	animatedVisibilityScope: AnimatedVisibilityScope,
)
{
	val width = remember { mutableIntStateOf(0) }
	val height = remember { mutableIntStateOf(0) }
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
			.memoryCachePolicy(CachePolicy.WRITE_ONLY)
			.error(R.drawable.error)
			.build()
	}
	val modifier = remember { mutableStateOf(Modifier.fillMaxSize()) }
	val scale = remember { mutableStateOf(ContentScale.FillHeight) }

	SubcomposeAsyncImage(
		model = (imgRequest),
		contentDescription = item,
		modifier = modifier.value
			.padding(10.dp)
			.sharedElement(
				state = rememberSharedContentState(
					key = item
				),
				animatedVisibilityScope = animatedVisibilityScope,
			)
			.aspectRatio(1f)
			.clickable {
				if(isError)
				{
					openAlertDialog.value = true
				}
				else
				{
					val maxVisibleElements = lazyState.layoutInfo.visibleItemsInfo.size
					scope.launch {
						if(lazyState.layoutInfo.totalItemsCount - index > maxVisibleElements)
						{
							lazyState.scrollToItem(lazyState.firstVisibleItemIndex, 0)
							//Это решает проблему с доворотом списка, если сверху видно только часть картинок в строке,
							// а не целиком всю строку картинок, то список прокрутится наверх
						}
						else
						{
							lazyState.scrollToItem(lazyState.firstVisibleItemIndex + 3, 0)
							//Это решает ту же проблему, только для конца списка, когда поднять список не имеет возможности,
							// потому что могут скрыться нужные элементы снизу, на которые мы нажали
						}
						Log.d("current", item)
						currentPicture(item, lazyState.firstVisibleItemIndex, lazyState.firstVisibleItemScrollOffset)
						openAlertDialog.value = false
					}
				}
			}
			.clip(RoundedCornerShape(8.dp)),
		contentScale = scale.value,
		loading = {
			Box(Modifier.fillMaxSize()) {
				CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
			}
		},
		onError = {
			isError = true
			addError(item, it.result.throwable.message.toString())
		},
		onSuccess = {
			isError = false
			val image = it.result.image
			width.intValue = image.width
			height.intValue = image.height
			val widthLocal = width.intValue
			val heightLocal = height.intValue

			scale.value =
				if(widthLocal < heightLocal && (!isScreenInPortrait || heightLocal - widthLocal > 50))
				{
					ContentScale.FillHeight
				}
				else if(widthLocal > heightLocal)
				{
					ContentScale.FillWidth
				}
				else
				{
					ContentScale.Crop
				}
		}
	)

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
	isPortraitOrientation: Boolean,
	animatedVisibilityScope: AnimatedVisibilityScope,
)
{
	val context = LocalContext.current
	Log.d("PicturesScreen", "From cache? ${!imagesUrlsSP.isNullOrEmpty()}")
	Log.d("We got:", "$imagesUrlsSP")
	val scope = rememberCoroutineScope()
	val listState = rememberLazyGridState()
	if(imagesUrlsSP.isNullOrEmpty())
	{
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
					horizontalArrangement = Arrangement.End,
					state = listState,
					modifier = Modifier
						.fillMaxSize(),
					columns = GridCells.Fixed(count = calculateGridSpan.value)) {
					items(items = list) {
						ItemsCard(
							item = it,
							getErrorMessageFromErrorsList = getErrorMessageFromErrorsList,
							currentPicture = currentPicture,
							isValidUrl = isValidUrl,
							lazyState = listState,
							addError = addError,
							index = list.indexOf(it),
							scope = scope,
							isScreenInPortrait = isPortraitOrientation,
							animatedVisibilityScope = animatedVisibilityScope
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
			horizontalArrangement = Arrangement.End,
			state = listState,
			modifier = Modifier
				.fillMaxSize(),
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
					index = imagesUrlsSP.indexOf(it),
					scope = scope,
					isScreenInPortrait = isPortraitOrientation,
					animatedVisibilityScope = animatedVisibilityScope
				)
			}
		}
	}
	LaunchedEffect(Unit) {
		listState.scrollToItem(index, offset)
		delay(1500)
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
