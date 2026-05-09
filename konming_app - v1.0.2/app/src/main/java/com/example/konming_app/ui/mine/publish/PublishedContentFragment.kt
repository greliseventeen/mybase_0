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
import com.example.konming_app.ui.home.ContentDetailActivity

class PublishedContentFragment : Fragment() {
    private lateinit var viewModel: PublishedContentViewModel
    private lateinit var adapter: PublishedContentAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvContents: RecyclerView
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

        viewModel = ViewModelProvider(this)[PublishedContentViewModel::class.java]

        initViews(view)
        setupAdapter()
        setupObservers()
        setupListeners()
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvContents = view.findViewById(R.id.rv_list)
        llEmpty = view.findViewById(R.id.ll_empty)
    }

    private fun setupAdapter() {
        adapter = PublishedContentAdapter(
            onItemClick = { content ->
                val intent = Intent(requireContext(), ContentDetailActivity::class.java)
                intent.putExtra(ContentDetailActivity.EXTRA_CONTENT_ID, content.id)
                startActivity(intent)
            },
            onDeleteClick = { content ->
                showDeleteConfirmDialog(content)
            }
        )
        rvContents.adapter = adapter
        rvContents.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.contents.observe(viewLifecycleOwner) { contents ->
            adapter.submitList(contents)
            if (contents.isEmpty()) {
                rvContents.visibility = View.GONE
                llEmpty.visibility = View.VISIBLE
            } else {
                rvContents.visibility = View.VISIBLE
                llEmpty.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            swipeRefresh.isRefreshing = loading
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadContents()
        }
    }

    private fun showDeleteConfirmDialog(content: com.example.konming_app.data.model.Content) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除内容")
            .setMessage("确定要删除这条内容吗？删除后不可恢复！")
            .setPositiveButton("确定") { _, _ ->
                viewModel.deleteContent(content.id)
                Toast.makeText(requireContext(), "已删除内容", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContents()
    }
}
