package com.example.konming_app.ui.community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.konming_app.R
import com.example.konming_app.data.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    private val posts = mutableListOf<Post>()
    private var onItemClick: ((Post) -> Unit)? = null

    fun setOnItemClickListener(listener: (Post) -> Unit) {
        onItemClick = listener
    }

    fun submitList(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val onItemClick: ((Post) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvNickname: TextView = itemView.findViewById(R.id.tvNickname)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
        private val rvImages: RecyclerView = itemView.findViewById(R.id.rvImages)

        private val imageAdapter = PostImageAdapter()

        init {
            rvImages.layoutManager = GridLayoutManager(itemView.context, 3)
            rvImages.adapter = imageAdapter
            rvImages.addItemDecoration(ItemSpacingDecoration(2))
        }

        fun bind(post: Post) {
            tvNickname.text = "用户${post.userId}"
            tvTime.text = formatTime(post.timestamp)
            tvContent.text = post.content
            tvLikeCount.text = "${(10..200).random()}"
            tvCommentCount.text = "${(5..50).random()}"

            // 处理图片
            val imagePathsStr = post.imagePaths
            if (!imagePathsStr.isNullOrEmpty()) {
                val paths = imagePathsStr.split(";").filter { it.isNotEmpty() }
                if (paths.isNotEmpty()) {
                    rvImages.visibility = View.VISIBLE
                    imageAdapter.submitList(paths)
                } else {
                    rvImages.visibility = View.GONE
                }
            } else {
                rvImages.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(post)
            }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> {
                    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                    sdf.format(timestamp)
                }
            }
        }
    }
}
