package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.databinding.ItemVideoBinding

class VideoListAdapter(
    private val onClick: (VideoItem) -> Unit,
) : RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {
    private val items = mutableListOf<VideoItem>()

    fun submitList(list: List<VideoItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VideoViewHolder(
        private val binding: ItemVideoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoItem) {
            binding.imageCover.load(item.coverUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }
            binding.textTitle.text = item.title
            binding.textRemarks.text = item.remarks.ifBlank { "暂无备注" }
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

