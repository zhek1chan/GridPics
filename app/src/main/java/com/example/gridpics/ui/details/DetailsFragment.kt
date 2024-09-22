package com.example.gridpics.ui.details

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gridpics.R
import com.example.gridpics.databinding.FragmentDetailsBinding

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
        Log.d("DetailsFragment","$img")
        binding.photoView.setImageURI(Uri.parse(img))
        binding.backIcon.setOnClickListener {
            navigateBack()
        }
        return root
    }

    private fun navigateBack() {
        val navController = findNavController()
        navController.navigate(R.id.navigation_home)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}