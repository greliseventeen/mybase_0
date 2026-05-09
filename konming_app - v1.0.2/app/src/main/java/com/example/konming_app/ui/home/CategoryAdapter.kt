package com.example.konming_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konming_app.R

class CategoryAdapter(
    private var categories: List<String>,
    private val onItemClick: (String) -> Unit = {}
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0
    
    fun updateData(newCategories: List<String>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_label, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategory.text = category
        
        if (position == selectedPosition) {
            holder.tvCategory.setBackgroundResource(R.drawable.category_tag_selected)
            holder.tvCategory.setTextColor(holder.itemView.context.resources.getColor(android.R.color.white))
        } else {
            holder.tvCategory.setBackgroundResource(R.drawable.category_tag_unselected)
            holder.tvCategory.setTextColor(holder.itemView.context.resources.getColor(R.color.text_secondary))
        }
        
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onItemClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}