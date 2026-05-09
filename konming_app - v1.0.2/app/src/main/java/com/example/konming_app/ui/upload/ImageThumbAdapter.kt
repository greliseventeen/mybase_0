package com.example.konming_app.ui.upload

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.konming_app.R
import java.io.File

class ImageThumbAdapter(
    private val imagePaths: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageThumbAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_thumb, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val path = imagePaths[position]
        holder.ivImage.setImageURI(Uri.fromFile(File(path)))
        
        holder.ivDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = imagePaths.size

    fun removeAt(position: Int) {
        imagePaths.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, imagePaths.size)
    }

    fun addAll(paths: List<String>) {
        val startPosition = imagePaths.size
        imagePaths.addAll(paths)
        notifyItemRangeInserted(startPosition, paths.size)
    }

    fun add(path: String) {
        imagePaths.add(path)
        notifyItemInserted(imagePaths.size - 1)
    }
}
