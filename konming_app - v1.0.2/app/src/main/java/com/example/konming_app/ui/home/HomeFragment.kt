package com.example.konming_app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.konming_app.R
import com.example.konming_app.data.local.PreferenceManager
import com.example.konming_app.data.model.Content
import com.example.konming_app.data.repository.ContentRepository
import com.example.konming_app.data.repository.RepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var contentRepository: ContentRepository
    private lateinit var preferenceManager: PreferenceManager
    
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvContent: RecyclerView
    private lateinit var layoutLoadMore: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    
    private var categoryAdapter: CategoryAdapter? = null
    private var contentAdapter: ContentAdapter? = null
    
    private var favoriteIds = mutableSetOf<Int>()
    private var userId = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        contentRepository = RepositoryFactory.getContentRepository()
        preferenceManager = RepositoryFactory.getPreferenceManager()
        userId = preferenceManager.getLoggedInUserId()
        
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        
        initViews(view)
        setupRecyclerViews()
        setupObservers()
        loadFavorites()
    }
    
    override fun onResume() {
        super.onResume()
        // 确保返回页面时重新加载收藏状态
        if (userId != -1) {
            loadFavorites()
        }
        // 确保内容列表有数据，如果为空则刷新
        if (viewModel.filteredContents.value.isNullOrEmpty()) {
            viewModel.refresh()
        }
    }
    
    private fun initViews(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        rvCategories = view.findViewById(R.id.rv_categories)
        rvContent = view.findViewById(R.id.rv_content)
        layoutLoadMore = view.findViewById(R.id.layout_load_more)
        layoutEmpty = view.findViewById(R.id.layout_empty)
        
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
            loadFavorites()
        }
    }
    
    private fun setupRecyclerViews() {
        rvCategories.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            requireContext(),
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
            false
        )
        
        rvContent.layoutManager = StaggeredGridLayoutManager(
            2,
            StaggeredGridLayoutManager.VERTICAL
        )
        
        rvContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = rvContent.layoutManager as? StaggeredGridLayoutManager
                layoutManager?.let {
                    val visibleItemPositions = it.findLastVisibleItemPositions(null)
                    val lastVisibleItem = visibleItemPositions.maxOrNull() ?: 0
                    val totalItemCount = it.itemCount
                    
                    if (totalItemCount - lastVisibleItem <= 5) {
                        viewModel.loadMore()
                    }
                }
            }
        })
    }
    
    private fun setupObservers() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isNotEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = CategoryAdapter(categories) { category ->
                        viewModel.filterByCategory(category)
                    }
                    rvCategories.adapter = categoryAdapter
                } else {
                    categoryAdapter?.updateData(categories)
                }
            }
        }
        
        viewModel.filteredContents.observe(viewLifecycleOwner) { contents ->
            if (contentAdapter == null) {
                contentAdapter = ContentAdapter(
                    contents,
                    favoriteIds,
                    viewModel.userCache.value ?: emptyMap(),
                    onItemClick = { content ->
                        openContentDetail(content)
                    },
                    onFavoriteClick = { content, isAdd ->
                        toggleFavorite(content, isAdd)
                    }
                )
                rvContent.adapter = contentAdapter
            } else {
                contentAdapter?.updateData(contents)
                contentAdapter?.updateFavorites(favoriteIds)
            }
            
            updateEmptyState(contents.isEmpty())
        }
        
        viewModel.userCache.observe(viewLifecycleOwner) { userCache ->
            contentAdapter?.updateUserCache(userCache)
        }
        
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            swipeRefreshLayout.isRefreshing = isRefreshing
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            layoutLoadMore.visibility = if (isLoading && !viewModel.isRefreshing.value!!) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            rvContent.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvContent.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }
    
    private fun loadFavorites() {
        if (userId != -1) {
            lifecycleScope.launch {
                val ids = withContext(Dispatchers.IO) {
                    contentRepository.getFavoriteContentIds(userId)
                }
                favoriteIds.clear()
                favoriteIds.addAll(ids)
                contentAdapter?.updateFavorites(favoriteIds)
            }
        }
    }
    
    private fun toggleFavorite(content: Content, isAdd: Boolean) {
        if (userId == -1) return
        
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (isAdd) {
                    contentRepository.addToFavorites(userId, content.id)
                    favoriteIds.add(content.id)
                } else {
                    contentRepository.removeFavorite(userId, content.id)
                    favoriteIds.remove(content.id)
                }
            }
            contentAdapter?.updateFavorites(favoriteIds)
        }
    }
    
    private fun openContentDetail(content: Content) {
        val intent = Intent(requireContext(), ContentDetailActivity::class.java).apply {
            putExtra(ContentDetailActivity.EXTRA_CONTENT_ID, content.id)
        }
        startActivity(intent)
    }
}
