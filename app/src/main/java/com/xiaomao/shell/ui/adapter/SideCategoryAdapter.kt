package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.Category
import com.xiaomao.shell.databinding.ItemSideCategoryBinding

class SideCategoryAdapter(
    private val onClick: (Category) -> Unit,
) : RecyclerView.Adapter<SideCategoryAdapter.SideCategoryViewHolder>() {
    private val items = mutableListOf<Category>()
    private var selectedId: String? = null

    fun submitList(list: List<Category>, selectedTypeId: String?) {
        items.clear()
        items.addAll(list)
        selectedId = selectedTypeId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SideCategoryViewHolder {
        val binding = ItemSideCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SideCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SideCategoryViewHolder, position: Int) {
        holder.bind(items[position], items[position].typeId == selectedId)
    }

    override fun getItemCount(): Int = items.size

    inner class SideCategoryViewHolder(
        private val binding: ItemSideCategoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Category, selected: Boolean) {
            binding.textCategory.text = item.name
            binding.textCategory.setBackgroundResource(
                if (selected) R.drawable.bg_tab_green else R.drawable.bg_dark_card_soft,
            )
            binding.textCategory.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (selected) R.color.black else R.color.text_primary,
                ),
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

