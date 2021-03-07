package com.luo.face2.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import com.luo.learnc01.modules.Box
import com.zyao89.view.zloading.ZLoadingDialog
import com.zyao89.view.zloading.Z_TYPE
import java.nio.ByteBuffer

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
object Utils {
    /**
     * 获取动画加载的对话框
     */
    fun getDialog(
        context: Context,
        type: Z_TYPE = Z_TYPE.CIRCLE,
        color: Int = Color.RED
    ): ZLoadingDialog =
        ZLoadingDialog(context)
            .setLoadingBuilder(type) //设置类型
            .setLoadingColor(color) //颜色
            .setHintText("Loading...")
            .setHintTextSize(16f) // 设置字体大小 dp
            .setHintTextColor(Color.GRAY) // 设置字体颜色
            .setDurationTime(0.5) // 设置动画时间百分比 - 0.5倍
            .setDialogBackgroundColor(Color.parseColor("#CC111111")) // 设置背景色，默认白色

    fun getPixelsRGBA(image: Bitmap): ByteArray? {
        // 计算图像由多少个像素点组成
        val bytes = image.byteCount
        val buffer = ByteBuffer.allocate(bytes) // 创建一个新的buffer
        image.copyPixelsToBuffer(buffer) // 将数据赋值给buffer
        return buffer.array()
    }

    fun cropBitmap(bitmap: Bitmap, box: Box?): Bitmap =
        if (box == null) {
            bitmap
        } else {
            Bitmap.createBitmap(
                bitmap,
                (box.x1 - 30).coerceAtLeast(0),
                (box.y1 - 30).coerceAtLeast(0),
                (box.x2 - box.x1 + 90).coerceAtMost(
                    bitmap.width - (box.x1 - 30).coerceAtLeast(0)
                ),
                (box.y2 - box.y1 + 90).coerceAtMost(
                    bitmap.height - (box.y1 - 30).coerceAtLeast(0)
                )
            )
        }

    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    fun scaleBitmap(origin: Bitmap?, ratio: Float): Bitmap? {
        if (origin == null) {
            return null
        }
        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.preScale(ratio, ratio)
        val newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
        if (newBM == origin) {
            return newBM
        }
//        origin.recycle()
        return newBM
    }

}