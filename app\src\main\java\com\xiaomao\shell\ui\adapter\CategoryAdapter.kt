package com.xiaomao.shell.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xiaomao.shell.R
import com.xiaomao.shell.data.model.Category
import com.xiaomao.shell.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onClick: (Category) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    private val items = mutableListOf<Category>()
    private var selectedId: String? = null

    fun submitList(list: List<Category>, selectedTypeId: String?) {
        items.clear()
        items.addAll(list)
        selectedId = selectedTypeId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position], items[position].typeId == selectedId)
    }

    override fun getItemCount(): Int = items.size

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Category, selected: Boolean) {
            binding.textCategory.text = item.name
            binding.textCategory.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (selected) R.color.white else R.color.text_primary,
                ),
            )
            binding.textCategory.setBackgroundResource(
                if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip,
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

