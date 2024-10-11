package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil3.compose.AsyncImage
import com.example.gridpics.R
import com.example.gridpics.ui.placeholder.NoInternetScreen
import com.example.gridpics.ui.themes.ComposeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel


class PicturesFragment : Fragment() {
    private val viewModel by viewModel<PicturesViewModel>()
    private val sharedPrefs: String = "sharedPrefs"
    private val key: String = "list_of_pics"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        onBackPressed()
        val sharedPreferences = requireContext().getSharedPreferences(sharedPrefs, MODE_PRIVATE)
        val text = sharedPreferences.getString(key, "")
        Log.d("PicturesFragment", "Shared prefs - $text")
        if (!text.isNullOrEmpty()) {
            return ComposeView(requireContext()).apply {
                setContent {
                    ShowPictures(text)
                }
            }
        } else {
            viewModel.resume()
            viewModel.getPics()
            return ComposeView(requireContext()).apply {
                setContent {
                    ShowPictures(null)
                }
            }
        }

    }


    @Composable
    fun ItemNewsCard(item: String) {
        ComposeTheme {
            Log.d("PicturesFragment", "Pic url - $item")
            AsyncImage(
                model = item,
                contentDescription = null,
                modifier = Modifier
                    .clickable { clickAdapting(item) }
                    .padding(10.dp)
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_error_image),
                onError = { Log.d("Error", "ERROR LOADING $item") }
            )
        }
    }

    private fun clickAdapting(item: String) {
        val bundle = Bundle()
        bundle.putString("pic", item)
        val navController = findNavController()
        navController.navigate(R.id.navigation_dashboard, bundle)
    }

    @Composable
    fun ShowList(s: String?) {
        Log.d("PicturesFragment", "From cache? ${!s.isNullOrEmpty()}")
        if (s == null) {
            val value by viewModel.observeState().observeAsState()
            when (value) {
                is PictureState.SearchIsOk -> {
                    saveToSharedPrefs(
                        requireContext(),
                        (value as PictureState.SearchIsOk).data
                    )
                    val list = (value as PictureState.SearchIsOk).data.split("\n")

                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp, 45.dp, 0.dp, 50.dp),
                        columns = GridCells.Fixed(count = calculateGridSpan())

                    ) {
                        Log.d("PicturesFragmnet", "$list")
                        items(list) {
                            ItemNewsCard(it)
                        }
                    }
                }

                PictureState.ConnectionError -> {
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
                        )
                    }
                }

                PictureState.NothingFound -> Unit
                null -> Unit
            }
        } else {
            val items = s.split("\n")
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp, 45.dp, 0.dp, 50.dp),
                columns = GridCells.Fixed(count = calculateGridSpan())
            ) {
                Log.d("PicturesFragment", "$items")
                items(items) {
                    ItemNewsCard(it)
                }
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShowPictures(s: String?) {
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
                ShowList(s)
            }
        }
    }

    @Composable
    fun GradientButton(
        gradientColors: List<Color>,
        cornerRadius: Dp,
        nameButton: String,
        roundedCornerShape: RoundedCornerShape,
    ) {

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp),
            onClick = {
                viewModel.getPics()
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


    private fun saveToSharedPrefs(context: Context, s: String) {
        val sharedPreferences = context.getSharedPreferences(sharedPrefs, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, s)
        editor.apply()
    }

    private fun calculateGridSpan(): Int {
        Log.d("HomeFragment", "Calculate span started")
        val width = Resources.getSystem().displayMetrics.widthPixels
        val orientation = this.resources.configuration.orientation
        val density = requireContext().resources.displayMetrics.density
        return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ((width / density).toInt() / 110)
        } else {
            ((width / density).toInt() / 110)
        }
    }

    private fun onBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    fragmentManager?.popBackStack()
                    if (isEnabled) {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            })
    }

}