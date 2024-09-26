package com.example.gridpics.ui.pictures

import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.gridpics.R
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso


class PicturesViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val pic: ImageView = itemView.findViewById(R.id.items_image)

    fun bind(s: String) {
        Picasso.get().load(s).networkPolicy(NetworkPolicy.OFFLINE).into(pic, object : Callback {
            override fun onSuccess() {}

            override fun onError(e: Exception?) {
                Picasso.get()
                    .load(Uri.parse(s))
                    .resize(100, 100)
                    .centerCrop()
                    .error(R.drawable.ic_error_image)
                    .into(pic, object : Callback {
                        override fun onSuccess() {}

                        override fun onError(e: Exception?) {
                                Log.d("IMAGE EXCEPTION!", s)
                                Log.d("Picasso", "Could not fetch image: $e")
                            }
                    })
            }
        })
    }
}
