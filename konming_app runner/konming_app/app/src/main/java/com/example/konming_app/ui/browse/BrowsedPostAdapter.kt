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

class BrowsedPostAdapter(
    private val onItemClick: (BrowseHistoryItem.PostHistory) -> Unit
) : ListAdapter<BrowseHistoryItem.PostHistory, BrowsedPostAdapter.ViewHolder>(PostDiffCallback()) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)

        fun bind(item: BrowseHistoryItem.PostHistory) {
            val displayText = if (item.content.length > 100) {
                item.content.substring(0, 100) + "..."
            } else {
                item.content
            }
            tvContent.text = displayText
            tvTime.text = TimeFormatter.formatRelativeTime(item.browseTime)

            val context = itemView.context
            ivAvatar.setImageResource(R.drawable.default_cover)
            if (!item.firstImagePath.isNullOrEmpty()) {
                val imageFile = File(item.firstImagePath)
                if (imageFile.exists()) {
                    ivImage.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(imageFile)
                        .placeholder(R.drawable.default_cover)
                        .centerCrop()
                        .into(ivImage)
                } else {
                    ivImage.visibility = View.GONE
                }
            } else {
                ivImage.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_browse_post, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<BrowseHistoryItem.PostHistory>() {
        override fun areItemsTheSame(
            oldItem: BrowseHistoryItem.PostHistory,
            newItem: BrowseHistoryItem.PostHistory
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: BrowseHistoryItem.PostHistory,
            newItem: BrowseHistoryItem.PostHistory
        ): Boolean = oldItem == newItem
    }
}
