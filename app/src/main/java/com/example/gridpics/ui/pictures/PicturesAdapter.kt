package com.example.gridpics.ui.pictures

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gridpics.R
import java.io.File

class PicturesAdapter(
    private val picsUrls: List<File>,
    private val clickListener: Click
) :
    RecyclerView.Adapter<PicturesViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PicturesViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_picture, parent, false)
        return PicturesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return picsUrls.size
    }

    override fun onBindViewHolder(holder: PicturesViewHolder, position: Int) {
        holder.bind(picsUrls[position])
        holder.itemView.setOnClickListener {
            clickListener.onClick(picsUrls[position].toString())
            notifyDataSetChanged()
        }
    }

    fun interface Click {
        fun onClick(string: String)
    }

}