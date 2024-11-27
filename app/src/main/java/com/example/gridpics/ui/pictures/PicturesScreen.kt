package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
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
import com.example.gridpics.ui.activity.MainActivity.Companion.CACHE_IS_SAVED
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFERENCE_GRIDPICS
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFS_PICTURES
import com.example.gridpics.ui.activity.Screen
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import com.example.gridpics.ui.placeholder.NoInternetScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PicturesScreen(
	navController: NavController,
	postPressOnBackButton: () -> Unit,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	getPics: () -> Unit,
	postState: (String) -> Unit,
	state: MutableState<PicturesScreenUiState>,
	clearErrors: () -> Unit,
	postPositiveState: () -> Unit,
	postDefaultUrl: () -> Unit,
	currentPicture: (String) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (String) -> Unit,
)
{
	val context = LocalContext.current
	postPositiveState.invoke()
	postDefaultUrl.invoke()
	BackHandler {
		postPressOnBackButton.invoke()
	}
	val orientation = context.resources.configuration.orientation
	val windowInsets = if(orientation == Configuration.ORIENTATION_LANDSCAPE)
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
				if(!state.value.picturesUrl.isNullOrEmpty() && !state.value.clearedCache)
				{
					ShowList(
						imagesUrlsSP = state.value.picturesUrl,
						checkIfExists = checkIfExists,
						addError = addError,
						getPics = getPics,
						postState = postState,
						state = state,
						clearErrors = clearErrors,
						navController = navController,
						currentPicture = currentPicture,
						isValidUrl = isValidUrl,
						postSavedUrls = postSavedUrls
					)
				}
				else
				{
					ShowList(
						imagesUrlsSP = null,
						checkIfExists = checkIfExists,
						addError = addError,
						getPics = getPics,
						postState = postState,
						state = state,
						clearErrors = clearErrors,
						navController = navController,
						currentPicture = currentPicture,
						isValidUrl = isValidUrl,
						postSavedUrls = postSavedUrls
					)
				}
			}
		}
	)
}

@Composable
fun itemNewsCard(
	item: String,
	navController: NavController,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	getPics: () -> Unit,
	currentPicture: (String) -> Unit,
	isValidUrl: (String) -> Boolean,
	loadingHasBeenEnded: Boolean,
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
		if(loadingHasBeenEnded)
		{
			isClicked = false
			currentPicture(item)
			navController.navigate(Screen.Details.route)
		}
		else
		{
			val txt = stringResource(R.string.wait_for_loading_to_end)
			LaunchedEffect(item) { Toast.makeText(context, txt, Toast.LENGTH_SHORT).show() }
		}
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
						getPics()
						Toast.makeText(context, reloadString, Toast.LENGTH_LONG).show()
					},
					dialogTitle = stringResource(R.string.error_ocurred_loading_img),
					dialogText = stringResource(R.string.error_double_dot) + errorMessage.value + stringResource(R.string.question_retry_again),
					icon = Icons.Default.Warning,
					stringResource(R.string.cancel),
					stringResource(R.string.confirm)
				)
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

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ShowList(
	imagesUrlsSP: String?,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	getPics: () -> Unit,
	postState: (String) -> Unit,
	state: MutableState<PicturesScreenUiState>,
	clearErrors: () -> Unit,
	navController: NavController,
	currentPicture: (String) -> Unit,
	isValidUrl: (String) -> Boolean,
	postSavedUrls: (String) -> Unit,
)
{
	val context = LocalContext.current
	Log.d("PicturesScreen", "From cache? ${!imagesUrlsSP.isNullOrEmpty()}")
	Log.d("We got:", "$imagesUrlsSP")
	val scope = rememberCoroutineScope()
	val listState = rememberLazyGridState()
	if(imagesUrlsSP.isNullOrEmpty())
	{
		when(state.value.loadingState)
		{
			is PicturesState.SearchIsOk ->
			{
				val loadingString = stringResource(R.string.loading_has_been_started)
				Log.d("Now state is", "Loading")
				LaunchedEffect(Unit) {
					Toast.makeText(context, loadingString, Toast.LENGTH_SHORT).show()
					saveToSharedPrefs(context, (state.value.loadingState as PicturesState.SearchIsOk).data)
				}
				val value = remember(state.value.loadingState) { (state.value.loadingState as PicturesState.SearchIsOk).data }
				val list = remember(state.value.loadingState) { value.split("\n") }

				LazyVerticalGrid(
					state = listState,
					modifier = Modifier
						.fillMaxSize(), columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(list) {
						itemNewsCard(
							item = it,
							navController = navController,
							checkIfExists = checkIfExists,
							addError = addError,
							getPics = getPics,
							currentPicture = currentPicture,
							isValidUrl = isValidUrl,
							loadingHasBeenEnded = false
						)
					}
				}
				scope.launch {
					delay(6000)
					postState(value)
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
						clearErrors = clearErrors,
						getPics = getPics
					)
				}
			}
			is PicturesState.NothingFound -> Unit
			is PicturesState.Loaded ->
			{
				val loadingEnded = stringResource(R.string.loading_has_been_ended)
				LaunchedEffect(Unit) {
					Toast.makeText(context, loadingEnded, Toast.LENGTH_SHORT).show()
					postSavedUrls((state.value.loadingState as PicturesState.Loaded).data)
				}
				Log.d("Now state is", "Loaded")
				val list = remember(state.value.loadingState) { ((state.value.loadingState as PicturesState.Loaded).data).split("\n") }
				LazyVerticalGrid(
					state = listState,
					modifier = Modifier
						.fillMaxSize(),
					columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(list) {
						itemNewsCard(
							item = it,
							navController = navController,
							checkIfExists = checkIfExists,
							addError = addError,
							getPics = getPics,
							currentPicture = currentPicture,
							isValidUrl = isValidUrl,
							loadingHasBeenEnded = true
						)
					}
				}
			}
		}
	}
	else
	{
		Log.d("Now state is", "Loaded from sp")
		LaunchedEffect(Unit) {
			saveToSharedPrefs(context, imagesUrlsSP)
		}
		val items = remember(imagesUrlsSP) { imagesUrlsSP.split("\n") }
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
					getPics = getPics,
					currentPicture = currentPicture,
					isValidUrl = isValidUrl,
					loadingHasBeenEnded = true
				)
			}
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
	getPics: () -> Unit,
)
{
	Button(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 32.dp, end = 32.dp),
		onClick = {
			clearErrors()
			getPics.invoke()
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
			Text(text = nameButton, fontSize = 20.sp, color = Color.White)
		}
	}
}

@Composable
fun AlertDialogMain(
	onDismissRequest: () -> Unit,
	onConfirmation: () -> Unit,
	dialogTitle: String,
	dialogText: String,
	icon: ImageVector,
	textButtonCancel: String,
	textButtonConfirm: String,
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

private fun saveToSharedPrefs(context: Context, picturesUrl: String)
{
	val sharedPreferencesPictures = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
	val editorPictures = sharedPreferencesPictures.edit()
	editorPictures.putString(SHARED_PREFS_PICTURES, picturesUrl)
	editorPictures.putBoolean(CACHE_IS_SAVED, false)
	editorPictures.apply()
}

@Composable
private fun calculateGridSpan(): Int
{
	val context = LocalContext.current
	Log.d("HomeFragment", "Calculate span started")
	val width = context.resources.displayMetrics.widthPixels
	val orientation = LocalConfiguration.current.orientation
	val density = context.resources.displayMetrics.density
	return if(orientation == Configuration.ORIENTATION_PORTRAIT)
	{
		((width / density).toInt() / 110)
	}
	else
	{
		((width / density).toInt() / 110)
	}
}