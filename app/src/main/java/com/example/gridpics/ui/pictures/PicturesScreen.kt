package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
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
import com.example.gridpics.ui.activity.MainActivity.Companion.CACHE
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURE
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFERENCE_GRIDPICS
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFS_PICTURES
import com.example.gridpics.ui.activity.Screen
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
	state: MutableState<PictureState>,
	clearErrors: () -> Unit,
	postPositiveState: () -> Unit,
	postDefaultUrl: () -> Unit,
	newState: () -> Unit,
	sharedPrefsPictures: String?,
	clearedCache: Boolean,
	currentPicture: (String) -> Unit
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
		WindowInsets.displayCutout.union(WindowInsets.systemBarsIgnoringVisibility)
	}
	else
	{
		WindowInsets.systemBarsIgnoringVisibility
	}
	Scaffold(
		modifier = Modifier.fillMaxSize(),
		bottomBar = { BottomNavigationBar(navController) },
		contentWindowInsets = windowInsets,
		content = { padding ->
			Column(
				modifier = Modifier
					.padding(padding)
					.consumeWindowInsets(padding)
					.fillMaxSize()
			) {
				if(!sharedPrefsPictures.isNullOrEmpty() && !clearedCache)
				{
					newState()
					ShowPictures(
						savedPicsUrls = sharedPrefsPictures,
						checkIfExists = checkIfExists,
						addError = addError,
						getPics = getPics,
						postState = postState,
						state = state,
						clearErrors = clearErrors,
						navController = navController,
						currentPicture = currentPicture
					)
				}
				else if(!clearedCache && sharedPrefsPictures.isNullOrEmpty())
				{
					newState()
					getPics()
					ShowPictures(
						savedPicsUrls = sharedPrefsPictures,
						checkIfExists = checkIfExists,
						addError = addError,
						getPics = getPics,
						postState = postState,
						state = state,
						clearErrors = clearErrors,
						navController = navController,
						currentPicture = currentPicture
					)
				}
				else if(clearedCache || sharedPrefsPictures.isNullOrEmpty())
				{
					newState()
					getPics()
					ShowPictures(
						savedPicsUrls = sharedPrefsPictures,
						checkIfExists = checkIfExists,
						addError = addError,
						getPics = getPics,
						postState = postState,
						state = state,
						clearErrors = clearErrors,
						navController = navController,
						currentPicture = currentPicture
					)
				}
			}
		}
	)
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun itemNewsCard(
	item: String,
	navController: NavController,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	getPics: () -> Unit,
	currentPicture: (String) -> Unit
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
		},
	)
	if(isClicked)
	{

		isClicked = false
		currentPicture(item)
		val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
		val editor = sharedPreferences.edit()
		editor.putString(PICTURE, item)
		editor.apply()
		navController.navigate(Screen.Details.route)
	}
	when
	{
		openAlertDialog.value ->
		{
			if(isValidUrl(item))
			{
				AlertDialogMain(
					onDismissRequest = { openAlertDialog.value = false },
					onConfirmation =
					{
						openAlertDialog.value = false
						println("Confirmation registered")
						getPics()
						Toast.makeText(context, context.getString(R.string.reload), Toast.LENGTH_LONG).show()
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
	state: MutableState<PictureState>,
	clearErrors: () -> Unit,
	navController: NavController,
	currentPicture: (String) -> Unit
)
{
	val context = LocalContext.current
	Log.d("PicturesScreen", "From cache? ${!imagesUrlsSP.isNullOrEmpty()}")
	Log.d("We got:", "$imagesUrlsSP")
	val scope = rememberCoroutineScope()
	val listState = rememberLazyGridState()
	if(imagesUrlsSP.isNullOrEmpty())
	{
		when(state.value)
		{
			is PictureState.SearchIsOk ->
			{
				Toast.makeText(context, stringResource(R.string.loading_has_been_started), Toast.LENGTH_SHORT).show()
				Log.d("Now state is", "Loading")
				saveToSharedPrefs(context, (state.value as PictureState.SearchIsOk).data)
				val list = (state.value as PictureState.SearchIsOk).data.split("\n")

				LazyVerticalGrid(
					state = listState,
					modifier = Modifier
						.fillMaxSize()
						.padding(0.dp, 55.dp, 0.dp, 0.dp), columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(list) {
						itemNewsCard(
							item = it,
							navController = navController,
							checkIfExists = checkIfExists,
							addError = addError,
							getPics = getPics,
							currentPicture = currentPicture
						)
					}
				}
				scope.launch {
					delay(6000)
					postState((state.value as PictureState.SearchIsOk).data)
				}
			}
			PictureState.ConnectionError ->
			{
				Log.d("Net", "No internet")
				Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
					NoInternetScreen()
					val cornerRadius = 16.dp
					val gradientColor = listOf(Color.Green, Color.Yellow)
					GradientButton(
						gradientColors = gradientColor,
						cornerRadius = cornerRadius,
						nameButton = stringResource(R.string.try_again),
						roundedCornerShape = RoundedCornerShape(topStart = 30.dp, bottomEnd = 30.dp),
						clearErrors,
						getPics
					)
				}
			}
			PictureState.NothingFound -> Unit
			is PictureState.Loaded ->
			{
				Toast.makeText(context, stringResource(R.string.loading_has_been_ended), Toast.LENGTH_SHORT).show()
				Log.d("Now state is", "Loaded")
				val list = (state.value as PictureState.Loaded).data.split("\n")
				LazyVerticalGrid(
					state = listState,
					modifier = Modifier
						.fillMaxSize()
						.padding(0.dp, 55.dp, 0.dp, 0.dp), columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(list) {
						itemNewsCard(
							item = it,
							navController = navController,
							checkIfExists = checkIfExists,
							addError = addError,
							getPics = getPics,
							currentPicture = currentPicture
						)
					}
				}
			}
		}
	}
	else
	{
		Log.d("Now state is", "Loaded from sp")
		saveToSharedPrefs(context, imagesUrlsSP)
		val items = remember { imagesUrlsSP.split("\n") }
		Log.d("item", items.toString())
		LazyVerticalGrid(
			state = listState,
			modifier = Modifier
				.fillMaxSize()
				.padding(0.dp, 55.dp, 0.dp, 0.dp), columns = GridCells.Fixed(count = calculateGridSpan())) {
			Log.d("PicturesFragment", "$items")
			items(items) {
				itemNewsCard(
					item = it,
					navController = navController,
					checkIfExists = checkIfExists,
					addError = addError,
					getPics = getPics,
					currentPicture = currentPicture
				)
			}
		}
	}
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ShowPictures(
	savedPicsUrls: String?,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	getPics: () -> Unit,
	postState: (String) -> Unit,
	state: MutableState<PictureState>,
	clearErrors: () -> Unit,
	navController: NavController,
	currentPicture: (String) -> Unit
)
{
	Scaffold(
		topBar = {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp, 0.dp, 16.dp, 0.dp)
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
	) {
		ShowList(
			imagesUrlsSP = savedPicsUrls,
			checkIfExists = checkIfExists,
			addError = addError,
			getPics = getPics,
			postState = postState,
			state = state,
			clearErrors,
			navController = navController,
			currentPicture = currentPicture
		)
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
			clearErrors.invoke()
			getPics.invoke()
		},
		contentPadding = PaddingValues(),
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

private fun saveToSharedPrefs(context: Context, s: String)
{
	val sharedPreferencesPictures = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
	val editorPictures = sharedPreferencesPictures.edit()
	editorPictures.putString(SHARED_PREFS_PICTURES, s)
	editorPictures.putBoolean(CACHE, false)
	editorPictures.apply()
}

fun isValidUrl(url: String): Boolean
{
	val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
	return urlPattern.matches(url)
}

@Composable
private fun calculateGridSpan(): Int
{
	val context = LocalContext.current
	Log.d("HomeFragment", "Calculate span started")
	val width = Resources.getSystem().displayMetrics.widthPixels
	val orientation = context.resources.configuration.orientation
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