package com.example.gridpics.ui.custom

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.gridpics.R
import com.example.gridpics.dp2px
import com.google.android.material.imageview.ShapeableImageView

class ImageView: ShapeableImageView
{
	constructor(context: Context): super(context)
	constructor(context: Context, attrs: AttributeSet?): super(context, attrs)
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(
		context,
		attrs,
		defStyleAttr
	)

	fun setImage(imageUrl: String?)
	{
		if(!TextUtils.isEmpty(imageUrl))
		{
			this.load(imageUrl) {
				crossfade(true)
				placeholder(R.drawable.ic_error_image)
				transformations(
					RoundedCornersTransformation(
						4.dp2px.toFloat(),
						4.dp2px.toFloat(),
						0f,
						0f
					)
				)
			}
		}
	}
}