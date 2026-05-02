// SPDX-License-Identifier: Apache-2.0
package org.hiero.android_example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.hiero.android_example.ui.main.SectionsPagerAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit  var sectionsPagerAdapter: SectionsPagerAdapter
    private lateinit var viewPager: ViewPager2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sectionsPagerAdapter = SectionsPagerAdapter(this)
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs = findViewById<TabLayout>(R.id.tabs)
        TabLayoutMediator(tabs, viewPager
        ) { tab: TabLayout.Tab, position: Int ->
            when (position) {
                0 -> tab.setText(R.string.tab_title_1)
                1 -> tab.setText(R.string.tab_title_2)
                2 -> tab.setText(R.string.tab_title_3)
                else -> tab.text = "unhandled"
            }
        }.attach()
    }
}

