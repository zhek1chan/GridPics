package com.example.gridpics.ui.details

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
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
                            pic.setImageDrawable(resources.getDrawable(R.drawable.ic_error_image))
                        }
                    })
            }
        })

        pic.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)


        binding.backIcon.setOnClickListener {
            navigateBack()
        }
        binding.url.text = img
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var isVisible = true
        val pic = binding.photoView
        binding.photoView.setOnClickListener {
            if (isVisible) {
                requireActivity().window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                )
                requireActivity().window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                binding.backIcon.visibility = View.GONE
                binding.url.visibility = View.GONE
                //val param = pic.layoutParams as ViewGroup.MarginLayoutParams
                //param.setMargins(0, 70, 0, 0)
                //pic.layoutParams = param
                pic.setPadding(0, 70, 0, 0)
                Log.d("PicZOOM", "${pic.isZoomed}")
                requireActivity().window.navigationBarColor = Color.TRANSPARENT

                isVisible = false
            } else {
                isVisible = true
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                binding.backIcon.visibility = View.VISIBLE
                binding.url.visibility = View.VISIBLE
                //val param = pic.layoutParams as ViewGroup.MarginLayoutParams
                //param.setMargins(0, 0, 0, 0)
                //pic.layoutParams = param
                pic.setPadding(0, 0, 0, 0)
                requireActivity().window.navigationBarColor = Color.BLACK
                requireActivity().window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        }



        binding.layout.setOnClickListener {
            binding.backIcon.visibility = View.VISIBLE
            binding.url.visibility = View.VISIBLE
        }
    }

    private fun navigateBack() {
        requireActivity().onBackPressed()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}