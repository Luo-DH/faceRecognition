package com.luo.face2.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.apkfuns.logutils.LogUtils
import com.google.common.util.concurrent.ListenableFuture
import com.luo.face2.databinding.FragmentDetectBinding
import com.luo.face2.utils.toCropBitmap
import com.luo.face2.utils.toRotaBitmap
import com.luo.face2.viewmodel.DetectFragmentViewModel
import com.luo.face2.viewmodel.MainActivityViewModel
import com.luo.learnc01.modules.Bbox
import com.luo.learnc01.modules.Box
import com.luo.learnc01.modules.DBMsg
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
@AndroidEntryPoint
class DetectFragment : Fragment() {

    private var _binding: FragmentDetectBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel by activityViewModels<MainActivityViewModel>()
    private val viewModel by viewModels<DetectFragmentViewModel>()

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider

    private var executor = Executors.newSingleThreadExecutor()

    private lateinit var preview: Preview
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageAnalysis: ImageAnalysis

    private var rotaBitmap: Bitmap? = null

    private lateinit var dbFeaturesWithBitmap: HashMap<String, DBMsg>

    // 当前帧对应的名字
    private lateinit var name: String

    // 当前帧人脸框信息，没有检测到人脸就是空
    private var lastBox: Bbox? = null
    private var lastBox2: Box? = null

    // 正常人脸阈值
    private var threshold: Double = 0.7

    // 高分人脸阈值
    private var thresholdHigh: Double = 0.8

    // 低分人脸阈值
    private var thresholdLow: Double = 0.4

    // 是否可以进行人脸检测
    private var canDetect: Boolean = true

    private val voteMap = HashMap<String, Int>()

    // 选择镜头
    private var lensFacing = CameraSelector.LENS_FACING_FRONT


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        canDetect = true
        binding.imageView2.visibility = View.GONE
        binding.imageViewFinalBitmap.visibility = View.GONE
        binding.finalName.visibility = View.GONE

        _binding = null
        cameraProvider.unbindAll()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()


        // 设置相机
        setupCameraProviderFuture()

        // 设置数据监听
        setupObserver()

    }

    private fun initUI() {
        dbFeaturesWithBitmap = activityViewModel.faceList.value?.peekContent()?.data ?: HashMap()
        if (dbFeaturesWithBitmap.size == 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack()
                Toast.makeText(requireContext(), "请添加人脸，人脸库为空", Toast.LENGTH_SHORT).show()
            }, 100)
        }
    }

    /**
     * 设置相机参数
     */
    private fun setupCameraProviderFuture() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext()).apply {
            addListener(Runnable {
                cameraProvider = this.get()

                // 设置预览
                preview = setupPreview()

                // 镜头选择
                cameraSelector = setupCameraSelector()

                // 处理图片
                imageAnalysis = setupImageAnalysis()

                cameraProvider.bindToLifecycle(
                    requireActivity(),
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            }, ContextCompat.getMainExecutor(requireContext()))
        }
    }

    /**
     * 设置预览参数
     */
    private fun setupPreview() = Preview.Builder()
        .setTargetResolution(Size(960, 1280))  // 分辨率
//        .setTargetRotation(binding.pvFinder.display.rotation)
        .build().also {
            it.setSurfaceProvider(binding.pvFinder.createSurfaceProvider())
        }

    /**
     * 选择镜头
     */
    private fun setupCameraSelector() = CameraSelector.Builder()
        .requireLensFacing(lensFacing).build()

    /**
     * 图片分析操作
     */
    private fun setupImageAnalysis() = ImageAnalysis.Builder()
        .setTargetResolution(Size(960, 1280))
//        .setTargetRotation(binding.pvFinder.display.rotation)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build().apply {

            // 开线程处理图片
            setAnalyzer(executor, ImageAnalysis.Analyzer { image ->

                var bitmap = Bitmap.createBitmap(
                    image.width, image.height, Bitmap.Config.ARGB_8888
                )
                image.use {
                    bitmap = viewModel.imageToBitmap2(image)
                }

                // 旋转图片
                rotaBitmap = bitmap.toRotaBitmap()

                // 人脸检测
                rotaBitmap?.let { viewModel.detectFace2(it, lastBox2) }


            })
        }


    /**
     * 设置观察者，数据监听
     *      包括图片获取，特征值获取等
     */
    private fun setupObserver() {
//
        /**
         * 人脸识别获得数据
         *      获得数据后，和数据库进行比对
         */
        viewModel.feature.observe(viewLifecycleOwner) {
            // 进行人脸比对
            viewModel.calCosineDistance(it, dbFeaturesWithBitmap)
        }
//
        /**
         * 获得比对结果
         *      得到识别的人名
         */
        viewModel.cosDist.observe(this.viewLifecycleOwner) { results ->

            val result = results.maxBy { it.value }!!

            /**
             * 获得名字后，投票加一
             */
            val tmpName = if (result.value > threshold) result.key else "Unknow"
            var highScore = false
            var lowScore = false
            if (result.value > thresholdHigh)
                highScore = true
            if (result.value < thresholdLow)
                lowScore = true

            // 投票
//            viewModel.toVote(tmpName, voteMap)
            viewModel.toVote2(tmpName, voteMap, highScore = highScore, lowScore = lowScore)

            name = ""
            // 更新UI操作
            GlobalScope.launch(Dispatchers.Main) {

                binding.tvThr.text = "相似度：${(result.value * 100).toInt()}%"
            }
        }
//
        // 人脸识别时间监测
        viewModel.faceCast.observe(this.viewLifecycleOwner) {
            binding.textView.text = "人脸识别耗时: ${it} ms"
        }
//
        // 人脸检测获得数据
        viewModel.detectBox2.observe(this.viewLifecycleOwner) {
            rotaBitmap?.copy(Bitmap.Config.ARGB_8888, true)?.let { bitmap ->
                analysisBitmap(bitmap, it)
            }
        }

        // 人脸检测时间检测
        viewModel.detectCast.observe(this.viewLifecycleOwner) {
            binding.valTxtView.text = "人脸监测耗时: ${it} ms"
        }
//
        // 绘制人脸框检测
        viewModel.bitmapRes.observe(this.viewLifecycleOwner) {
            requireActivity().runOnUiThread {
                binding.imageView.setImageBitmap(it)
            }
        }

        // 检测相机原始数据
        viewModel.bitmapMeta.observe(this.viewLifecycleOwner) { bitmap ->

            // 旋转图片
            rotaBitmap = bitmap.toRotaBitmap()

            // 人脸检测
            rotaBitmap?.let {
                viewModel.detectFace(it)
            }
        }

        activityViewModel.faceList.observe(this.viewLifecycleOwner) {
            it.peekContent().let { faceList ->
                dbFeaturesWithBitmap = faceList.data ?: HashMap()
                LogUtils.d(dbFeaturesWithBitmap)
                binding.dbNms.text = "数据库人脸数量：${faceList.data?.size ?: 0}"

            }
        }
//
//        // 数据库人脸加上图片
//        viewModel.featuresWithBitmap.observe(this.viewLifecycleOwner) {
//            dbFeaturesWithBitmap = it
//            binding.dbNms.text = "数据库人脸数量：${it.size}"
//        }
//
        // 投票更新
        viewModel.mapVote.observe(this.viewLifecycleOwner) { voteMap ->

            // 畅票
            viewModel.voting(voteMap)

        }

        // 是否在畅票
        viewModel.isVoting.observe(this.viewLifecycleOwner) {
            if (it) { // 正在畅票

            } else {
                // 更新UI操作
                GlobalScope.launch(Dispatchers.Main) {
                    binding.boxPrediction.visibility = View.GONE
                    binding.imageView2.visibility = View.VISIBLE
                    binding.imageViewFinalBitmap.visibility = View.VISIBLE
                    binding.finalName.visibility = View.VISIBLE
                    binding.imageView2.setImageBitmap(dbFeaturesWithBitmap[name]?.bitmap)
                    binding.imageViewFinalBitmap.setImageBitmap(dbFeaturesWithBitmap[name]?.bitmap)
                    binding.tvName.text = "识别结果：$name"
                    binding.tvNow.text = "识别结果：$name"
                    if (dbFeaturesWithBitmap[name] != null) {
                        binding.finalName.setTextColor(Color.GREEN)
                        binding.finalName.text = "识别成功：$name"
                    } else {
                        binding.finalName.setTextColor(Color.RED)
                        binding.finalName.text = "识别失败，请重试 ${name}"
                    }
                    viewModel.isShowing()

                }
            }
        }
        viewModel.isShowing.observe(this.viewLifecycleOwner) {
            if (!it) {
                canDetect = true
                binding.imageView2.visibility = View.GONE
                binding.imageViewFinalBitmap.visibility = View.GONE
                binding.finalName.visibility = View.GONE
            }
        }

        viewModel.finalName.observe(this.viewLifecycleOwner) {
            // 设置最终检测名字
            name = it

            // 显示结果的过程中不可识别
            canDetect = false

            // 清空投票池
            voteMap.clear()
        }

        /**
         * 连续空帧观察，如果连续10帧是空的，则清空投票池
         */
        viewModel.emptyBox.observe(this.viewLifecycleOwner) {
            if (it >= 10) {
                voteMap.clear()
            }
        }
    }

    /**
     * Helper function used to map the coordinates for objects coming out of
     * the model into the coordinates that the user sees on the screen.
     */
    private fun mapOutputCoordinates(location: RectF): RectF {
        val view_finder = binding.pvFinder

        // Step 1: map location to the preview coordinates
        val previewLocation = RectF(
            location.left * view_finder.width,
            location.top * view_finder.height,
            location.right * view_finder.width,
            location.bottom * view_finder.height
        )

//        val lensFacing = CameraSelector.LENS_FACING_BACK

        // Step 2: compensate for camera sensor orientation and mirroring
        val isFrontFacing = lensFacing == CameraSelector.LENS_FACING_FRONT
        val correctedLocation = if (isFrontFacing) {
            RectF(
                view_finder.width - previewLocation.right,
                previewLocation.top,
                view_finder.width - previewLocation.left,
                previewLocation.bottom
            )
        } else {
            previewLocation
        }

//        return correctedLocation

        // Step 3: compensate for 1:1 to 4:3 aspect ratio conversion + small margin
        val margin = 0.1f
        val requestedRatio = 18.7f / 15f
        val midX = (correctedLocation.left + correctedLocation.right) / 2f
        val midY = (correctedLocation.top + correctedLocation.bottom) / 2f
        return if (view_finder.width < view_finder.height) {
            RectF(
                midX - (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
                midY - (1f - margin) * correctedLocation.height() / 2f,
                midX + (1f + margin) * requestedRatio * correctedLocation.width() / 2f,
                midY + (1f - margin) * correctedLocation.height() / 2f
            )
        } else {
            RectF(
                midX - (1f - margin) * correctedLocation.width() / 2f,
                midY - (1f + margin) * requestedRatio * correctedLocation.height() / 2f,
                midX + (1f - margin) * correctedLocation.width() / 2f,
                midY + (1f + margin) * requestedRatio * correctedLocation.height() / 2f
            )
        }
    }

    /**
     * 处理人脸检测后得到的人脸框
     */
    private fun analysisBitmap(bitmap: Bitmap, result: List<Box>) =
        binding.pvFinder.post {
            if (!canDetect) {
                voteMap.clear()
                return@post
            }

            if (result.isEmpty()) {
                binding.boxPrediction.visibility = View.GONE
                viewModel.drawBoxRects(bitmap, null)
                lastBox = null
                lastBox2 = null
                viewModel.isEmptyBox(true)
                return@post
            }
            viewModel.isEmptyBox(false)

//            val maxResult = result.maxBy { (it.x2 - it.x1) * (it.y2 - it.y1) }!!

            val box = result.maxBy { (it.x2 - it.x1) * (it.y2 - it.y1) }!!
//            val box = maxResult


            // 记录当前帧人脸框信息
//            lastBox = box
            lastBox2 = box

            val location = mapOutputCoordinates(
                RectF().also {
                    it.left = box.x1.toFloat() / bitmap.width
                    it.right = box.x2.toFloat() / bitmap.width
                    it.top = box.y1.toFloat() / bitmap.height
                    it.bottom = box.y2.toFloat() / bitmap.height
                }
            )

            (binding.boxPrediction.layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin = location.top.toInt()
                leftMargin = location.left.toInt()
                width = min(binding.pvFinder.width, location.right.toInt() - location.left.toInt())
                height =
                    min(binding.pvFinder.height, location.bottom.toInt() - location.top.toInt())
            }

            // Make sure all UI elements are visible
            binding.boxPrediction.visibility = View.VISIBLE

            val cropBitmap =
//                maxResult.toCropBitmap(bitmap)
                result.maxBy { (it.x2 - it.x1) * (it.y2 - it.y1) }!!.toCropBitmap(bitmap)


            val landmarks = IntArray(10)
            var i = 0
            box.landmarks.forEach {
                landmarks[i] = it.x.toInt() - box.x1
                landmarks[i + 5] = it.y.toInt() - box.y1
                i += 1
            }

            viewModel.getFeature(cropBitmap, landmarks)

            // 绘制人脸框
            viewModel.drawBoxRects(bitmap, result.maxBy { (it.x2 - it.x1) * (it.y2 - it.y1) }!!)
//            viewModel.drawBoxRects(bitmap, maxResult)
        }


}