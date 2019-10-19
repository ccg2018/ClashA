package com.github.cgg.clasha

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.blankj.utilcode.util.FragmentUtils
import com.blankj.utilcode.util.LogUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.data.*
import com.github.cgg.clasha.databinding.LayoutLogsViewBinding
import com.github.cgg.clasha.databinding.ListLogBinding
import com.github.cgg.clasha.utils.Key.LOG_PAGESIZE


/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-06-09
 * @describe
 */
class LogsFragment : ToolbarFragment(), FragmentUtils.OnBackClickListener, Toolbar.OnMenuItemClickListener {
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.action_remove_all_logs -> {
                LogsRepository
                    .getInstance(LogsLocalDataSource.getInstance(app.mAppExecutors, LogsDatabase.logMessageDao))
                    .removeAll()
                mAdapter.data.clear()
                mAdapter.notifyDataSetChanged()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    lateinit var dataBinding: LayoutLogsViewBinding
    lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    lateinit var list: RecyclerView
    val logsViewModel by lazy {
        ViewModelProviders.of(context as FragmentActivity).get(LogsViewModel::class.java)
    }

    val mAdapter by lazy {
        LogsAdapter()
    }
    private var nextRequestPage: Int = 1

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
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.layout_logs_view, container, false)

        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        LogUtils.iTag("life", "onViewCreated")

        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.logs_menu)
        toolbar.setOnMenuItemClickListener(this)

        mSwipeRefreshLayout = dataBinding.root.findViewById(R.id.swipeLayout)
        list = dataBinding.root.findViewById(R.id.list)
        dataBinding.logsViewModel = logsViewModel
        dataBinding.lifecycleOwner = this
        logsViewModel.title.set(getString(R.string.logs_view))
        initAdapter()
        initRefreshLayout()
        if (logsViewModel.data.value?.isEmpty() != false) {
            mSwipeRefreshLayout.isRefreshing = true
            app.handler.postDelayed({
                refresh()
            }, 500)
        } else {
            setData(true, logsViewModel.data.value ?: arrayListOf())
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        LogUtils.iTag("life", "onActivityCreated")
    }

    private fun initAdapter() {
        mAdapter.setOnLoadMoreListener({ loadMore() }, list)
        mAdapter.openLoadAnimation(BaseQuickAdapter.ALPHAIN)
        mAdapter.isFirstOnly(true)
        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        list.layoutManager = linearLayoutManager
        list.addItemDecoration(DividerItemDecoration(context, linearLayoutManager.orientation))
        list.adapter = mAdapter
    }

    private fun initRefreshLayout() {
        mSwipeRefreshLayout.setOnRefreshListener { refresh() }
    }

    private fun setData(isRefresh: Boolean, data: List<LogMessage>) {
        LogUtils.json("setData", data)
        nextRequestPage++
        if (isRefresh) {
            val calculateDiff = DiffUtil.calculateDiff(LogDiffCallback(mAdapter.data, data))
//            mAdapter.setNewData(data)
            mAdapter.data.clear()
            mAdapter.data.addAll(data)
            logsViewModel.setData(data)
            calculateDiff.dispatchUpdatesTo(mAdapter)
        } else {
            if (data.isNotEmpty()) {
                mAdapter.addData(data)
            }
        }

        if (data.size < LOG_PAGESIZE) {
            mAdapter.loadMoreEnd(isRefresh)
        } else {
            mAdapter.loadMoreComplete()
        }
    }

    private fun refresh() {
        nextRequestPage = 1
        mAdapter.setEnableLoadMore(false)//这里的作用是防止下拉刷新的时候还可以上拉加载
        //请求
        LogsRepository
            .getInstance(LogsLocalDataSource.getInstance(app.mAppExecutors, LogsDatabase.logMessageDao))
            .getLogsByPage(app.currentProfileConfig?.id ?: 0,
                nextRequestPage - 1, object : LogsDataSource.LoadLogsCallback {
                    override fun onLogsLoaded(logs: List<LogMessage>) {
                        setData(true, logs)
                        mAdapter.setEnableLoadMore(true)
                        mSwipeRefreshLayout.isRefreshing = false
                    }

                    override fun onDataNotAvailable() {
                        mAdapter.setEnableLoadMore(true)
                        mSwipeRefreshLayout.isRefreshing = false
                    }
                })
    }


    private fun loadMore() {
        LogUtils.iTag("loadMore", "加载新数据page: ${nextRequestPage}")
        LogsRepository
            .getInstance(LogsLocalDataSource.getInstance(app.mAppExecutors, LogsDatabase.logMessageDao))
            .getLogsByPage(app.currentProfileConfig?.id ?: 0,
                nextRequestPage - 1, object : LogsDataSource.LoadLogsCallback {
                    override fun onLogsLoaded(logs: List<LogMessage>) {
                        val isRefresh = nextRequestPage == 1
                        setData(isRefresh, logs)
                    }

                    override fun onDataNotAvailable() {
                        mAdapter.loadMoreEnd()
                    }
                })
    }

    inner class LogDiffCallback(old: List<LogMessage>, new: List<LogMessage>) : DiffUtil.Callback() {

        var oldList: List<LogMessage> = old
        var newList: List<LogMessage> = new

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//            val old = oldList[oldItemPosition]
//            val new = newList[newItemPosition]
//
//            if (old.src?.equals(new.src) == false) {
//                return false
//            } else if (old.dst?.equals(new.dst) == false) {
//                return false
//            } else if (old.logType?.equals(new.logType) == false) {
//                return false
//            } else if (old.matchType?.equals(new.matchType) == false) {
//                return false
//            }
//            return true
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

    }

    inner class LogsAdapter
        : BaseQuickAdapter<LogMessage, LogsAdapter.LogViewHolder>(R.layout.list_log) {


        init {
            setHasStableIds(true)
        }

        override fun convert(helper: LogViewHolder?, item: LogMessage?) {
            val binding = helper?.getBinding()
            binding as ListLogBinding
            binding.logmsg = item
        }

        override fun getItemView(layoutResId: Int, parent: ViewGroup?): View {
            val binding: ViewDataBinding = DataBindingUtil.inflate(mLayoutInflater, layoutResId, parent, false)
                ?: return super.getItemView(layoutResId, parent)
            val view = binding.root
            view.setTag(R.id.BaseQuickAdapter_databinding_support, binding)
            return view
        }

        inner class LogViewHolder(view: View) : BaseViewHolder(view) {
            fun getBinding(): ViewDataBinding {
                return itemView.getTag(R.id.BaseQuickAdapter_databinding_support) as ViewDataBinding
            }
        }

    }


}