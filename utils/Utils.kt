package com.luo.face2.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
}