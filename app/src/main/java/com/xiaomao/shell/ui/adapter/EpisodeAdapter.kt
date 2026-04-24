package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.PlayEpisode
import com.xiaomao.shell.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val onClick: (PlayEpisode) -> Unit,
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {
    private val items = mutableListOf<PlayEpisode>()
    private var selectedUrl: String? = null

    fun submitList(list: List<PlayEpisode>, selectedPlayUrl: String? = null) {
        items.clear()
        items.addAll(list)
        selectedUrl = selectedPlayUrl
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(items[position], items[position].playUrl == selectedUrl)
    }

    override fun getItemCount(): Int = items.size

    inner class EpisodeViewHolder(
        private val binding: ItemEpisodeBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlayEpisode, selected: Boolean) {
            binding.textEpisode.text = item.name
            binding.textEpisode.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (selected) R.color.white else R.color.text_primary,
                ),
            )
            binding.textEpisode.setBackgroundResource(
                if (selected) R.drawable.bg_chip_selected else R.drawable.bg_episode,
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

