package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.PIC
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.placeholder.NoInternetScreen
import com.example.gridpics.ui.themes.ComposeTheme
import org.koin.androidx.compose.getViewModel

@Composable
fun PicturesScreen(navController: NavController)
{
	val txt = LocalContext.current.getSharedPreferences("shared_prefs", MODE_PRIVATE).getString(PICTURES, null)
	val viewModel = getViewModel<PicturesViewModel>()
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

@SuppressLint("UnrememberedMutableState")
@Composable
fun ItemNewsCard(item: String, nc: NavController)
{
	ComposeTheme {
		var isClicked by remember { mutableStateOf(false) }
		Log.d("PicturesFragment", "Pic url - $item")
		AsyncImage(
			model = item,
			contentDescription = null,
			modifier = Modifier
				.clickable {
					isClicked = true
				}
				.padding(10.dp)
				.size(100.dp)
				.clip(RoundedCornerShape(8.dp)),
			contentScale = ContentScale.Crop,
			error = painterResource(R.drawable.ic_error_image),
			onError = { Log.d("Error", "ERROR LOADING $item") }
		)
		if(isClicked)
		{
			isClicked = false
			val sharedPreferences = LocalContext.current.getSharedPreferences(PIC, MODE_PRIVATE)
			val editor = sharedPreferences.edit()
			editor.putString(PIC, item)
			editor.apply()
			nc.navigate("details_screen")
		}
	}
}

@Composable
fun ShowList(s: String?, vm: PicturesViewModel, nv: NavController)
{
	Log.d("PicturesFragment", "From cache? ${!s.isNullOrEmpty()}")
	if(s == null)
	{
		val value by vm.observeState().observeAsState()
		when(value)
		{
			is PictureState.SearchIsOk ->
			{
				saveToSharedPrefs(
					LocalContext.current,
					(value as PictureState.SearchIsOk).data
				)
				val list = (value as PictureState.SearchIsOk).data.split("\n")

				LazyVerticalGrid(
					modifier = Modifier
						.fillMaxSize()
						.padding(0.dp, 45.dp, 0.dp, 50.dp),
					columns = GridCells.Fixed(count = calculateGridSpan())
				) {
					Log.d("PicturesFragment", "$list")
					items(list) {
						ItemNewsCard(it, nv)
					}
				}
			}
			PictureState.ConnectionError ->
			{
				Log.d("Net", "No internet")
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center
				) {
					NoInternetScreen()
					val cornerRadius = 16.dp
					val gradientColor = listOf(Color.Green, Color.Yellow)
					GradientButton(
						gradientColors = gradientColor,
						cornerRadius = cornerRadius,
						nameButton = stringResource(R.string.try_again),
						roundedCornerShape = RoundedCornerShape(
							topStart = 30.dp,
							bottomEnd = 30.dp
						),
						vm
					)
				}
			}
			PictureState.NothingFound -> Unit
			null -> Unit
		}
	}
	else
	{
		val items = s.split("\n")
		LazyVerticalGrid(
			modifier = Modifier
				.fillMaxSize()
				.padding(0.dp, 45.dp, 0.dp, 50.dp),
			columns = GridCells.Fixed(count = calculateGridSpan())
		) {
			Log.d("PicturesFragment", "$items")
			items(items) {
				ItemNewsCard(it, nv)
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
		Scaffold(
			topBar = {
				TopAppBar(
					modifier = Modifier
						.height(35.dp)
						.padding(0.dp, 10.dp, 0.dp, 0.dp),
					colors = TopAppBarDefaults.topAppBarColors(
						titleContentColor = MaterialTheme.colorScheme.onPrimary
					),
					title = {
						Text("GridPics")
					}
				)
			},
		) {
			ShowList(s, vm, nv)
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
	Button(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 32.dp, end = 32.dp),
		onClick = {
			vm.getPics()
		},
		contentPadding = PaddingValues(),
		colors = ButtonDefaults.buttonColors(
			containerColor = Color.Transparent
		),
		shape = RoundedCornerShape(cornerRadius)
	) {
		Box(
			modifier = Modifier
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
				.padding(horizontal = 16.dp, vertical = 8.dp),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = nameButton,
				fontSize = 20.sp,
				color = Color.White
			)
		}
	}
}

private fun saveToSharedPrefs(context: Context, s: String)
{
	val sharedPreferences = context.getSharedPreferences(PICTURES, MODE_PRIVATE)
	val editor = sharedPreferences.edit()
	editor.putString(PICTURES, s)
	editor.apply()
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
