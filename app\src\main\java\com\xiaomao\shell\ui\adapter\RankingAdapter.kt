package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.VideoItem
import com.xiaomao.shell.databinding.ItemRankingBinding

class RankingAdapter(
    private val onClick: (VideoItem) -> Unit,
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {
    private val items = mutableListOf<VideoItem>()

    fun submitList(list: List<VideoItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding = ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class RankingViewHolder(
        private val binding: ItemRankingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoItem, position: Int) {
            binding.imageCover.load(item.coverUrl) {
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
                crossfade(true)
            }
            binding.textTitle.text = item.title
            binding.textDesc.text = item.remarks.ifBlank { "全网热播中" }
            binding.textHeat.text = binding.root.context.getString(
                R.string.label_rank_hot,
                8105 - (position * 357),
            )
            binding.textNumber.text = (position + 1).toString()
            binding.textNumber.setBackgroundResource(
                if (position < 3) R.drawable.bg_rank_number else R.drawable.bg_rank_number_alt,
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
