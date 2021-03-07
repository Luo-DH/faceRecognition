package com.luo.face2.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.apkfuns.logutils.LogUtils
import com.luo.face2.databinding.FragmentAddBinding
import com.luo.face2.utils.Resource
import com.luo.face2.utils.Status
import com.luo.face2.utils.Utils
import com.luo.face2.viewmodel.MainActivityViewModel
import com.wildma.pictureselector.PictureBean
import com.wildma.pictureselector.PictureSelector
import com.zyao89.view.zloading.ZLoadingDialog
import dagger.hilt.android.AndroidEntryPoint

/**
 * @author: Luo-DH
 * @date: 3/7/21
 *  人脸添加操作
 *  从图库选择图片，输入姓名，点击按钮提交，提交完成推出页面
 */
@AndroidEntryPoint
class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel by activityViewModels<MainActivityViewModel>()

    private var tmpBitmap: Bitmap? = null

    private lateinit var dialog: ZLoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()

        setupClickListener()

        setupObserver()
    }

    private fun initUI() {
        dialog = Utils.getDialog(requireContext(), color = Color.GRAY)
    }

    private fun setupClickListener() {
        // 当用户点击图片时，打开图库进行图片挑选
        binding.cardView.setOnClickListener {
            PictureSelector
                .create(this, PictureSelector.SELECT_REQUEST_CODE)
                .selectPicture(true, 500, 500, 1, 1)
        }

        // 当用户点击上传头像
        binding.addFaBtnUpgrade.setOnClickListener {
            val name = binding.addFaEditInput.text
            if (name.isNullOrBlank()) {
                Toast.makeText(requireContext(), "请输入名字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (tmpBitmap == null) {
                Toast.makeText(requireContext(), "请选择图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 添加人脸
            activityViewModel.addOneFace(tmpBitmap!!, name.toString())
        }
    }

    private fun setupObserver() {
        activityViewModel.faceList.observe(this.viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                when (it.status) {
                    Status.LOADING -> {
                        dialog.show()
                    }
                    Status.ERROR -> {
                        dialog.cancel()
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                    }
                    Status.SUCCESS -> {
                        dialog.cancel()
                        findNavController().popBackStack()
                        Toast.makeText(activity?.applicationContext, "添加成功", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PictureSelector.SELECT_REQUEST_CODE) {
            if (data != null) {
                val pictureBean: PictureBean =
                    data.getParcelableExtra(PictureSelector.PICTURE_RESULT)!!
                val bitmap = BitmapFactory.decodeFile(pictureBean.path)
                binding.addFaIvAvatar.setImageBitmap(bitmap)
                tmpBitmap = bitmap
            }
        }

    }

}
