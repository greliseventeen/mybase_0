package com.example.konming_app.ui.community

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konming_app.R
import java.io.File

class PostImageAdapter : RecyclerView.Adapter<PostImageAdapter.ImageViewHolder>() {
    private val imagePaths = mutableListOf<String>()

    fun submitList(paths: List<String>) {
        imagePaths.clear()
        imagePaths.addAll(paths)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imagePaths[position])
    }

    override fun getItemCount(): Int = imagePaths.size

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)

        fun bind(path: String) {
            Glide.with(itemView.context)
                .load(File(path))
                .into(ivImage)

            itemView.setOnClickListener {
                Toast.makeText(
                    itemView.context.applicationContext,
                    "图片查看功能即将开放",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
