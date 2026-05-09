package com.example.konming_app.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konming_app.R
import com.example.konming_app.data.model.BrowseHistoryItem
import com.example.konming_app.util.TimeFormatter
import java.io.File

class BrowsedContentAdapter(
    private val onItemClick: (BrowseHistoryItem.ContentHistory) -> Unit
) : ListAdapter<BrowseHistoryItem.ContentHistory, BrowsedContentAdapter.ViewHolder>(ContentDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)

        fun bind(item: BrowseHistoryItem.ContentHistory) {
            tvTitle.text = item.title
            tvCategory.text = item.category ?: "其他"
            tvTime.text = TimeFormatter.formatRelativeTime(item.browseTime)

            val context = itemView.context
            if (!item.coverPath.isNullOrEmpty()) {
                val coverFile = File(item.coverPath)
                if (coverFile.exists()) {
                    Glide.with(context)
                        .load(coverFile)
                        .placeholder(R.drawable.default_cover)
                        .centerCrop()
                        .into(ivCover)
                } else {
                    ivCover.setImageResource(R.drawable.default_cover)
                }
            } else {
                ivCover.setImageResource(R.drawable.default_cover)
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_browse_content, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ContentDiffCallback : DiffUtil.ItemCallback<BrowseHistoryItem.ContentHistory>() {
        override fun areItemsTheSame(
            oldItem: BrowseHistoryItem.ContentHistory,
            newItem: BrowseHistoryItem.ContentHistory
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: BrowseHistoryItem.ContentHistory,
            newItem: BrowseHistoryItem.ContentHistory
        ): Boolean = oldItem == newItem
    }
}
