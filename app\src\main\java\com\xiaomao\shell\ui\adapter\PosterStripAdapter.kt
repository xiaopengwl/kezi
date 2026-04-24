package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.databinding.ItemPosterStripBinding

class PosterStripAdapter(
    private val onClick: (VideoItem) -> Unit,
) : RecyclerView.Adapter<PosterStripAdapter.PosterViewHolder>() {
    private val items = mutableListOf<VideoItem>()

    fun submitList(list: List<VideoItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val binding = ItemPosterStripBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PosterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PosterViewHolder(
        private val binding: ItemPosterStripBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoItem) {
            binding.imagePoster.load(item.coverUrl) {
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
                crossfade(true)
            }
            binding.textTitle.text = item.title
            binding.textSource.text = item.remarks.ifBlank { "更新中" }
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

