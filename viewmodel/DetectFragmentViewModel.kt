package com.luo.face2.viewmodel

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luo.face2.repository.DetectFragmentRepository
import com.luo.face2.utils.Utils
import com.luo.learnc01.face.ArcFace
import com.luo.learnc01.face.RetinaFace2
import com.luo.learnc01.modules.Box
import com.luo.learnc01.modules.DBMsg
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
class DetectFragmentViewModel
@ViewModelInject constructor(
    private val repository: DetectFragmentRepository
) : ViewModel() {

    // 人脸检测后的人脸框
    private val _detectBox2 = MutableLiveData<List<Box>>()
    val detectBox2: LiveData<List<Box>> = _detectBox2

    // 人脸识别耗时
    private val _faceCast = MutableLiveData<Long>().apply { postValue(0L) }
    val faceCast: LiveData<Long> = _faceCast

    // 人脸检测耗时
    private val _detectCast = MutableLiveData<Int>()
    val detectCast: LiveData<Int> = _detectCast

    // 人脸特征值
    private val _feature = MutableLiveData<FloatArray>()
    val feature: LiveData<FloatArray> = _feature

    // 相机元数据
    private val _bitmapMeta = MutableLiveData<Bitmap>()
    val bitmapMeta: LiveData<Bitmap> = _bitmapMeta

    // 与数据库图片进行比对
    private val _cosDist = MutableLiveData<HashMap<String, Float>>()
    val cosDist: LiveData<HashMap<String, Float>> = _cosDist

    // 用以投票的map，记录每个名字出现的次数
    private val _mapVote =
        MutableLiveData<HashMap<String, Int>>().also { it.postValue(HashMap()) }
    val mapVote: LiveData<HashMap<String, Int>> = _mapVote


    // 绘制完人脸框的bitmap
    private val _bitmapRes = MutableLiveData<Bitmap>()
    val bitmapRes: LiveData<Bitmap> = _bitmapRes

    // 记录空帧数量，达到某一数量则清空投票池
    private val _emptyBox = MutableLiveData<Int>(0)
    val emptyBox: LiveData<Int> = _emptyBox

    // 最终检测人脸结果
    private val _finalName = MutableLiveData<String>()
    val finalName: LiveData<String> = _finalName

    // 检测是否正在投票
    private val _isVoting = MutableLiveData<Boolean>().also { it.postValue(true) }
    val isVoting: LiveData<Boolean> = _isVoting
    /**
     * 人脸检测方法
     */
    fun detectFace(bitmap: Bitmap) {
        viewModelScope.launch {
            detectFaceBackGround(bitmap)
        }
    }

    private suspend fun detectFaceBackGround(bitmap: Bitmap) {
        withContext(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            val smallBitmap = Utils.scaleBitmap(bitmap, 0.5f)!!
            _detectBox2.postValue(
                RetinaFace2().detect(bitmap, 4f)
            )
            val end = System.currentTimeMillis()
            _detectCast.postValue((end - start).toInt())
        }
    }
    /**
     * 人脸检测方法
     *      根据上一帧人脸框的位置来检测，可能可以加快检测速度
     * @param bitmap: 将要检测人脸的图片，Bitmap格式
     * @param box: 上一帧人脸框信息，如果没有人脸则为null
     */
    fun detectFace2(bitmap: Bitmap, box: Box?) {
        val start = System.currentTimeMillis()
        detectFaceBackGround2(bitmap, box)
        val end = System.currentTimeMillis()
        _detectCast.postValue((end - start).toInt())    // 计算耗时
    }

    private fun detectFaceBackGround2(bitmap: Bitmap, box: Box?) {
        // 裁剪bitmap
        val cropBitmap = Utils.cropBitmap(bitmap, box)
        val smallBitmap = Utils.scaleBitmap(cropBitmap, 0.25f)!!
        val rect = FloatArray(2)
        if (box != null) {
            rect[0] = (box.x1 - 30).coerceAtLeast(0).toFloat()
            rect[1] = (box.y1 - 30).coerceAtLeast(0).toFloat()

            _detectBox2.postValue(
                RetinaFace2().detectWithROI(smallBitmap, 4f, rect)
            )
        } else {

            _detectBox2.postValue(
                RetinaFace2().detect(smallBitmap, 4f)
            )
        }

    }

    /**
     * 人脸识别，获得单个人脸特征值
     */
    fun getFeature(bitmap: Bitmap, landmarks: IntArray) {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            _feature.postValue(
                ArcFace().getFeatureWithWrap2(
                    Utils.getPixelsRGBA(bitmap), bitmap.width, bitmap.height, landmarks
                )
            )
            val end = System.currentTimeMillis()
            _faceCast.postValue((end - start))
        }
    }

    /**
     * 进行数据比对
     */
    fun calCosineDistance(feature: FloatArray, features: HashMap<String, DBMsg>) {
        GlobalScope.launch(Dispatchers.Default) {
            _cosDist.postValue(HashMap<String, Float>().apply {
                features.forEach {
                    this[it.key] = ArcFace().calCosineDistance(feature, it.value.floatArray)
                }

            })
        }

    }

    /**
     * 投票方法2
     *      将名字对应次数加一
     *      如果特征阈值大于第二个阈值，则加两分
     * @param name: 特征比对的名字
     * @param voteMap: 投票池
     * @param highScore: 高分人脸，计算两分
     * @param lowScore: 低分人脸，计算两分 (Unknown + 2)
     */
    fun toVote2(
        name: String,
        voteMap: HashMap<String, Int>,
        highScore: Boolean,
        lowScore: Boolean
    ) {

        val count = voteMap[name] ?: 0
        _mapVote.postValue(
            if (highScore || lowScore) {
                voteMap.also { it[name] = count + 2 }
            } else {
                voteMap.also { it[name] = count + 1 }
            }
        )

    }


    /**
     * 绘制人脸框
     */
    fun drawBoxRects(bitmap: Bitmap, result: Box?) {
        _bitmapRes.postValue(repository.drawBoxRects(bitmap, result))
    }

    /**
     * 把相机数据转换成bitmap
     */
    fun imageToBitmap2(image: ImageProxy) = repository.imageToBitmap(image)

    fun isEmptyBox(empty: Boolean) {
        if (empty) {
            _emptyBox.value?.plus(1)
        } else {
            _emptyBox.value = 0
        }
    }

    /**
     * 统计投票结果
     */
    fun voting(voteMap: HashMap<String, Int>) {
        viewModelScope.launch {
            val result = voteMap.maxBy { it.value }
            if (result == null || result.value < 8) {
                _isVoting.postValue(true)
            } else {
                _finalName.postValue(result.key) // 设置最后的名字
                _isVoting.postValue(false)
            }
        }

    }

    private val _isShowing = MutableLiveData<Boolean>()
    val isShowing: LiveData<Boolean> = _isShowing

    fun isShowing() {
        viewModelScope.launch {
            _isShowing.postValue(true)
            delay(1_000)
            _isShowing.postValue(false)
        }
    }
}