package com.github.cgg.clasha

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.*
import com.chad.library.adapter.base.BaseItemDraggableAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.bg.BaseService
import com.github.cgg.clasha.data.ConfigManager
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.data.ProfileConfig
import com.github.cgg.clasha.utils.*
import com.github.cgg.clasha.widget.ClashAWebviewBottomSheetDialog
import com.github.cgg.clasha.widget.EditTextDialog
import me.rosuh.filepicker.bean.FileItemBeanImpl
import me.rosuh.filepicker.config.AbstractFileFilter
import me.rosuh.filepicker.config.AbstractFileType
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.FileType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-14
 * @describe
 */
class ProfileListFragment : ToolbarFragment(), Toolbar.OnMenuItemClickListener,
    FragmentUtils.OnBackClickListener {
    override fun onBackClick(): Boolean {
        return false
    }

    companion object {
        private const val TAG = "ClashAProfileConfigFragment"
        private const val REQUEST_IMPORT = 2
    }

    private val isEnabled get() = (activity as MainActivity).state.let { it.canStop || it == BaseService.State.Stopped }

    private val isTempAllowImport get() = (activity as MainActivity).state.let { it == BaseService.State.Idle || it == BaseService.State.Stopped }

    private lateinit var profileConfigsAdapter: ProfileConfigsAdapter
    private lateinit var mItemDragAndSwipeCallback: ItemDragAndSwipeCallback
    private lateinit var mItemTouchHelper: ItemTouchHelper
    private lateinit var snackBarRootView: View
    private val saveInformationCallback = DialogInterface.OnDismissListener { getSelectProxys() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_fr_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.app_name)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener(this)

        snackBarRootView = view.findViewById(R.id.content)

        val rv = view.findViewById<RecyclerView>(R.id.list)
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rv.layoutManager = layoutManager
        rv.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
        var profileConfigs = ConfigManager.getAllProfileConfigs() ?: mutableListOf()
        profileConfigsAdapter = ProfileConfigsAdapter(profileConfigs)
        ConfigManager.listener = profileConfigsAdapter
        mItemDragAndSwipeCallback = ItemDragAndSwipeCallback(profileConfigsAdapter)
        mItemTouchHelper = ItemTouchHelper(mItemDragAndSwipeCallback)
        mItemTouchHelper.attachToRecyclerView(rv)
        mItemDragAndSwipeCallback.setSwipeMoveFlags(ItemTouchHelper.START)
        profileConfigsAdapter.enableSwipeItem()
        profileConfigsAdapter.setOnItemSwipeListener(object : OnItemSwipeListener {
            override fun clearView(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
            }

            override fun onItemSwiped(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
            }

            override fun onItemSwipeStart(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
            }

            override fun onItemSwipeMoving(
                canvas: Canvas?,
                viewHolder: RecyclerView.ViewHolder?,
                dX: Float,
                dY: Float,
                isCurrentlyActive: Boolean
            ) {
                canvas?.drawColor(ContextCompat.getColor(activity as MainActivity, R.color.material_red_700))
            }
        })
        rv.adapter = profileConfigsAdapter
        //        layoutManager.scrollToPosition(profilesAdapter.profiles.indexOfFirst { it.id == DataStore.profileId })

        toolbar.findViewById<View>(R.id.action_dashboard).setOnLongClickListener {
            when ((activity as MainActivity).state) {
                BaseService.State.Connected -> {
                    val dialog = ClashAWebviewBottomSheetDialog(context!!)
                    dialog.setTitle(R.string.title_dashboard)
                    dialog.setCanBack(false)
                    dialog.setShowBackNav(false)
                    dialog.setPort(DataStore.portApi.toString())
                    dialog.loadUrl("http://clash.razord.top/")
                    dialog.setOnDismissListener(saveInformationCallback)
                    dialog.show()
                    dialog.setMaxHeight(ScreenUtils.getScreenHeight())
                    dialog.setPeekHeight(ScreenUtils.getScreenHeight())
                }
                else -> ToastUtils.showShort(R.string.message_dashboard_please_startproxy)
            }
            true
        }
    }



    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item?.itemId) {

            R.id.action_dashboard -> {
                when ((activity as MainActivity).state) {
                    BaseService.State.Connected -> {
                        val dialog = ClashAWebviewBottomSheetDialog(context!!)
                        dialog.setTitle(R.string.title_dashboard)
                        dialog.setCanBack(false)
                        dialog.setShowBackNav(false)
                        dialog.setPort(DataStore.portApi.toString())
                        dialog.loadUrl("http://127.0.0.1:8881/index.html")

                        dialog.setOnDismissListener(saveInformationCallback)
                        //dialog.loadUrl("http://clash.razord.top/")
                        dialog.show()
                        dialog.setMaxHeight(ScreenUtils.getScreenHeight())
                        dialog.setPeekHeight(ScreenUtils.getScreenHeight())

                    }
                    else -> ToastUtils.showShort(R.string.message_dashboard_please_startproxy)
                }
            }

            R.id.action_download_config -> {
                if (!isTempAllowImport) {
                    ToastUtils.showShort("请停止连接，再导入")
                } else {
                    DownloadConfigDialog().show()
                }
            }

            R.id.action_import_config -> {
                if (!isTempAllowImport) {
                    ToastUtils.showShort("请停止连接，再导入")
                } else {
                    importLocalConfig()
                }
            }
            R.id.action_make_config -> {
                AppExecutors().diskIO.execute {
                    var file = File(app.filesDir, "Country.mmdb")
                    LogUtils.w(file.absolutePath)
                    if (file.exists()) {
                        LogUtils.w("文件存在 删除")
                        file.delete()
                    }
                    if (!file.exists()) {
                        val inputStream = context!!.assets.open("Country.mmdb")
                        val su = FileIOUtils.writeFileFromIS(file, inputStream)
                        LogUtils.w("copy Country.mmdb is $su")
                        SnackbarUtils.with(snackBarRootView).setDuration(SnackbarUtils.LENGTH_LONG)
                            .apply {
                                if (su) {
                                    setMessage("fix Country.mmdb (copy Country.mmdb) is success")
                                    setBgResource(R.color.colorPrimary)
                                    showSuccess()
                                } else {
                                    setMessage("fix Country.mmdb (copy Country.mmdb) is failed")
                                    showError()
                                }
                            }
                    }
                }
            }
            7 -> {
                throw RuntimeException("This is a crash")
            }
        }

        return super.onOptionsItemSelected(item)
    }


    private inner class DownloadConfigDialog {
        val builder: AlertDialog.Builder
        val editText: EditText
        lateinit var dialog: AlertDialog

        init {
            val view = layoutInflater.inflate(R.layout.dialog_download_config, null)
            editText = view.findViewById(R.id.url_content)
            builder = AlertDialog.Builder(activity!!)
                .setTitle(R.string.download_config)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val inputUrl = editText.text.toString()
                    if (inputUrl.isNullOrEmpty() || !RegexUtils.isURL(inputUrl)) {
                        ToastUtils.showLong(R.string.message_download_url_legal)
                        return@setPositiveButton
                    }
                    //显示正在加载
                    DownloadUrl().apply {
                        url = inputUrl
                        mContext = activity
                        configName = URL(inputUrl).host
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

                }.setView(view)
        }

        fun show() {
            dialog = builder.create()
            dialog.show()
        }
    }

    private class DownloadUrl : AsyncTask<Unit, Int, String>() {

        var url = ""
        var mContext: Context? = null
        var configName: String = ""
        var progressDialog: ProgressDialog? = null
        var profileConfigId: Long? = null
        var updateCallback: ((config: ProfileConfig) -> Unit)? = null

        override fun onPreExecute() {
            super.onPreExecute()
            LogUtils.wTag(TAG, "Config Name is $configName")
            progressDialog = ProgressDialog(mContext)
            progressDialog?.setCancelable(false)
            progressDialog?.setMessage(mContext?.getString(R.string.message_download_config_progress))
            progressDialog?.show()
        }

        override fun doInBackground(vararg params: Unit?): String {
            val mClient = OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .addHeader("Connection", "keep-alive")
                .addHeader("phoneModel", Build.MODEL)
                .addHeader("systemVersion", Build.VERSION.RELEASE)
                .url(url).build()

            return try {
                mClient.newCall(request).execute().body()?.string().toString()
            } catch (e: Exception) {
                ToastUtils.showShort(R.string.message_download_config_fail)
                LogUtils.w(TAG, e)
                Crashlytics.log(Log.ERROR, TAG, e.localizedMessage)
                ""
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            result?.let { it ->
                try {

                    var config =
                        if (profileConfigId != null) ConfigManager.getProfileConfig(profileConfigId!!) else null

                    config = ProfileConfig.findProfileConfig(
                        it,
                        config,
                        configName,
                        url
                    )

                    if (profileConfigId == null) {
                        ConfigManager.createProfileConfig(config)
                    } else {
                        config.time = System.currentTimeMillis()
                        ConfigManager.updateProfileConfig(config)
                        updateCallback?.invoke(config)
                    }

                    ToastUtils.showLong(R.string.message_download_config_success)
                    Crashlytics.log(Log.INFO, TAG, mContext?.getString(R.string.message_download_config_success))
                } catch (e: Exception) {
                    ToastUtils.showShort(R.string.message_download_config_fail)
                    Crashlytics.setString("Download debug result", result)
                }
            }

            progressDialog?.dismiss()
        }

    }

    private var selectedItem: BaseViewHolder? = null

    inner class ProfileConfigsAdapter(data: List<ProfileConfig>) :
        BaseItemDraggableAdapter<ProfileConfig,
                BaseViewHolder>(R.layout.list_profile, data),
        ConfigManager.Listener {

        private fun inRange(position: Int): Boolean {
            return position >= 0 && position < mData.size
        }

        override fun onItemSwiped(viewHolder: RecyclerView.ViewHolder?) {
            val pos = getViewHolderPosition(viewHolder)
            if (inRange(pos)) {
                val profileId = mData[pos].id
                ConfigManager.delProfile(profileId)
            }
        }


        override fun onAdd(profile: ProfileConfig) {
            val pos = itemCount
            profileConfigsAdapter.data += profile
            notifyItemChanged(pos)
        }

        override fun onRemove(profileId: Long) {
            val index = mData.indexOfFirst { it.id == profileId }
            if (index < 0) return
            mData.removeAt(index)
            notifyItemRemoved(index)
            if (profileId == DataStore.profileId) DataStore.profileId = 0
        }

        override fun onCleared() {
        }

        fun refreshId(id: Long) {
            val index = data.indexOfFirst { it.id == id }
            if (index >= 0) notifyItemChanged(index)
        }

        fun refreshConfig(config: ProfileConfig) {
            val index = data.indexOfFirst { it.id == config.id }
            if (index >= 0) {
                data[index] = config
                notifyItemChanged(index)
            }
        }


        override fun convert(helper: BaseViewHolder, item: ProfileConfig) {
            helper.setText(android.R.id.text1, item.configName)
            helper.setText(R.id.text_update_time, getString(R.string.iterm_update_time, item.getDateFormatted()))

            if (TextUtils.isEmpty(item.url)) {
                helper.setGone(R.id.refresh_update, false)
            } else {
                helper.setGone(R.id.refresh_update, true)
            }
            //todo
            helper.getView<AppCompatImageView>(R.id.refresh_update).setOnClickListener { itView ->
                itView.isEnabled = false
                if (!TextUtils.isEmpty(item.url)) {
                    //显示正在加载
                    DownloadUrl().apply {
                        url = item.url!!
                        mContext = activity
                        configName = item.configName!!
                        profileConfigId = item.id
                        updateCallback = {
                            itView.isEnabled = true
                            profileConfigsAdapter.refreshConfig(it)
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }

            }
            if (item.id == DataStore.profileId) {
                helper.itemView.isSelected = true
                selectedItem = helper
            } else {
                helper.itemView.isSelected = false
                if (selectedItem === helper) selectedItem = null
            }
            helper.itemView.setOnClickListener {
                if (isEnabled) {
                    val activity = activity as MainActivity
                    val old = DataStore.profileId
                    DataStore.profileId = item.id
                    refreshId(old)
                    it.isSelected = true
                    if (activity.state.canStop) app.reloadService()
                    //todo 切换config
                }
            }

            helper.itemView.setOnLongClickListener {
                val dialog = bindEditDialog(title = getString(R.string.title_edit_config_name),
                    isMultiline = false,
                    hasOnNeutral = false,
                    onOk = { editText, tag ->
                        if (!TextUtils.isEmpty(editText.text)) {
                            val index = data.indexOfFirst { it.id == item.id }
                            profileConfigsAdapter.data[index].configName = editText.text.toString()
                            ConfigManager.updateProfileConfig(profileConfigsAdapter.data[index])
                            refreshId(item.id)
                        }
                    })

                dialog.show(
                    fragmentManager ?: (activity as MainActivity).supportFragmentManager,
                    "configName"
                )
                true
            }
        }
    }

    private fun bindEditDialog(
        title: String? = null,
        hint: String? = null,
        text: String? = null,
        tag: String? = null,
        isMultiline: Boolean = false,
        hasOnNeutral: Boolean = false,
        onOk: ((editText: EditText, tag: String) -> Unit)? = null,
        onNeutral: ((editText: EditText, tag: String) -> Unit)? = null
    ): EditTextDialog {
        val dialog = EditTextDialog.newInstance(
            title = title, hint = hint, text = text, tag = tag,
            isMultiline = isMultiline, hasOnNeutral = hasOnNeutral
        )
        dialog.onOk = onOk
        dialog.onNeutral = onNeutral
        return dialog
    }

    private fun importLocalConfig() {
        if (!PermissionUtils.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            PermissionUtils.permission(PermissionConstants.STORAGE)
                .rationale { shouldRequest -> DialogHelper.showRationaleDialog(shouldRequest, activity) }
                .callback(object : PermissionUtils.FullCallback {
                    override fun onGranted(permissionsGranted: MutableList<String>?) {
                        importLocalConfig()
                    }

                    override fun onDenied(
                        permissionsDeniedForever: MutableList<String>?,
                        permissionsDenied: MutableList<String>?
                    ) {
                        if (!permissionsDeniedForever!!.isEmpty()) {
                            DialogHelper.showOpenAppSettingDialog(activity)
                        }
                    }

                }).request()
        } else {
            FilePickerManager.from(this)
                .maxSelectable(1)
                .fileType(object : AbstractFileType() {
                    private val allDefaultFileType: ArrayList<FileType> by lazy {
                        val fileTypes = ArrayList<FileType>()

                        fileTypes.add(YmlFileType())
                        fileTypes.add(YamlFileType())
                        fileTypes
                    }

                    override fun fillFileType(itemBeanImpl: FileItemBeanImpl): FileItemBeanImpl {
                        for (type in allDefaultFileType) {
                            if (type.verify(itemBeanImpl.fileName)) {
                                itemBeanImpl.fileType = type
                                break
                            }
                        }
                        return itemBeanImpl
                    }
                })
                .filter(object : AbstractFileFilter() {
                    override fun doFilter(listData: ArrayList<FileItemBeanImpl>): ArrayList<FileItemBeanImpl> {
                        return ArrayList(listData.filter { item ->
                            ((item.isDir) || (item.fileType is YmlFileType) || (item.fileType is YamlFileType))
                        })
                    }
                })
                .forResult(REQUEST_IMPORT)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) super.onActivityResult(requestCode, resultCode, data)
        else when (requestCode) {
            REQUEST_IMPORT -> {
                val list = FilePickerManager.obtainData()
                val path = list[0]
                LogUtils.d("import config.yaml -> $path")
                if (!TextUtils.isEmpty(path)) {
                    val file = File(path!!)
                    if (file.exists()) {

                        try {
                            ConfigManager.createProfileConfig(ProfileConfig.findProfileConfig(file.readText()))
                            ToastUtils.showLong(
                                getString(R.string.message_import_config_success)
                            )
                        } catch (e: IOException) {
                            ToastUtils.showShort(R.string.message_import_config_fail)
                            Crashlytics.log(Log.ERROR, TAG, e.localizedMessage)
                        } catch (e: java.lang.RuntimeException) {
                            //yml content format error
                            e.printStackTrace()
                            ToastUtils.showLong("${getString(R.string.message_import_config_fail)}, ${e.localizedMessage}")
                            Crashlytics.log(Log.ERROR, TAG, e.localizedMessage)
                        }
                    } else {
                        ToastUtils.showShort(R.string.message_import_config_none)
                    }

                } else {
                    ToastUtils.showShort(R.string.message_import_config_fail)
                }
            }
        }
    }

    override fun onDestroy() {
        ConfigManager.listener = null
        super.onDestroy()
    }

}