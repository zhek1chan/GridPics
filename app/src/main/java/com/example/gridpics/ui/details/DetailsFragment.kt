package com.example.gridpics.ui.details

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.gridpics.R
import com.example.gridpics.databinding.FragmentDetailsBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso


class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val img = arguments?.getString("pic")!!
        val pic = binding.photoView

        Picasso.get().load(img).networkPolicy(NetworkPolicy.OFFLINE).into(pic, object : Callback {
            override fun onSuccess() {}

            override fun onError(e: Exception?) {
                Picasso.get()
                    .load(Uri.parse(img))
                    .error(R.drawable.ic_error_image)
                    .into(pic, object : Callback {
                        override fun onSuccess() {}

                        override fun onError(e: Exception?) {
                            Log.d("IMAGE EXCEPTION!", img)
                            Log.d("Picasso", "Could not fetch image: $e")
                        }
                    })
            }
        })

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val picHeight = pic.drawable.minimumHeight
        val picWidth = pic.drawable.minimumWidth
        val k: Float
        val dWidth: Int = displayMetrics.widthPixels
        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            k = dWidth.toFloat() / picWidth.toFloat()
            val imgHeight = (k * picHeight).toInt()
            binding.photoView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, imgHeight)
        } else {
            k = dWidth.toFloat() / picHeight.toFloat()
            val imgHeight = (k * picHeight).toInt()
            binding.photoView.layoutParams = LinearLayout.LayoutParams(imgHeight, MATCH_PARENT)
        }

        binding.backIcon.setOnClickListener {
            navigateBack()
        }
        binding.url.text = img
        binding.photoView.setOnScaleChangeListener { scaleFactor, _, _ ->
            pic.scale
            Log.d("DetailsFragment", "${pic.scale}")
            if (scaleFactor != 0f) {
                binding.backIcon.visibility = View.INVISIBLE
                binding.url.visibility = View.INVISIBLE
            } else {
                binding.backIcon.visibility = View.VISIBLE
                binding.url.visibility = View.VISIBLE
            }
        }

        binding.layout.setOnClickListener {
            binding.backIcon.visibility = View.VISIBLE
            binding.url.visibility = View.VISIBLE
        }

        binding.space.setOnClickListener {
            binding.backIcon.visibility = View.VISIBLE
            binding.url.visibility = View.VISIBLE
        }
        return root
    }

    private fun navigateBack() {
        requireActivity().onBackPressed()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}