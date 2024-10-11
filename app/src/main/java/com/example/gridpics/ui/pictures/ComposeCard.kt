package com.example.gridpics.ui.pictures

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil3.compose.AsyncImage
import com.example.gridpics.R
import com.example.gridpics.databinding.FragmentImagesBinding
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {
    private var _binding: FragmentImagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    private val viewModel by viewModel<PicturesViewModel>()
    private val sharedPrefs: String = "sharedPrefs"
    private val key: String = "list_of_pics"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagesBinding.inflate(inflater, container, false)
        recyclerView = binding.rvItems
        val sharedPreferences = requireContext().getSharedPreferences(sharedPrefs, MODE_PRIVATE)
        val text = sharedPreferences.getString(key, "")
        if (!text.isNullOrEmpty()) {
            //showContent(text)
        } else {
            viewModel.resume()
            viewModel.getPics()
            viewModel.observeState().observe(viewLifecycleOwner) {
                //render(it)
            }
        }
        return ComposeView(requireContext()).apply {
            setContent {
                ItemNewsCard("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQKMBWXDkh39EwFfxTgsvf-f-IuC_cMHDX1Sg")
            }
        }
    }


    @Composable
    fun ItemNewsCard(item: String) {
        //ComposeTheme {
        val imgLoader = ImageLoader.Builder(requireContext())
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = { clickAdapting(item) }),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            val listener = object : ImageRequest.Listener {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    super.onError(request, result)
                }

                override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                    super.onSuccess(request, result)
                }
            }
            val placeholder = resources.getDrawable(R.drawable.ic_error_image)
            val imageRequest = ImageRequest.Builder(requireContext())
                .data(item)
                .listener(listener)
                .dispatcher(Dispatchers.IO)
                .memoryCacheKey(item)
                .diskCacheKey(item)
                .placeholder(placeholder)
                .error(placeholder)
                .fallback(placeholder)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            AsyncImage(
                model = imageRequest,
                contentDescription = "Image Description",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }

    private fun clickAdapting(item: String) {
        val bundle = Bundle()
        bundle.putString("pic", item)
        val navController = findNavController()
        navController.navigate(R.id.navigation_dashboard, bundle)
    }
}