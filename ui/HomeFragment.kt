package com.luo.face2.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.luo.face2.R
import com.luo.face2.databinding.FragmentHomeBinding

/**
 * @author: Luo-DH
 * @date: 3/7/21
 *  主页，用户点击app看到的第一个页面
 *  提供按钮选项跳转到不同页面
 *  - 人脸录入：
 *  - 人脸识别：
 *  - 人脸库一览：
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListener()

    }

    private fun setupClickListener() {

        binding.apply {
            // 添加人脸
            homeFhBtnAddFace.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_addFragment)
            }
            // 人脸识别
            homeFhBtnDetectFace.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_detectFragment)
            }
            // 人脸库
            homeFhBtnFaceList.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_listFragment)
            }
        }

    }

}