package com.example.konming_app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konming_app.R
import com.example.konming_app.data.model.Content
import com.example.konming_app.data.model.User
import java.io.File

class ContentAdapter(
    private var contentList: List<Content>,
    private var favoriteIds: Set<Int> = emptySet(),
    private var userCache: Map<Int, User> = emptyMap(),
    private val onItemClick: (Content) -> Unit = {},
    private val onFavoriteClick: (Content, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ContentAdapter.ContentViewHolder>() {

    fun updateData(newList: List<Content>) {
        contentList = newList
        notifyDataSetChanged()
    }

    fun updateFavorites(newFavoriteIds: Set<Int>) {
        favoriteIds = newFavoriteIds.toMutableSet()
        notifyDataSetChanged()
    }

    fun updateUserCache(newUserCache: Map<Int, User>) {
        userCache = newUserCache
        notifyDataSetChanged()
    }

    inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
        val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)
        val tvTypeLabel: TextView = itemView.findViewById(R.id.tv_type_label)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val ivPublisherAvatar: ImageView = itemView.findViewById(R.id.iv_publisher_avatar)
        val tvPublisherName: TextView = itemView.findViewById(R.id.tv_publisher_name)
        val ivFavorite: ImageView = itemView.findViewById(R.id.iv_favorite)

        fun bind(content: Content, isFavorite: Boolean, user: User?) {
            tvTitle.text = content.title

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

            // 设置发布者信息
            user?.let {
                tvPublisherName.text = it.nickname ?: it.username
                if (!it.avatarPath.isNullOrEmpty()) {
                    val avatarFile = File(it.avatarPath)
                    if (avatarFile.exists()) {
                        Glide.with(context)
                            .load(avatarFile)
                            .placeholder(R.drawable.default_avatar)
                            .circleCrop()
                            .into(ivPublisherAvatar)
                    } else {
                        ivPublisherAvatar.setImageResource(R.drawable.default_avatar)
                    }
                } else {
                    ivPublisherAvatar.setImageResource(R.drawable.default_avatar)
                }
            } ?: run {
                tvPublisherName.text = "未知用户"
                ivPublisherAvatar.setImageResource(R.drawable.default_avatar)
            }

            when (content.type) {
                "video" -> {
                    tvTypeLabel.visibility = View.GONE
                    content.duration?.let { duration ->
                        if (duration > 0) {
                            tvDuration.text = formatDuration(duration)
                            tvDuration.visibility = View.VISIBLE
                        } else {
                            tvDuration.visibility = View.GONE
                        }
                    } ?: run {
                        tvDuration.visibility = View.GONE
                    }
                }
                "article" -> {
                    tvDuration.visibility = View.GONE
                    tvTypeLabel.visibility = View.VISIBLE
                    tvTypeLabel.text = "文章"
                }
                "audio" -> {
                    tvDuration.visibility = View.GONE
                    tvTypeLabel.visibility = View.VISIBLE
                    tvTypeLabel.text = "音频"
                }
                else -> {
                    tvDuration.visibility = View.GONE
                    tvTypeLabel.visibility = View.GONE
                }
            }

            if (isFavorite) {
                ivFavorite.setImageResource(R.drawable.ic_heart_filled)
            } else {
                ivFavorite.setImageResource(R.drawable.ic_heart_outline)
            }

            itemView.setOnClickListener {
                onItemClick(content)
            }

            ivFavorite.setOnClickListener {
                onFavoriteClick(content, !isFavorite)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content_card, parent, false)
        return ContentViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        val content = contentList[position]
        val isFavorite = favoriteIds.contains(content.id)
        val user = userCache[content.userId]
        holder.bind(content, isFavorite, user)
    }

    override fun getItemCount(): Int = contentList.size

    private fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
