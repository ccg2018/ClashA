package com.github.cgg.clasha

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceDataStore
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.*
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.aidl.ClashAConnection
import com.github.cgg.clasha.aidl.IClashAService
import com.github.cgg.clasha.aidl.TrafficStats
import com.github.cgg.clasha.base.SupportActivity
import com.github.cgg.clasha.bg.BaseService
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.data.OnPreferenceDataStoreChangeListener
import com.github.cgg.clasha.utils.DialogHelper
import com.github.cgg.clasha.utils.Key
import com.github.cgg.clasha.utils.YmlFileType
import com.github.cgg.clasha.utils.loadPortFromConfig
import com.github.cgg.clasha.widget.ClashAWebviewBottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.loveplusplus.update.UpdateChecker
import me.rosuh.filepicker.bean.FileItemBeanImpl
import me.rosuh.filepicker.config.AbstractFileFilter
import me.rosuh.filepicker.config.AbstractFileType
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class MainActivity : SupportActivity(), ClashAConnection.Callback, OnPreferenceDataStoreChangeListener,
    NavigationView.OnNavigationItemSelectedListener {


    companion object {
        private const val TAG = "ClashAMainActivity"
        private const val REQUEST_CONNECT = 1
        private const val REQUEST_IMPORT = 2
        private const val RESULT_IMPORT = 1002

        var stateListener: ((BaseService.State) -> Unit)? = null
        fun pendingIntent(context: Context) = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), 0
        )

    }


    internal lateinit var drawer: DrawerLayout
    private lateinit var navigation: NavigationView
    /*---TEMP-----*/
    private lateinit var toolbar: Toolbar

    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var btnDownloadConfig: Button
    private lateinit var btnImportConfig: Button




    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String?) {
        when (key) {
            Key.serviceMode -> app.handler.post {
                connection.disconnect(this)
                connection.connect(this, this)
            }
            /*Key.nightMode -> {
                val mode = DataStore.nightMode
                AppCompatDelegate.setDefaultNightMode(when (mode) {
                    AppCompatDelegate.getDefaultNightMode() -> return
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getSystemService<UiModeManager>()!!.nightMode
                    else -> mode
                })
                recreate()
            }*/
        }
    }

    // service
    var state: BaseService.State by Delegates.observable(BaseService.State.Idle) { property, oldValue, newValue ->
        LogUtils.i("MainActivity state: $oldValue -> $newValue")
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) =
        changeState(state, msg, true)

    override fun trafficUpdated(profileId: Long, stats: TrafficStats) {
//        if (profileId == 0L) this@MainActivity.stats.updateTraffic(
//            stats.txRate, stats.rxRate, stats.txTotal, stats.rxTotal)
//        if (state != BaseService.State.Stopping) {
//            (supportFragmentManager.findFragmentById(R.id.fragment_holder) as? ToolbarFragment)
//                ?.onTrafficUpdated(profileId, stats)
//        }
    }

    override fun trafficPersisted(profileId: Long) {
//        ProfilesFragment.instance?.onTrafficPersisted(profileId)
    }

    private fun changeState(state: BaseService.State, msg: String? = null, animate: Boolean = false) {
        //fab.changeState(state, animate)
        //stats.changeState(state)
        //if (msg != null) snackbar(getString(R.string.vpn_error).format(Locale.ENGLISH, msg)).show()
        if (msg != null) ToastUtils.showShort(msg)
        this.state = state
        changeButtonState(state)
        //ProfilesFragment.instance?.profilesAdapter?.notifyDataSetChanged()  // refresh button enabled state
        stateListener?.invoke(state)


    }

    private fun changeButtonState(state: BaseService.State) = when {
        state == BaseService.State.Idle || state == BaseService.State.Stopped -> {
            btnStartService.isEnabled = true
            btnStopService.isEnabled = false
            btnImportConfig.isEnabled = true
            btnDownloadConfig.isEnabled = true
        }
        state == BaseService.State.Connecting || state == BaseService.State.Connected -> {
            btnStopService.isEnabled = true
            btnStartService.isEnabled = false
            btnDownloadConfig.isEnabled = false
            btnImportConfig.isEnabled = false
        }
        state == BaseService.State.Stopping -> btnStopService.isEnabled = false
        else -> {
        }
    }

    private fun importLocalConfig() {
        if (!PermissionUtils.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            PermissionUtils.permission(PermissionConstants.STORAGE)
                .rationale { shouldRequest -> DialogHelper.showRationaleDialog(shouldRequest, this) }
                .callback(object : PermissionUtils.FullCallback {
                    override fun onGranted(permissionsGranted: MutableList<String>?) {
                        importLocalConfig()
                    }

                    override fun onDenied(
                        permissionsDeniedForever: MutableList<String>?,
                        permissionsDenied: MutableList<String>?
                    ) {
                        if (!permissionsDeniedForever!!.isEmpty()) {
                            DialogHelper.showOpenAppSettingDialog(this@MainActivity)
                        }
                    }

                }).request()
        } else {
            FilePickerManager.from(this)
                .maxSelectable(1)
                .fileType(object : AbstractFileType() {
                    private val allDefaultFileType: ArrayList<FileType> by lazy {
                        val fileTypes = ArrayList<FileType>()
                        fileTypes.add(AudioFileType())
                        fileTypes.add(RasterImageFileType())
                        fileTypes.add(CompressedFileType())
                        fileTypes.add(DataBaseFileType())
                        fileTypes.add(ExecutableFileType())
                        fileTypes.add(FontFileType())
                        fileTypes.add(PageLayoutFileType())
                        fileTypes.add(TextFileType())
                        fileTypes.add(VideoFileType())
                        fileTypes.add(WebFileType())
                        fileTypes.add(YmlFileType())
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
                            ((item.isDir) || (item.fileType is YmlFileType))
                        })
                    }
                })
                .forResult(REQUEST_IMPORT)
        }
    }

    private val handler = Handler()
    private val connection = ClashAConnection(handler, true)
    override fun onServiceConnected(service: IClashAService) = changeState(
        try {
            BaseService.State.values()[service.state]
        } catch (_: DeadObjectException) {
            BaseService.State.Idle
        }
    )

    override fun onServiceDisconnected() = changeState(BaseService.State.Idle)
    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }

    fun isDebugInit() {
        if (BuildConfig.DEBUG) {
            Crashlytics.setUserName("CCG DEBUG")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDebugInit()
        setContentView(R.layout.layout_main)

        drawer = findViewById(R.id.drawer)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        navigation = findViewById(R.id.navigation)
        navigation.setNavigationItemSelectedListener(this)
        //temp
        toolbar.setNavigationIcon(R.drawable.ic_navigation_menu)
        toolbar.setNavigationOnClickListener { drawer.openDrawer(GravityCompat.START) }

        btnStartService = findViewById(R.id.btn_start_proxy)
        btnStopService = findViewById(R.id.btn_stop_proxy)
        btnDownloadConfig = findViewById(R.id.btn_download_config)
        btnImportConfig = findViewById(R.id.btn_import_config)

        btnStartService.setOnClickListener {
            toggle()
        }

        btnStopService.setOnClickListener {
            toggle()
        }

        btnDownloadConfig.setOnClickListener {
            DownloadConfigDialog().show()
        }

        btnImportConfig.setOnClickListener {
            importLocalConfig()
        }

        changeState(BaseService.State.Idle)   // reset everything to init state
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
    }

    private fun toggle() = when {
        state.canStop -> app.stopService()
        DataStore.serviceMode == Key.modeVpn -> {
            val intent = VpnService.prepare(this)
            if (intent != null) startActivityForResult(intent, REQUEST_CONNECT)
            else onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null)
        }
        else -> app.startService()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) drawer.closeDrawers() else {
            //todo
            when (item.itemId) {
                R.id.globalSettings -> println("todo")//displayFragment(GlobalSettingsFragment())
                R.id.testGo -> startActivity(Intent(this, TestActivity::class.java))
                else -> return false
            }
            item.isChecked = true

        }
        return true
    }

    /*private fun displayFragment(fragment: ToolbarFragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, fragment).commitAllowingStateLoss()
        drawer.closeDrawers()
    }*/

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onStop() {
        connection.bandwidthTimeout = 0
        super.onStop()
    }

    private inner class DownloadConfigDialog {
        val builder: AlertDialog.Builder
        val editText: EditText
        lateinit var dialog: AlertDialog

        init {
            val view = layoutInflater.inflate(R.layout.dialog_download_config, null)
            editText = view.findViewById(R.id.url_content)
            builder = AlertDialog.Builder(this@MainActivity)
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
                        mContext = this@MainActivity
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


        init {
            configName = "config-${TimeUtils.getNowMills()}.yml"

        }

        override fun onPreExecute() {
            super.onPreExecute()
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
                .addHeader("platform", "2")
                .addHeader("phoneModel", Build.MODEL)
                .addHeader("systemVersion", Build.VERSION.RELEASE)
                .url(url).build()

            try {
                return mClient.newCall(request).execute().body()?.string().toString()
            } catch (e: Exception) {
                Crashlytics.logException(e)
                Crashlytics.log(Log.ERROR, TAG, e.localizedMessage)
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            result?.let { it ->
                try {
                    var file = File(app.filesDir, configName)
                    file.writeText(it)
                    DataStore.tempConfigPath = file.absolutePath
                    var yaml = Yaml()
                    val loadAll = yaml.loadAll(it)
                    loadAll.forEach { value ->
                        loadPortFromConfig(value)
                    }
                } catch (e: Exception) {
                    ToastUtils.showShort(R.string.message_download_config_fail)
                    Crashlytics.logException(e)
                    Crashlytics.setString("Download debug result", result)
                    return
                }
            }
            val message = mContext?.getString(R.string.message_download_config_success) + ", " +
                    mContext?.getString(
                        R.string.message_read_config_port,
                        DataStore.portProxy.toString(),
                        DataStore.portController.toString()
                    )
            ToastUtils.showLong(
                message
            )
            Crashlytics.log(Log.INFO, TAG, message)
            progressDialog?.dismiss()
        }


    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        var notVPNItem = menu?.findItem(R.id.action_not_vpn)
        notVPNItem?.isChecked = DataStore.serviceMode == Key.modeProxy
//        menu?.add(1, 7, 1, "抛出异常")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {

            R.id.action_check_update -> {
                UpdateChecker.checkForDialog(
                    this,
                    "https://raw.githubusercontent.com/ccg2018/ClashA/master/update/update.json"
                )
                var result = ""
                DeviceUtils.getABIs().forEach {
                    LogUtils.i(it)
                    result += it
                }
            }

            R.id.action_dashboard -> {
                when (state) {
                    BaseService.State.Connected -> {
                        val dialog = ClashAWebviewBottomSheetDialog(this)
                        dialog.setTitle(R.string.title_dashboard)
                        dialog.setCanBack(false)
                        dialog.setShowBackNav(false)
                        dialog.loadUrl("file:///android_asset/yacd/index_0.35.html?port=${DataStore.portController}#/proxies")
                        dialog.show()
                        dialog.setMaxHeight(ScreenUtils.getScreenHeight())
                        dialog.setPeekHeight(ScreenUtils.getScreenHeight())
                    }
                    else -> ToastUtils.showShort(R.string.message_dashboard_please_startproxy)
                }
            }

            R.id.action_not_vpn -> {
                item?.isChecked = !item?.isChecked
                DataStore.serviceMode = if (item?.isChecked) Key.modeProxy else Key.modeVpn
            }
            7 -> {
                throw RuntimeException("This is a crash")
            }
        }
        return super.onOptionsItemSelected(item)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            requestCode == REQUEST_IMPORT -> {
                if (resultCode == Activity.RESULT_OK) {

                    val list = FilePickerManager.obtainData()
                    val path = list[0]
                    LogUtils.d("import config.yml -> $path")
                    if (!TextUtils.isEmpty(path)) {
                        val file = File(path!!)
                        if (file.exists()) {
                            val out = File(
                                app.filesDir,
                                "config-${TimeUtils.getNowMills()}.yml"
                            )//-${TimeUtils.getNowMills()}
                            try {
                                file.copyTo(out, true)
                                DataStore.tempConfigPath = out.absolutePath

                                var yaml = Yaml()
                                val loadAll = yaml.loadAll(FileInputStream(out))
                                loadAll.forEach() { value ->
                                    loadPortFromConfig(value)
                                }

                                ToastUtils.showLong(
                                    getString(R.string.message_import_config_success) + ", " + getString(
                                        R.string.message_read_config_port,
                                        DataStore.portProxy.toString(),
                                        DataStore.portController.toString()
                                    )
                                )

                            } catch (e: IOException) {
                                ToastUtils.showShort(R.string.message_import_config_fail)
                                Crashlytics.logException(e)
                                Crashlytics.log(Log.ERROR, TAG, e.localizedMessage)
                            } catch (e: java.lang.RuntimeException) {
                                //yml content format error
                                ToastUtils.showLong("${getString(R.string.message_import_config_fail)}, ${e.localizedMessage}")
                                Crashlytics.logException(e)
                                Crashlytics.log(Log.ERROR, TAG, e.localizedMessage)
                                DataStore.tempConfigPath = ""
                                out.delete()
                            }
                        } else {
                            ToastUtils.showShort(R.string.message_import_config_none)
                        }

                    } else {
                        ToastUtils.showShort(R.string.message_import_config_fail)
                    }
                } else {
                }
            }
            requestCode == REQUEST_CONNECT -> if (resultCode == Activity.RESULT_OK) {
                app.startService()
            } else {
                ToastUtils.showLong(R.string.vpn_permission_denied)
                LogUtils.w(TAG, "Failed to start VpnService from onActivityResult: $data")
                Crashlytics.log(Log.INFO, TAG, "Failed to start VpnService from onActivityResult: $data")
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
    }
}
