package com.example.konming_app.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.konming_app.R
import com.example.konming_app.ui.home.ContentDetailActivity

class BrowsedContentHistoryFragment : Fragment() {
    private lateinit var viewModel: BrowsedContentHistoryViewModel
    private lateinit var adapter: BrowsedContentAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvList: RecyclerView
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

        viewModel = ViewModelProvider(this)[BrowsedContentHistoryViewModel::class.java]

        initViews(view)
        setupAdapter()
        setupObservers()
        setupListeners()
        viewModel.loadData(true)
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        rvList = view.findViewById(R.id.rv_list)
        llEmpty = view.findViewById(R.id.ll_empty)
    }

    private fun setupAdapter() {
        adapter = BrowsedContentAdapter { item ->
            val intent = android.content.Intent(requireContext(), ContentDetailActivity::class.java)
            intent.putExtra(ContentDetailActivity.EXTRA_CONTENT_ID, item.contentId)
            startActivity(intent)
        }
        rvList.adapter = adapter
        rvList.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            if (items.isEmpty()) {
                rvList.visibility = View.GONE
                llEmpty.visibility = View.VISIBLE
            } else {
                rvList.visibility = View.VISIBLE
                llEmpty.visibility = View.GONE
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            swipeRefresh.isRefreshing = loading
        }
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            viewModel.loadData(true)
        }
    }
}
