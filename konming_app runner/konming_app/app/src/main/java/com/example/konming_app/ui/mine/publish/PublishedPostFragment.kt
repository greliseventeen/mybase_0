package com.example.konming_app.ui.mine.publish

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.konming_app.R
import com.example.konming_app.ui.community.PostDetailActivity

class PublishedPostFragment : Fragment() {
    private lateinit var viewModel: PublishedPostViewModel
    private lateinit var adapter: PublishedPostAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvPosts: RecyclerView
    private lateinit var llEmpty: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_published_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PublishedPostViewModel::class.java]

        initViews(view)
        setupAdapter()
        setupObservers()
        setupListeners()
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvPosts = view.findViewById(R.id.rv_list)
        llEmpty = view.findViewById(R.id.ll_empty)
    }

    private fun setupAdapter() {
        adapter = PublishedPostAdapter(
            onItemClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
                startActivity(intent)
            },
            onDeleteClick = { post ->
                showDeleteConfirmDialog(post)
            }
        )
        rvPosts.adapter = adapter
        rvPosts.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            if (posts.isEmpty()) {
                rvPosts.visibility = View.GONE
                llEmpty.visibility = View.VISIBLE
            } else {
                rvPosts.visibility = View.VISIBLE
                llEmpty.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            swipeRefresh.isRefreshing = loading
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }
    }

    private fun showDeleteConfirmDialog(post: com.example.konming_app.data.model.Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除帖子")
            .setMessage("确定要删除这条帖子吗？删除后不可恢复！")
            .setPositiveButton("确定") { _, _ ->
                viewModel.deletePost(post.id)
                Toast.makeText(requireContext(), "已删除帖子", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPosts()
    }
}
