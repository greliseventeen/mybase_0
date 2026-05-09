package com.example.konming_app.ui.mine.publish

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
import com.example.konming_app.data.model.Content
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PublishedContentAdapter(
    private val onItemClick: (Content) -> Unit,
    private val onDeleteClick: (Content) -> Unit
) : ListAdapter<Content, PublishedContentAdapter.ContentViewHolder>(ContentDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)
        private val btnDelete: TextView = itemView.findViewById(R.id.btn_delete)

        fun bind(content: Content) {
            tvTitle.text = content.title
            tvCategory.text = content.category ?: "其他"
            tvTime.text = dateFormat.format(content.timestamp)
            tvType.text = when (content.type) {
                "video" -> "视频"
                "audio" -> "音频"
                "image" -> "图片"
                else -> "内容"
            }

            val context = itemView.context
            if (!content.coverPath.isNullOrEmpty()) {
                val coverFile = File(content.coverPath)
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

            itemView.setOnClickListener {
                onItemClick(content)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(content)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        return ContentViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_published_content, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ContentDiffCallback : DiffUtil.ItemCallback<Content>() {
        override fun areItemsTheSame(oldItem: Content, newItem: Content): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Content, newItem: Content): Boolean {
            return oldItem == newItem
        }
    }
}
