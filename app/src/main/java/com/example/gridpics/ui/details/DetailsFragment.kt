package com.example.gridpics.ui.details

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Layout
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
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
        val displayMetrics = DisplayMetrics()
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
        val dWidth = displayMetrics.widthPixels


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

        val picHeight = pic.drawable.minimumHeight
        val picWidth = pic.drawable.minimumWidth
        val k = dWidth.toFloat() / picWidth.toFloat()
        val imgHeight = (k * picHeight).toInt()
        Log.d("WTF", "$imgHeight")
        binding.photoView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, imgHeight)
        binding.backIcon.setOnClickListener {
            navigateBack()
        }
        binding.url.text = img
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