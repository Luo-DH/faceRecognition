package com.luo.face2.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.GridLayoutManager
import com.luo.face2.adapters.FaceListAdapter
import com.luo.face2.databinding.FragmentListBinding
import com.luo.face2.modules.FaceMsg
import com.luo.face2.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
@AndroidEntryPoint
class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private val activityViewModel by activityViewModels<MainActivityViewModel>()

    @Inject
    lateinit var faceListAdapter: FaceListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerview()

        setupObserver()
    }

    private fun initRecyclerview() {
        binding.rv.apply {
            adapter = faceListAdapter
            layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
        }
    }

    private fun setupObserver() {
        activityViewModel.faceList.observe(this.viewLifecycleOwner) {
            val faceList = ArrayList<FaceMsg>()
            it.peekContent().data?.let { datas ->
                datas.forEach { data ->
                    faceList.add(
                        FaceMsg(data.key, data.value.bitmap)
                    )
                }
            }
            faceListAdapter.datas = faceList
        }
    }

}