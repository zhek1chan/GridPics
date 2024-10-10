package com.example.gridpics.ui.details

import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().window.setFlags(SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN, 0)

    }

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


        val rectangle = Rect()
        val window: Window = requireActivity().window
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight: Int = rectangle.top
        val contentViewTop =
            window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
        val titleBarHeight = contentViewTop - statusBarHeight
        Log.d("height of status bar", "$titleBarHeight")
        val navBarHeight = getNavigationBarHeight()
        Log.d("height of nav bar", "$navBarHeight")
        binding.photoView.setOnClickListener {
            if (isVisible) {
                requireActivity().window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                )
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                binding.backIcon.visibility = View.GONE
                binding.url.visibility = View.GONE
                pic.setPadding(0, 0, 0, -titleBarHeight)
                binding.layout.setPadding(0, 0, 0, 0)
                Log.d("TEST1", "1 Is Visible make false, zoomed - ${pic.isZoomed}")
                if ((!pic.isZoomed)) {
                    pic.setPadding(0, 0, 0, -titleBarHeight)
                    binding.layout.setPadding(0, 0, 0, 0)
                    Log.d("TEST1", "1 Is Visible make false, zoomed - ${pic.isZoomed}")
                } else {
                    Log.d("TEST2", "2 Is Visible make false, zoomed - ${pic.isZoomed}")
                    binding.layout.setPadding(0, 0, 0, 0)
                    pic.setPadding(0, 0, 0, -titleBarHeight)
                    pic.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
                requireActivity().window.navigationBarColor = Color.TRANSPARENT
                isVisible = false
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (!pic.isZoomed) {
                    pic.setPadding(0, 0, 0, 0)
                    binding.layout.setPadding(0, 0, 0, 0)
                    pic.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    Log.d("Test3", "Showing Interface, zoomed - ${pic.isZoomed}")
                } else {
                    Log.d("Test4", "Showing Interface, zoomed - ${pic.isZoomed}")
                    pic.setPadding(0, 0, 0, 0)
                    binding.layout.setPadding(0, 0, 0, 0)
                    pic.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
                binding.backIcon.visibility = View.VISIBLE
                binding.url.visibility = View.VISIBLE
                window.navigationBarColor = Color.BLACK
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                isVisible = true
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


    fun getNavigationBarHeight(): Int {
        val display: Display = requireActivity().windowManager.defaultDisplay
        val point = Point()
        display.getRealSize(point)
        val realHeight = point.y

        // Используем Reflection для доступа к скрытому полю
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId)
        }

        // Если не удалось получить высоту через ресурсы, используем разницу между реальной и видимой высотой
        return realHeight - display.height
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}