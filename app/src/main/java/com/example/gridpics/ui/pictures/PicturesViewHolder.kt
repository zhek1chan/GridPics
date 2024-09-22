package com.example.gridpics.ui.pictures

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.gridpics.R
import java.io.File

class PicturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val pic: ImageView = itemView.findViewById(R.id.items_image)

    fun bind(s: File) {
        if (s.toString().isEmpty()) {
            pic.setImageDrawable(itemView.resources.getDrawable(R.drawable.ic_error_image))
        } else {
            val cornerPixelSize =
                itemView.resources.getDimensionPixelSize(R.dimen.item_corner_radius)
            Glide.with(itemView)
                .asBitmap()
                .load(s)
                .placeholder(itemView.resources.getDrawable(R.drawable.ic_error_image))
                .transform(CenterCrop(), RoundedCorners(cornerPixelSize), CenterCrop())
                .into(pic)
        }
    }
}