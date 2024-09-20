package com.example.gridpics.ui.pictures

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.gridpics.R

class PicturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val pic: ImageView = itemView.findViewById(R.id.items_image)

    fun bind(s: String) {
        if (Uri.parse(s).toString().isEmpty()) {
            pic.setImageDrawable(itemView.resources.getDrawable(R.drawable.ic_error_image))
        } else {
            val cornerPixelSize =
                itemView.resources.getDimensionPixelSize(R.dimen.item_corner_radius)
            Glide.with(itemView)
                .load(Uri.parse(s))
                .transform(CenterCrop(), RoundedCorners(cornerPixelSize), CenterCrop())
                .into(pic)
        }
    }
}