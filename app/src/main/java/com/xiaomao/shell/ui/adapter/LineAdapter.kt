package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.PlayGroup
import com.xiaomao.shell.databinding.ItemLineBinding

class LineAdapter(
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<LineAdapter.LineViewHolder>() {
    private val items = mutableListOf<PlayGroup>()
    private var selectedIndex = 0

    fun submitList(list: List<PlayGroup>, selected: Int) {
        items.clear()
        items.addAll(list)
        selectedIndex = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineViewHolder {
        val binding = ItemLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LineViewHolder, position: Int) {
        holder.bind(items[position], position == selectedIndex, position)
    }

    override fun getItemCount(): Int = items.size

    inner class LineViewHolder(
        private val binding: ItemLineBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlayGroup, selected: Boolean, position: Int) {
            binding.textLine.text = item.name
            binding.textLine.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (selected) R.color.white else R.color.text_primary,
                ),
            )
            binding.textLine.setBackgroundResource(
                if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip,
            )
            binding.root.setOnClickListener { onClick(position) }
        }
    }
}

