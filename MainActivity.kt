package com.luo.face2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import com.luo.face2.viewmodel.MainActivityViewModel
import com.luo.learnc01.face.ArcFace
import com.luo.learnc01.face.RetinaFace2
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RetinaFace2().init(assets)
        ArcFace().init(assets)

    }

}