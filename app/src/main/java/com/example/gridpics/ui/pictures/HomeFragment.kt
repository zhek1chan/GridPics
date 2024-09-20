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
import com.example.gridpics.databinding.FragmentHomeBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    private val viewModel by viewModel<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel.getPics()
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
            is PictureState.NothingFound -> showEmpty()
            PictureState.ConnectionError -> Unit
        }
    }

    private fun showEmpty() {
        //TODO("Not yet implemented")
    }

    private fun showContent(list: List<String>) {
        /*binding.emptyLibrary.visibility = View.GONE
        binding.placeholderMessage.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE*/
        recyclerView.adapter = PicturesAdapter(list) {
            clickAdapting(it)
        }
        val spanCount = calculateGridSpan()
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun calculateGridSpan(): Int {
        val width = Resources.getSystem().displayMetrics.widthPixels
        val height = Resources.getSystem().displayMetrics.heightPixels
        val orientation = this.resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            (width / 100)
        } else {
            (height / 100)
        }
    }

    private fun clickAdapting(item: String) {
        Log.d("PlaylistFragment", "Click on the playlist")
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