package com.example.gridpics.ui.pictures

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gridpics.R
import com.example.gridpics.databinding.FragmentImagesBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File


class PictureFragment : Fragment() {

    private var _binding: FragmentImagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    private val viewModel by viewModel<PicturesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagesBinding.inflate(inflater, container, false)
        viewModel.readFiles(requireContext())
        viewModel.observeState().observe(viewLifecycleOwner) {
            render(it)
        }
        recyclerView = binding.rvItems
        return binding.root
    }

    private fun render(state: PictureState) {
        Log.d("HomeFragment", "$state")
        when (state) {
            is PictureState.SearchIsOk -> showContent(state.data)
            is PictureState.NothingFound -> Unit
            PictureState.ConnectionError -> Unit
        }
    }

    private fun showContent(list: List<File>) {
        recyclerView.adapter = PicturesAdapter(list) {
            clickAdapting(it)
        }
        val spanCount = calculateGridSpan()
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        recyclerView.adapter?.notifyDataSetChanged()
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

    private fun clickAdapting(item: String) {
        val bundle = Bundle()
        bundle.putString("pic", item)
        val navController = findNavController()
        navController.navigate(R.id.navigation_dashboard, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}