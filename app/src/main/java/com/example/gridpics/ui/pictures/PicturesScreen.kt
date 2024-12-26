package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.error
import coil3.request.placeholder
import com.example.gridpics.R
import com.example.gridpics.ui.activity.BottomNavigationBar
import com.example.gridpics.ui.activity.MainActivity.Companion.LENGTH_OF_PICTURE
import com.example.gridpics.ui.activity.Screen
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import com.example.gridpics.ui.placeholder.NoInternetScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PicturesScreen(
	navController: NavController,
	postPressOnBackButton: () -> Unit,
	checkOnErrorExists: (String) -> Boolean,
	addError: (String) -> Unit,
	postState: (Boolean, String) -> Unit,
	state: MutableState<PicturesScreenUiState>,
	clearErrors: () -> Unit,
	postVisibleBarsState: () -> Unit,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (String) -> Unit,
	saveToSharedPrefs: (String) -> Unit,
)
{
	LaunchedEffect(Unit) {
		postVisibleBarsState()
	}
	BackHandler {
		postPressOnBackButton()
	}
	val value = state.value
	val windowInsets = if(!value.isPortraitOrientation)
	{
		WindowInsets.displayCutout.union(WindowInsets.statusBarsIgnoringVisibility)
	}
	else
	{
		WindowInsets.statusBarsIgnoringVisibility
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
			Column(
				modifier = Modifier
					.padding(padding)
					.consumeWindowInsets(padding)
					.fillMaxSize()
			) {
				val urls = value.picturesUrl
				val loadingState = value.loadingState
				val offset = value.offset
				val index = value.index
				ShowList(
					imagesUrlsSP = urls.ifEmpty {
						null
					},
					checkIfExists = checkOnErrorExists,
					addError = addError,
					postState = postState,
					state = loadingState,
					clearErrors = clearErrors,
					navController = navController,
					currentPicture = currentPicture,
					isValidUrl = isValidUrl,
					postSavedUrls = postSavedUrls,
					saveToSharedPrefs = saveToSharedPrefs,
					offset = offset,
					index = index
				)
			}
		}
	)
}

@SuppressLint("FrequentlyChangedStateReadInComposition")
@Composable
fun itemNewsCard(
	item: String,
	navController: NavController,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	postState: (Boolean, String) -> Unit,
	urls: String,
	postSavedUrls: (String) -> Unit,
	lazyState: LazyGridState,
): Boolean
{
	var isError by remember { mutableStateOf(false) }
	val context = LocalContext.current
	var isClicked by remember { mutableStateOf(false) }
	val openAlertDialog = remember { mutableStateOf(false) }
	val errorMessage = remember { mutableStateOf("") }
	var placeholder = R.drawable.loading
	if(checkIfExists(item))
	{
		placeholder = R.drawable.error
		isError = true
	}
	val headers = NetworkHeaders.Builder()
		.set("Cache-Control", "max-age=604800, must-revalidate, stale-while-revalidate=86400")
		.build()
	val imgRequest = remember(item) {
		ImageRequest.Builder(context)
			.data(item)
			.allowHardware(false)
			.httpHeaders(headers)
			.networkCachePolicy(CachePolicy.ENABLED)
			.memoryCachePolicy(CachePolicy.ENABLED)
			.fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(4))
			.interceptorCoroutineContext(Dispatchers.IO.limitedParallelism(4))
			.coroutineContext(Dispatchers.IO.limitedParallelism(4))
			.diskCachePolicy(CachePolicy.ENABLED)
			.placeholder(placeholder)
			.error(R.drawable.error)
			.build()
	}
	AsyncImage(
		model = (imgRequest),
		contentDescription = item,
		modifier = Modifier
			.clickable {
				if(!isError)
				{
					isClicked = true
					openAlertDialog.value = false
				}
				else
				{
					openAlertDialog.value = true
				}
			}
			.padding(10.dp)
			.size(100.dp)
			.clip(RoundedCornerShape(8.dp)),
		contentScale = ContentScale.Crop,
		onError = {
			isError = true
			errorMessage.value = it.result.throwable.message.toString()
			addError(item)
		},
		onSuccess = {
			isError = false
		}
	)
	if(isClicked)
	{
		Log.d("current", item)
		postSavedUrls(urls)
		currentPicture(item, lazyState.firstVisibleItemIndex, lazyState.firstVisibleItemScrollOffset)
		navController.navigate(Screen.Details.route) {
			popUpTo(Screen.Home.route) {
				inclusive = true
			}
		}
		isClicked = false
	}
	when
	{
		openAlertDialog.value ->
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
						postState(false, urls)
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
	return isError
}

@Composable
fun ShowList(
	imagesUrlsSP: String?,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	postState: (Boolean, String) -> Unit,
	state: PicturesState,
	clearErrors: () -> Unit,
	navController: NavController,
	currentPicture: (String, Int, Int) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (String) -> Unit,
	saveToSharedPrefs: (String) -> Unit,
	offset: Int,
	index: Int,
)
{
	val context = LocalContext.current
	Log.d("PicturesScreen", "From cache? ${!imagesUrlsSP.isNullOrEmpty()}")
	Log.d("We got:", "$imagesUrlsSP")
	val canChangeState = remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()
	val listState = rememberLazyGridState()
	if(imagesUrlsSP.isNullOrEmpty())
	{
		when(state)
		{
			is PicturesState.SearchIsOk ->
			{
				val loadingString = stringResource(R.string.loading_has_been_started)
				Log.d("Now state is", "Search Is Ok")
				LaunchedEffect(state) {
					saveToSharedPrefs(state.data)
					Toast.makeText(context, loadingString, Toast.LENGTH_SHORT).show()
				}
				val value = remember(state) { state.data }
				val list = remember(state) { value.split("\n") }
				postSavedUrls(value)
				LazyVerticalGrid(
					state = listState,
					modifier = Modifier
						.fillMaxSize(),
					columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(items = list) {
						itemNewsCard(
							item = it,
							navController = navController,
							checkIfExists = checkIfExists,
							addError = addError,
							currentPicture = currentPicture,
							isValidUrl = isValidUrl,
							postState = postState,
							urls = value,
							postSavedUrls = postSavedUrls,
							lazyState = listState
						)
					}
				}
				LaunchedEffect(state) {
					scope.launch {
						delay(6000)
						postState(true, value)
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
			is PicturesState.Loaded ->
			{
				Log.d("Now state is", "Loaded")
				val loadingEnded = stringResource(R.string.loading_has_been_ended)
				LaunchedEffect(Unit) {
					Toast.makeText(context, loadingEnded, Toast.LENGTH_SHORT).show()
					postSavedUrls(state.data)
				}
				val value = remember(state) { state.data }
				Log.d("Now state is", "Loaded")
				val list = remember(state) { (state.data).split("\n") }
				LazyVerticalGrid(
					state = listState,
					modifier = Modifier
						.fillMaxSize(),
					columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(items = list) {
						itemNewsCard(
							item = it,
							navController = navController,
							checkIfExists = checkIfExists,
							addError = addError,
							currentPicture = currentPicture,
							isValidUrl = isValidUrl,
							postState = postState,
							urls = value,
							postSavedUrls = postSavedUrls,
							lazyState = listState
						)
					}
				}
				canChangeState.value = true
			}
		}
	}
	else
	{
		Log.d("Now state is", "Loaded from sp")
		LaunchedEffect(Unit) {
			saveToSharedPrefs(imagesUrlsSP)
			postSavedUrls(imagesUrlsSP)
		}
		val items = remember(imagesUrlsSP) { imagesUrlsSP.split("\n").toSet().toList() }
		Log.d("item", items.toString())
		LazyVerticalGrid(
			state = listState,
			modifier = Modifier
				.fillMaxSize(),
			columns = GridCells.Fixed(count = calculateGridSpan())) {
			Log.d("PicturesFragment", "$items")
			items(items) {
				itemNewsCard(
					item = it,
					navController = navController,
					checkIfExists = checkIfExists,
					addError = addError,
					currentPicture = currentPicture,
					isValidUrl = isValidUrl,
					postState = postState,
					urls = imagesUrlsSP,
					postSavedUrls = postSavedUrls,
					lazyState = listState
				)
			}
		}
	}
	LaunchedEffect(Unit) {
		scope.launch {
			listState.scrollToItem(index, offset)
		}
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

@Composable
private fun calculateGridSpan(): Int
{
	val resources = LocalContext.current.resources.displayMetrics
	Log.d("HomeFragment", "Calculate span started")
	val width = resources.widthPixels
	val orientation = LocalConfiguration.current.orientation
	val density = resources.density
	return if(orientation == Configuration.ORIENTATION_PORTRAIT)
	{
		(width / density).toInt() / LENGTH_OF_PICTURE
	}
	else
	{
		(width / density).toInt() / LENGTH_OF_PICTURE
	}
}