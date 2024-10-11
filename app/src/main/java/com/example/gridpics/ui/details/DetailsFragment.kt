package com.example.gridpics.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import coil3.compose.rememberAsyncImagePainter
import com.example.gridpics.ui.themes.ComposeTheme


class DetailsFragment : Fragment() {

    private var interfaceIsVisible = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val img = arguments?.getString("pic")!!
        return ComposeView(requireContext()).apply {
            setContent {
                ShowDetails(img)
            }
        }
    }

    @Composable
    fun ShowDetails(img: String) {
        ComposeTheme {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                CenterAlignedTopAppBarExample(img)
            }
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset(0f, 0f)) }
            Image(
                painter = rememberAsyncImagePainter(img),
                contentDescription = null,
                modifier = Modifier
                    .padding(0.dp,40.dp,0.dp,0.dp)
                    .clickable { clickOnImage() }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Update the scale based on zoom gestures.
                            scale *= zoom

                            // Limit the zoom levels within a certain range (optional).
                            scale = scale.coerceIn(1f, 3f)

                            // Update the offset to implement panning when zoomed.
                            offset = if (scale == 1f) Offset(0f, 0f) else offset + pan
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

    private fun clickOnImage() {
        if (interfaceIsVisible) {
            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            )
            val decorView = requireActivity().window.decorView
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            interfaceIsVisible = false
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            val window = requireActivity().window.decorView
            window.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            interfaceIsVisible = true
        }
    }


    @Composable
    fun CenterAlignedTopAppBarExample(s: String) {
        ComposeTheme {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        s,
                        fontSize = 18.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 0.dp),
                        overflow = TextOverflow.Ellipsis,

                        )
                },
                navigationIcon = {
                    IconButton({ requireActivity().onBackPressed() }) {
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
        }
    }
}