package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.PIC
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.placeholder.NoInternetScreen
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun PicturesScreen(navController: NavController)
{
	val viewModel = koinViewModel<PicturesViewModel>()
	val txt = LocalContext.current.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, "")
	if(!txt.isNullOrEmpty())
	{
		ShowPictures(txt, viewModel, navController)
	}
	else
	{
		viewModel.resume()
		viewModel.getPics()
		ShowPictures(null, viewModel, navController)
	}
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ItemNewsCard(item: String, nc: NavController, vm: PicturesViewModel, fromCache: Boolean)
{
	ComposeTheme {
		val context = LocalContext.current
		var isClicked by remember { mutableStateOf(false) }
		var isError by remember { mutableStateOf(false) }
		val openAlertDialog = remember { mutableStateOf(false) }
		val errorMessage = remember { mutableStateOf("") }
		SingletonImageLoader.setSafe {
			ImageLoader.Builder(context)
				.diskCachePolicy(CachePolicy.ENABLED)
				.diskCache {
					DiskCache.Builder()
						.directory(context.cacheDir.resolve("image_cache"))
						.build()
				}
				.build()
		}
		var pl = R.drawable.loading
		if(fromCache)
		{
			pl = R.drawable.error
		}
		val imgRequest = ImageRequest.Builder(LocalContext.current)
			.data(item)
			.diskCachePolicy(CachePolicy.ENABLED)
			.placeholder(pl)
			.error(R.drawable.error)
			.build()
		SubcomposeAsyncImage(
			model = imgRequest,
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
			},
			onSuccess = {
				isError = false
			},
		)
		if(isClicked)
		{
			isClicked = false
			val sharedPreferences = context.getSharedPreferences(PIC, MODE_PRIVATE)
			val editor = sharedPreferences.edit()
			editor.putString(PIC, item)
			editor.apply()
			nc.navigate("details_screen")
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
							vm.getPics()
							Toast.makeText(context, context.getString(R.string.reload), Toast.LENGTH_LONG).show()
						},
						dialogTitle = stringResource(R.string.error_ocurred_loading_img),
						dialogText = "Ошибка: " + errorMessage.value + "\nПопробовать загрузить повторно?",
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
							println("Confirmation registered")
						},
						dialogTitle = stringResource(R.string.error_ocurred_loading_img), dialogText = stringResource(R.string.link_is_not_valid), icon = Icons.Default.Warning)
				}
			}
		}
	}
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ShowList(s: String?, vm: PicturesViewModel, nv: NavController)
{
	Log.d("PicturesScreen", "From cache? ${!s.isNullOrEmpty()}")
	Log.d("We got:", "$s")
	val scope = rememberCoroutineScope()
	if(s == "null" || s.isNullOrEmpty())
	{
		val value by vm.observeState().observeAsState()
		when(value)
		{
			is PictureState.SearchIsOk ->
			{
				Toast.makeText(LocalContext.current, "Началась загрузка", Toast.LENGTH_LONG).show()
				Log.d("Now state is", "Loading")
				saveToSharedPrefs(LocalContext.current, (value as PictureState.SearchIsOk).data)
				val list = (value as PictureState.SearchIsOk).data.split("\n")

				LazyVerticalGrid(modifier = Modifier
					.fillMaxSize()
					.padding(0.dp, 45.dp, 0.dp, 0.dp), columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(list) {
						ItemNewsCard(it, nv, vm, false)
					}
				}
				scope.launch {
					delay(10000)
					vm.postState((value as PictureState.SearchIsOk).data)
				}
			}
			PictureState.ConnectionError ->
			{
				Log.d("Net", "No internet")
				Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
					NoInternetScreen()
					val cornerRadius = 16.dp
					val gradientColor = listOf(Color.Green, Color.Yellow)
					GradientButton(gradientColors = gradientColor, cornerRadius = cornerRadius, nameButton = stringResource(R.string.try_again), roundedCornerShape = RoundedCornerShape(topStart = 30.dp, bottomEnd = 30.dp), vm)
				}
			}
			PictureState.NothingFound -> Unit
			null -> Unit
			is PictureState.Loaded ->
			{
				Toast.makeText(LocalContext.current, "Загрузка завершена", Toast.LENGTH_SHORT).show()
				Log.d("Now state is", "Loaded")
				val list = (value as PictureState.Loaded).data.split("\n")
				LazyVerticalGrid(modifier = Modifier
					.fillMaxSize()
					.padding(0.dp, 45.dp, 0.dp, 0.dp), columns = GridCells.Fixed(count = calculateGridSpan())) {
					items(list) {
						ItemNewsCard(it, nv, vm, true)
					}
				}
			}
		}
	}
	else
	{
		Log.d("Now state is", "Loaded from sp")
		saveToSharedPrefs(LocalContext.current, s)
		val items = remember { s.split("\n") }
		Log.d("item", items.toString())
		LazyVerticalGrid(
			modifier = Modifier
				.fillMaxSize()
				.padding(0.dp, 45.dp, 0.dp, 0.dp), columns = GridCells.Fixed(count = calculateGridSpan())) {
			Log.d("PicturesFragment", "$items")
			items(items) {
				ItemNewsCard(it, nv, vm, true)
			}
		}
	}
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowPictures(s: String?, vm: PicturesViewModel, nv: NavController)
{
	ComposeTheme {
		var openAddDialog by remember { mutableStateOf(false) }
		val editMessage = remember { mutableStateOf("") }
		var string = s.toString()

		Scaffold(
			topBar = {
				TopAppBar(modifier = Modifier
					.height(35.dp)
					.padding(0.dp, 10.dp, 0.dp, 0.dp), colors = TopAppBarDefaults.topAppBarColors(titleContentColor = MaterialTheme.colorScheme.onPrimary), title = {
					Row {
						Text(stringResource(R.string.gridpics))
						/*IconButton(
							onClick = {
								openAddDialog = false //TODO
							},
							modifier = Modifier
								.align(Alignment.CenterVertically)
						) {
							Icon(painter = rememberVectorPainter(Icons.Default.Add), contentDescription = "share", tint = MaterialTheme.colorScheme.onPrimary)
						}*/
					}
				})
				if(openAddDialog)
				{
					Row(verticalAlignment = Alignment.CenterVertically) {
						AddDialog(
							editMessage = editMessage,
							onSubmit =
							{
								string += "\n${editMessage.value}"
								openAddDialog = false
								Log.d("WHAT I WROTE:", string)
							},
							onDismiss = { openAddDialog = false }
						)
					}
				}
			},
		) {
			ShowList(string, vm, nv)
		}
	}
}

@Composable
fun GradientButton(
	gradientColors: List<Color>,
	cornerRadius: Dp,
	nameButton: String,
	roundedCornerShape: RoundedCornerShape,
	vm: PicturesViewModel,
)
{
	Button(modifier = Modifier
		.fillMaxWidth()
		.padding(start = 32.dp, end = 32.dp), onClick = {
		vm.getPics()
	}, contentPadding = PaddingValues(), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(cornerRadius)) {
		Box(modifier = Modifier
			.fillMaxWidth()
			.background(brush = Brush.horizontalGradient(colors = gradientColors), shape = roundedCornerShape)
			.clip(roundedCornerShape)
			.background(brush = Brush.linearGradient(colors = gradientColors), shape = RoundedCornerShape(cornerRadius))
			.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
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

@Composable
fun AddDialog(
	editMessage: MutableState<String>,
	onSubmit: () -> Unit,
	onDismiss: () -> Unit,
)
{
	Column(
		modifier = Modifier
			.clip(RoundedCornerShape(4.dp))
			.background(MaterialTheme.colorScheme.background)
			.padding(8.dp),
	) {
		Column(
			modifier = Modifier.padding(16.dp),
		) {
			Text(text = stringResource(R.string.extract_link))

			Spacer(modifier = Modifier.height(8.dp))

			TextField(
				value = editMessage.value,
				onValueChange = { editMessage.value = it },
				singleLine = true
			)
		}

		Spacer(modifier = Modifier.height(8.dp))

		Row(
			modifier = Modifier.align(Alignment.End)
		) {
			Button(
				onClick = onDismiss
			) {
				Text(stringResource(R.string.cancel))
			}

			Spacer(modifier = Modifier.width(8.dp))

			Button(
				onClick = onSubmit
			) {
				Text(stringResource(R.string.add))
			}
		}
	}
}

private fun saveToSharedPrefs(context: Context, s: String)
{
	Log.d("PicturesScreen", "Saved to SP $s")
	val sharedPreferences = context.getSharedPreferences(PICTURES, MODE_PRIVATE)
	val editor = sharedPreferences.edit()
	editor.putString(PICTURES, s)
	editor.apply()
}

fun isValidUrl(url: String): Boolean
{
	val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
	return urlPattern.matches(url)
}

@Composable
private fun calculateGridSpan(): Int
{
	Log.d("HomeFragment", "Calculate span started")
	val width = Resources.getSystem().displayMetrics.widthPixels
	val orientation = LocalContext.current.resources.configuration.orientation
	val density = LocalContext.current.resources.displayMetrics.density
	return if(orientation == Configuration.ORIENTATION_PORTRAIT)
	{
		((width / density).toInt() / 110)
	}
	else
	{
		((width / density).toInt() / 110)
	}
}