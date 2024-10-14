package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.example.gridpics.ui.activity.MainActivity.Companion.PIC
import com.example.gridpics.ui.themes.ComposeTheme
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.androidx.compose.getViewModel

@Composable
fun DetailsScreen(nc: NavController)
{
	val viewModel = getViewModel<DetailsViewModel>()
	val pic = LocalContext.current.getSharedPreferences(PIC, MODE_PRIVATE).getString(PIC, "null")
	if(pic != null)
	{
		ShowDetails(pic, viewModel, nc)
	}
	Log.d("pic", "We are on DetailsScreen $pic")
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun ShowDetails(img: String, vm: DetailsViewModel, nc: NavController)
{
	val isVisible = remember { mutableStateOf(true) }
	val dynamicPadding = remember { mutableStateOf(70.dp) }
	ComposeTheme {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(40.dp),
			contentAlignment = Alignment.TopCenter
		) {
			AnimatedVisibility(visible = isVisible.value) {
				var navBack by remember { mutableStateOf(false) }
				@OptIn(ExperimentalMaterial3Api::class)
				TopAppBar(
					title = {
						Text(
							img,
							fontSize = 18.sp,
							maxLines = 2,
							modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 0.dp),
							overflow = TextOverflow.Ellipsis,
						)
					},
					navigationIcon = {
						IconButton({ navBack = true }) {
							Icon(
								Icons.Filled.ArrowBack,
								contentDescription = "back",
								modifier = Modifier.padding(0.dp, 10.dp, 0.dp, 0.dp)
							)
						}
					},
					colors = TopAppBarDefaults.topAppBarColors(
						titleContentColor = MaterialTheme.colorScheme.onPrimary,
						navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
						actionIconContentColor = MaterialTheme.colorScheme.onPrimary
					)
				)
				if(navBack)
				{
					navBack = false
					nc.navigateUp()
				}
			}
		}
		AnimatedVisibility(visible = !isVisible.value) {
			Spacer(modifier = Modifier
				.fillMaxWidth()
				.height(40.dp))
		}
		var scale by remember { mutableStateOf(1f) }
		var offset by remember { mutableStateOf(Offset(0f, 0f)) }
		var isClicked by mutableStateOf(false)
		Image(
			painter = rememberAsyncImagePainter(img),
			contentDescription = null,
			modifier = Modifier
				.padding(0.dp, dynamicPadding.value, 0.dp, 0.dp)
				.clickable {
					isClicked = true
					isVisible.value = !isVisible.value
					if(isVisible.value == true)
					{
						dynamicPadding.value = 70.dp
					}
					else dynamicPadding.value = 70.dp
				}
				.pointerInput(Unit) {
					detectTransformGestures { _, pan, zoom, _ ->
						// Update the scale based on zoom gestures.
						scale *= zoom
						// Limit the zoom levels within a certain range (optional).
						scale = scale.coerceIn(1f, 3f)
						// Update the offset to implement panning when zoomed.
						offset = if(scale == 1f) Offset(0f, 0f) else offset + pan
					}
				}
				.graphicsLayer(
					scaleX = scale, scaleY = scale,
					translationX = offset.x, translationY = offset.y
				)
				.fillMaxSize()
		)
		if(isClicked)
		{
			isClicked = false
			ClickOnImage(vm)
		}
	}
}

@Composable
private fun ClickOnImage(viewModel: DetailsViewModel)
{
	val interfaceIsVisible = viewModel.observeState().value
	val activity = getActivity()
	val systemUiController: SystemUiController = rememberSystemUiController()
	if(interfaceIsVisible == true)
	{
		activity.window.decorView.apply {
			systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
		}
		systemUiController.isStatusBarVisible = false // Status bar
		systemUiController.isNavigationBarVisible = false // Navigation bar
		systemUiController.isSystemBarsVisible = false // Status & Navigation bars
		viewModel.setFalseState()
	}
	else
	{
		systemUiController.isStatusBarVisible = true // Status bar
		systemUiController.isNavigationBarVisible = true // Navigation bar
		systemUiController.isSystemBarsVisible = true // Status & Navigation bars
		viewModel.setTrueState()
	}
}

@Composable
fun getActivity(): Activity {
	var context = LocalContext.current
	while (context is ContextWrapper) {
		if (context is Activity) return context
		context = context.baseContext
	}
	throw IllegalStateException("no activity")
}