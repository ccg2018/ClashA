package com.github.cgg.clasha

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.github.cgg.clasha.aidl.TrafficStats

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-14
 * @describe
 */
open class ToolbarFragment : Fragment() {
    lateinit var toolbar: Toolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_navigation_menu)
        toolbar.setNavigationOnClickListener { (activity as MainActivity).drawer.openDrawer(GravityCompat.START) }
    }

    open fun onTrafficUpdated(profileId: Long, stats: TrafficStats) {}

    open fun onBackPressed(): Boolean = false
}