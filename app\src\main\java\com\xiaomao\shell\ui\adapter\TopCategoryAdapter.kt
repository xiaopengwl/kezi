package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.Category
import com.xiaomao.shell.databinding.ItemTopCategoryBinding

class TopCategoryAdapter(
    private val onClick: (Category) -> Unit,
) : RecyclerView.Adapter<TopCategoryAdapter.TopCategoryViewHolder>() {
    private val items = mutableListOf<Category>()
    private var selectedId: String? = null

    fun submitList(list: List<Category>, selectedTypeId: String?) {
        items.clear()
        items.addAll(list)
        selectedId = selectedTypeId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopCategoryViewHolder {
        val binding = ItemTopCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopCategoryViewHolder, position: Int) {
        holder.bind(items[position], items[position].typeId == selectedId)
    }

    override fun getItemCount(): Int = items.size

    inner class TopCategoryViewHolder(
        private val binding: ItemTopCategoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Category, selected: Boolean) {
            binding.textCategory.text = item.name
            binding.textCategory.setBackgroundResource(
                if (selected) R.drawable.bg_tab_green else R.drawable.bg_tab_dark,
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

