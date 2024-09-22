package com.example.gridpics.ui.dashboard

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.gridpics.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val img = arguments?.getString("pic")!!
        Log.d("PictureFragment","Image loaded")
        binding.photoView.setImageURI(img.toUri())
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}