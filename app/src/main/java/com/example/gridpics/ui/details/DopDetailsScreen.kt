package com.example.gridpics.ui.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.error
import com.example.gridpics.R

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun SharedTransitionScope.DopDetailsScreen(url: String, isPortrait: Boolean , animatedVisibilityScope: AnimatedVisibilityScope)
{
	val scale = if (isPortrait) {
		ContentScale.FillWidth
	} else {
		ContentScale.FillHeight
	}
	val paddings  = WindowInsets.systemBarsIgnoringVisibility.asPaddingValues()
	Box(modifier = Modifier.fillMaxSize()) {
		val headers = remember {
			NetworkHeaders.Builder()
				.set("Cache-Control", "max-age=604800, must-revalidate, stale-while-revalidate=86400")
				.build()
		}
		val context = LocalContext.current
		val imgRequest = remember(url) {
			ImageRequest.Builder(context)
				.data(url)
				.httpHeaders(headers)
				.memoryCachePolicy(CachePolicy.WRITE_ONLY)
				.error(R.drawable.error)
				.build()
		}
		SubcomposeAsyncImage(
			model = (imgRequest),
			contentDescription = url,
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.Center)
				.sharedElement(
					rememberSharedContentState(key = url),
					animatedVisibilityScope,
				),
			contentScale = scale,
			loading = {
				Box(Modifier.fillMaxSize()) {
					CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
				}
			}
		)
	}
}