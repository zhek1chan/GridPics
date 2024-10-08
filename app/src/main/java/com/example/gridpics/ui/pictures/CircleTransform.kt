package com.example.gridpics.ui.pictures

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toAndroidTileMode
import com.squareup.picasso.Transformation
import kotlin.math.min

class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val size =
            min(source.width.toDouble(), source.height.toDouble()).toInt()

        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) {
            source.recycle()
        }

        val bitmap = Bitmap.createBitmap(size, size, source.config)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader =
            BitmapShader(squaredBitmap, TileMode.Clamp.toAndroidTileMode(), TileMode.Clamp.toAndroidTileMode())
        paint.setShader(shader)
        paint.isAntiAlias = true

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        squaredBitmap.recycle()
        return bitmap
    }

    override fun key(): String {
        return "circle"
    }
}