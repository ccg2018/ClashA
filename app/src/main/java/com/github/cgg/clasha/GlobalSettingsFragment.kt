package com.github.cgg.clasha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blankj.utilcode.util.FragmentUtils
import com.blankj.utilcode.util.LogUtils

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-15
 * @describe
 */
class GlobalSettingsFragment : ToolbarFragment(),FragmentUtils.OnBackClickListener {

    override fun onBackClick(): Boolean {
        if (FragmentUtils.dispatchBackPress(childFragmentManager)) return true
        return if (childFragmentManager.backStackEntryCount == 0) {
            false
        } else {
            childFragmentManager.popBackStack()
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_global_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.global_settings)

        if (savedInstanceState != null) return

        FragmentUtils.add(
            childFragmentManager,
            GlobalSettingsPreferenceFragment(),
            R.id.content
        )

    }

}