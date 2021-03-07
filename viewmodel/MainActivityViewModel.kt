package com.luo.face2.viewmodel

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkfuns.logutils.LogUtils
import com.luo.face2.MainActivity
import com.luo.face2.repository.MainActivityRepository
import com.luo.face2.utils.Event
import com.luo.face2.utils.Resource
import com.luo.face2.utils.Utils
import com.luo.learnc01.face.RetinaFace2
import com.luo.learnc01.modules.Box
import com.luo.learnc01.modules.DBMsg
import kotlinx.coroutines.launch

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
class MainActivityViewModel
@ViewModelInject constructor(
    private val repository: MainActivityRepository
): ViewModel() {

    private val _faceList = MutableLiveData<Event<Resource<HashMap<String, DBMsg>>>>()
    val faceList: LiveData<Event<Resource<HashMap<String, DBMsg>>>> = _faceList




    /**
     * 添加人脸
     * @param bitmap Bitmap
     * @param name String
     */
    fun addOneFace(bitmap: Bitmap, name: String) {
        viewModelScope.launch {
            val features = _faceList.value?.peekContent()?.data ?: HashMap()
            _faceList.postValue(Event(Resource.loading(null)))
            val result = repository.addOneFace(bitmap)
            // 没有检测到人脸
            if (result == null) {
                _faceList.postValue(Event(Resource.error("没有检测到人脸", null)))
                return@launch
            }
            features[name] = DBMsg(result, bitmap)
            _faceList.postValue(Event(Resource.success(features)))
        }
    }



}