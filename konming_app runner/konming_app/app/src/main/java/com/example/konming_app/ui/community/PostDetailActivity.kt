package com.example.konming_app.ui.community

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.konming_app.R
import com.example.konming_app.data.model.Post
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PostDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_POST_ID = "post_id"
    }

    private lateinit var ivBack: ImageView
    private lateinit var tvContent: TextView
    private lateinit var rvImages: RecyclerView

    private var post: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        initViews()
        val postId = intent.getIntExtra(EXTRA_POST_ID, -1)
        if (postId != -1) {
            loadPost(postId)
        }
    }

    private fun initViews() {
        ivBack = findViewById(R.id.iv_back)
        tvContent = findViewById(R.id.tv_content)
        rvImages = findViewById(R.id.rv_images)

        ivBack.setOnClickListener {
            finish()
        }
    }

    private fun loadPost(postId: Int) {
        lifecycleScope.launch {
            post = withContext(Dispatchers.IO) {
                RepositoryFactory.getPostRepository().getPostById(postId)
            }

            post?.let {
                setupUI(it)
                val userId = RepositoryFactory.getPreferenceManager().getLoggedInUserId()
                if (userId != -1) {
                    withContext(Dispatchers.IO) {
                        RepositoryFactory.getBrowseHistoryRepository().addPostHistory(userId, it.id)
                    }
                }
            }
        }
    }

    private fun setupUI(post: Post) {
        tvContent.text = post.content

        val imagePaths = post.imagePaths
        if (!imagePaths.isNullOrEmpty()) {
            val paths = imagePaths.split(";").filter { it.isNotEmpty() }
            if (paths.isNotEmpty()) {
                rvImages.visibility = android.view.View.VISIBLE
                rvImages.layoutManager = GridLayoutManager(this, 3)
                val adapter = PostImageAdapter()
                adapter.submitList(paths)
                rvImages.adapter = adapter
                rvImages.addItemDecoration(ItemSpacingDecoration(4))
            }
        }
    }
}
