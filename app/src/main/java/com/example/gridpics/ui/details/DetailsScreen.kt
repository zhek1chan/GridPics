package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun DetailsScreen(nc: NavController, viewModel: DetailsViewModel)
{
	BackHandler {
		if(viewModel.observeState().value == true)
		{
			viewModel.changeState()
			nc.navigateUp()
		}
		else
		{
			nc.navigateUp()
		}
	}
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
		var scale by remember { mutableFloatStateOf(1f) }
		var offset by remember { mutableStateOf(Offset(0f, 0f)) }
		Image(
			painter = rememberAsyncImagePainter(img),
			contentDescription = null,
			modifier = Modifier
				.padding(0.dp, 40.dp, 0.dp, 0.dp)
				.clickable {
					isVisible.value = !isVisible.value
					vm.changeState()
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
	}
}