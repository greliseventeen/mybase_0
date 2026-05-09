package com.example.konming_app.ui.mine.publish

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

class MyPublishActivity : AppCompatActivity() {
    private lateinit var ivBack: ImageView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private val titles = arrayOf("内容", "帖子")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_publish)

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
        viewPager.adapter = PublishPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    private fun setupListeners() {
        ivBack.setOnClickListener {
            finish()
        }
    }

    private inner class PublishPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = titles.size

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                PublishedContentFragment()
            } else {
                PublishedPostFragment()
            }
        }
    }
}
