package com.luo.face2.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.luo.face2.databinding.CellFaceListBinding
import com.luo.face2.modules.FaceMsg
import com.luo.learnc01.modules.DBMsg

/**
 * @author: Luo-DH
 * @date: 3/7/21
 */
class FaceListAdapter : RecyclerView.Adapter<FaceListAdapter.FaceListViewHolder>() {

    inner class FaceListViewHolder(itemView: CellFaceListBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val title = itemView.tvTitle
        val avatar = itemView.iv
        val root = itemView.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaceListViewHolder {
        val holder = FaceListViewHolder(
            CellFaceListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
        holder.root.setOnClickListener {

        }
        return holder
    }

    override fun getItemCount() = datas.size

    override fun onBindViewHolder(holder: FaceListViewHolder, position: Int) {
        val data = datas[position]
        holder.apply {
            title.text = data.name
            avatar.setImageBitmap(data.bitmap)
        }
    }

    private val differUtil = object : DiffUtil.ItemCallback<FaceMsg>() {
        override fun areItemsTheSame(oldItem: FaceMsg, newItem: FaceMsg): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: FaceMsg, newItem: FaceMsg): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

    }

    private val differ = AsyncListDiffer<FaceMsg>(this, differUtil)

    var datas: List<FaceMsg>
        set(value) = differ.submitList(value)
        get() = differ.currentList

}