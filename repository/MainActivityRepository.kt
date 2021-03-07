package com.luo.face2.repository

import android.graphics.Bitmap
import com.apkfuns.logutils.LogUtils
import com.luo.face2.utils.Utils
import com.luo.face2.utils.toCropBitmap
import com.luo.learnc01.face.ArcFace
import com.luo.learnc01.face.RetinaFace2
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
@Singleton
class MainActivityRepository @Inject constructor(){

    /**
     * 添加人脸
     * @param bitmap Bitmap
     * @param name String
     * @return Boolean 是否成功添加，如果没有检测到人脸返回false
     */
    suspend fun addOneFace(bitmap: Bitmap): FloatArray? {
        delay(1_000)
        val result = RetinaFace2().detect(bitmap, 1.0f)
        // 如果没有检测到人脸返回null
        if (result.isEmpty()) {
            return null
        }
        val cropBitmap =
            result.maxBy { (it.x2 - it.x1) * (it.y2 - it.y1) }!!.toCropBitmap(bitmap)
        val box = result.maxBy { (it.x2 - it.x1) * (it.y2 - it.y1) }!!
        val landmarks = IntArray(10)
        var i = 0
        box.landmarks.forEach {
            landmarks[i] = it.x.toInt() - box.x1
            landmarks[i + 5] = it.y.toInt() - box.y1
            i += 1
        }

        return ArcFace().getFeatureWithWrap2(
            Utils.getPixelsRGBA(cropBitmap), cropBitmap.width, cropBitmap.height, landmarks
        )

    }

}