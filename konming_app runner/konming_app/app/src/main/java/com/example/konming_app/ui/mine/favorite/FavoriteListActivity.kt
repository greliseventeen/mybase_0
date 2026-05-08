package com.example.konming_app.ui.mine.favorite

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.konming_app.R
import com.example.konming_app.data.model.FavoriteItem
import com.example.konming_app.ui.community.PostDetailActivity
import com.example.konming_app.ui.home.ContentDetailActivity

class FavoriteListActivity : AppCompatActivity() {
    private lateinit var viewModel: FavoriteViewModel
    private lateinit var adapter: FavoriteAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvFavorites: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var ivBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_list)

        viewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]

        initViews()
        setupAdapter()
        setupObservers()
        setupListeners()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.iv_back)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        rvFavorites = findViewById(R.id.rv_favorites)
        llEmpty = findViewById(R.id.ll_empty)
    }

    private fun setupAdapter() {
        adapter = FavoriteAdapter(
            onItemClick = { item ->
                when (item) {
                    is FavoriteItem.ContentItem -> {
                        val intent = Intent(this, ContentDetailActivity::class.java)
                        intent.putExtra(ContentDetailActivity.EXTRA_CONTENT_ID, item.contentId)
                        startActivity(intent)
                    }
                    is FavoriteItem.PostItem -> {
                        val intent = Intent(this, PostDetailActivity::class.java)
                        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.postId)
                        startActivity(intent)
                    }
                }
            },
            onDeleteClick = { item ->
                showDeleteConfirmDialog(item)
            }
        )
        rvFavorites.adapter = adapter
        rvFavorites.layoutManager = LinearLayoutManager(this)
    }

    private fun setupObservers() {
        viewModel.favorites.observe(this) { items ->
            adapter.submitList(items)
            if (items.isEmpty()) {
                rvFavorites.visibility = android.view.View.GONE
                llEmpty.visibility = android.view.View.VISIBLE
            } else {
                rvFavorites.visibility = android.view.View.VISIBLE
                llEmpty.visibility = android.view.View.GONE
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            swipeRefresh.isRefreshing = loading
        }
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.loadFavorites()
        }
    }

    private fun showDeleteConfirmDialog(item: com.example.konming_app.data.model.FavoriteItem) {
        AlertDialog.Builder(this)
            .setTitle("删除收藏")
            .setMessage("确定要删除这条收藏吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.removeFavorite(item)
                Toast.makeText(this, "已删除收藏", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFavorites()
    }
}
