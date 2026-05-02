// SPDX-License-Identifier: Apache-2.0
package org.hiero.android_example.ui.main

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.Fragment
import java.lang.IllegalArgumentException

class SectionsPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PrivateKeyFragment()
            1 -> AccountBalanceFragment()
            2 -> CryptoTransferFragment()
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int {
        return 3
    }
}

