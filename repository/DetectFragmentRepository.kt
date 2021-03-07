package com.luo.face2.repository

import android.graphics.*
import androidx.camera.core.ImageProxy
import com.luo.learnc01.modules.Box
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
@Singleton
class DetectFragmentRepository @Inject constructor() {
    /**
     * 在bitmap上画出人脸框
     */
    fun drawBoxRects(mutableBitmap: Bitmap, box: Box?): Bitmap? {
        if (box == null) {
            return mutableBitmap
        }
        val canvas = Canvas(mutableBitmap)
        val boxPaint = Paint()
        boxPaint.alpha = 200
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 4 * mutableBitmap.width / 800.0f
        boxPaint.textSize = 40 * mutableBitmap.width / 800.0f
        boxPaint.color = Color.RED
        boxPaint.style = Paint.Style.FILL

        boxPaint.style = Paint.Style.STROKE
        val rect = RectF(box.x1.toFloat(), box.y1.toFloat(), box.x2.toFloat(), box.y2.toFloat())
        canvas.drawRect(rect, boxPaint)
        box.landmarks.forEach {
            canvas.drawCircle(it.x, it.y, 5F, boxPaint)
        }

        return mutableBitmap
    }


    fun imageToBitmap(image: ImageProxy): Bitmap {
        val nv21 = imageToNV21(image)
        val yuvImage =
            YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(
                0,
                0,
                yuvImage.width,
                yuvImage.height
            ), 100, out
        )
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun imageToNV21(image: ImageProxy): ByteArray {
        val planes = image.planes
        val y = planes[0]
        val u = planes[1]
        val v = planes[2]
        val yBuffer = y.buffer
        val uBuffer = u.buffer
        val vBuffer = v.buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        // U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        return nv21
    }
}