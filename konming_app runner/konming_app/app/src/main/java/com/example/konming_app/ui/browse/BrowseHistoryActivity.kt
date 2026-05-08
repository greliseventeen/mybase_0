package com.example.konming_app.ui.browse

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.example.konming_app.R

class BrowseHistoryActivity : AppCompatActivity() {
    private lateinit var ivBack: ImageView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private val titles = arrayOf("浏览的内容", "浏览的帖子")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_history)

        initViews()
        setupViewPager()
        setupListeners()
    }

    private fun initViews() {
        ivBack = findViewById(R.id.iv_back)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
    }

    private fun setupViewPager() {
        viewPager.adapter = BrowsePagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }
    }

    private inner class BrowsePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = titles.size

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                BrowsedContentHistoryFragment()
            } else {
                BrowsedPostHistoryFragment()
            }
        }
    }
}
