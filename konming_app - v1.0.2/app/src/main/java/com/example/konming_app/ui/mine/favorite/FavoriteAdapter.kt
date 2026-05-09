package com.example.konming_app.ui.mine.favorite

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
import com.example.konming_app.data.model.FavoriteItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class FavoriteAdapter(
    private val onItemClick: (FavoriteItem) -> Unit,
    private val onDeleteClick: (FavoriteItem) -> Unit
) : ListAdapter<FavoriteItem, RecyclerView.ViewHolder>(FavoriteDiffCallback()) {

    private val TYPE_CONTENT = 0
    private val TYPE_POST = 1

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FavoriteItem.ContentItem -> TYPE_CONTENT
            is FavoriteItem.PostItem -> TYPE_POST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CONTENT) {
            ContentViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_favorite_content, parent, false)
            )
        } else {
            PostViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_favorite_post, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ContentViewHolder -> holder.bind(getItem(position) as FavoriteItem.ContentItem)
            is PostViewHolder -> holder.bind(getItem(position) as FavoriteItem.PostItem)
        }
    }

    inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)

        fun bind(item: FavoriteItem.ContentItem) {
            tvTitle.text = item.title
            tvCategory.text = item.category ?: "其他"
            tvTime.text = dateFormat.format(item.timestamp)

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

            itemView.setOnClickListener {
                onItemClick(item)
            }

            ivDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)

        fun bind(item: FavoriteItem.PostItem) {
            val displayText = if (item.content.length > 50) {
                item.content.substring(0, 50) + "..."
            } else {
                item.content
            }
            tvContent.text = displayText
            tvTime.text = dateFormat.format(item.timestamp)

            val context = itemView.context
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

            itemView.setOnClickListener {
                onItemClick(item)
            }

            ivDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    private class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteItem>() {
        override fun areItemsTheSame(oldItem: FavoriteItem, newItem: FavoriteItem): Boolean {
            return when {
                oldItem is FavoriteItem.ContentItem && newItem is FavoriteItem.ContentItem -> 
                    oldItem.contentId == newItem.contentId
                oldItem is FavoriteItem.PostItem && newItem is FavoriteItem.PostItem -> 
                    oldItem.postId == newItem.postId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: FavoriteItem, newItem: FavoriteItem): Boolean {
            return oldItem == newItem
        }
    }
}
