package com.example.gridpics.ui.pictures

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gridpics.R
import com.example.gridpics.databinding.FragmentImagesBinding
import org.koin.androidx.viewmodel.ext.android.viewModel


class PicturesFragment : Fragment() {

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
            showContent(text)
        } else {
            viewModel.resume()
            viewModel.getPics()
            viewModel.observeState().observe(viewLifecycleOwner) {
                render(it)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onBackPressed()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun render(state: PictureState) {
        Log.d("PicturesFragment", "$state")
        when (state) {
            is PictureState.SearchIsOk -> showContent(state.data)
            is PictureState.NothingFound -> showToast(getString(R.string.nothing_found))
            PictureState.ConnectionError -> showToast(getString(R.string.no_internet))
        }
    }

    private fun showContent(s: String) {
        val list = s.split("\n")
        saveToSharedPrefs(requireContext(), s)

        recyclerView.adapter = PicturesAdapter(list) {
            clickAdapting(it)
        }
        val spanCount = calculateGridSpan()
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
    }

    private fun showToast(s: String) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show()
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

    private fun clickAdapting(item: String) {
        val bundle = Bundle()
        bundle.putString("pic", item)
        val navController = findNavController()
        navController.navigate(R.id.navigation_dashboard, bundle)
    }

    override fun onPause() {
        viewModel.pause()
        super.onPause()
    }

    override fun onResume() {
        Log.d("PicturesFragment", "onResume Call")
        viewModel.resume()
        viewModel.getPics()
        super.onResume()
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

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.pause()
        _binding = null
    }
}