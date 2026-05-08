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
import com.example.konming_app.data.model.Post
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class PublishedPostAdapter(
    private val onItemClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : ListAdapter<Post, PublishedPostAdapter.PostViewHolder>(PostDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.iv_image)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val btnDelete: TextView = itemView.findViewById(R.id.btn_delete)

        fun bind(post: Post) {
            val displayText = if (post.content.length > 100) {
                post.content.substring(0, 100) + "..."
            } else {
                post.content
            }
            tvContent.text = displayText
            tvTime.text = dateFormat.format(post.timestamp)

            val context = itemView.context
            val imagePaths = post.imagePaths?.split(",")
            val firstImagePath = imagePaths?.firstOrNull()

            if (!firstImagePath.isNullOrEmpty()) {
                val imageFile = File(firstImagePath)
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
                onItemClick(post)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        return PostViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_published_post, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
