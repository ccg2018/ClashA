package com.github.cgg.clasha

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceDataStore
import com.avos.avoscloud.*
import com.blankj.utilcode.util.*
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.aidl.ClashAConnection
import com.github.cgg.clasha.aidl.IClashAService
import com.github.cgg.clasha.aidl.TrafficStats
import com.github.cgg.clasha.bg.BaseService
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.data.OnPreferenceDataStoreChangeListener
import com.github.cgg.clasha.utils.Key
import com.github.cgg.clasha.utils.Key.isONKey
import com.github.cgg.clasha.utils.isPortUsing
import com.github.cgg.clasha.widget.EditTextDialog
import com.github.cgg.clasha.widget.ServiceButton
import com.google.android.material.navigation.NavigationView
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import java.lang.Exception
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity(), ClashAConnection.Callback, OnPreferenceDataStoreChangeListener,
    NavigationView.OnNavigationItemSelectedListener {


    companion object {
        private const val TAG = "ClashAMainActivity"
        private const val REQUEST_CONNECT = 1


        var stateListener: ((BaseService.State) -> Unit)? = null
        fun pendingIntent(context: Context) = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), 0
        )

        var server: Server? = null
    }


    internal lateinit var drawer: DrawerLayout
    private lateinit var navigation: NavigationView
    private lateinit var fab: ServiceButton


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
        fab.changeState(state, false)
        //stats.changeState(state)
        //if (msg != null) snackbar(getString(R.string.vpn_error).format(Locale.ENGLISH, msg)).show()
        LogUtils.i(TAG, "ChangeState - > $state, msg - > $msg, animate - > $animate")
        if (msg != null) ToastUtils.showShort(msg)
        this.state = state
        changeButtonState(state)
        //ProfilesFragment.instance?.profilesAdapter?.notifyDataSetChanged()  // refresh button enabled state
        stateListener?.invoke(state)


    }

    private fun changeButtonState(state: BaseService.State) = when {
        state == BaseService.State.Idle || state == BaseService.State.Stopped -> {
        }
        state == BaseService.State.Connecting || state == BaseService.State.Connected -> {
        }
        state == BaseService.State.Stopping -> {
        }
        else -> {
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
        navigation = findViewById(R.id.navigation)
        navigation.setNavigationItemSelectedListener(this)
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            toggle()
        }

        if (savedInstanceState == null) {
            navigation.menu.findItem(R.id.profileConfig).isChecked = true
            FragmentUtils.replace(
                supportFragmentManager,
                ProfileListFragment(),
                R.id.fragment
            )
        }

        changeState(BaseService.State.Idle)   // reset everything to init state
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)

        initHttpServer()
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
            when (item.itemId) {
                R.id.profileConfig -> {
                    FragmentUtils.replace(
                        supportFragmentManager,
                        ProfileListFragment(),
                        R.id.fragment
                    )
                }
                R.id.globalSettings -> {
                    FragmentUtils.replace(
                        supportFragmentManager,
                        GlobalSettingsFragment(),
                        R.id.fragment
                    )
                }

                R.id.logsView -> {
                    val findFragment = FragmentUtils.findFragment(supportFragmentManager, LogsFragment::class.java)
                    LogUtils.i("find fragment: $findFragment")
                    FragmentUtils.replace(
                        supportFragmentManager,
                        LogsFragment(),
                        R.id.fragment
                    )
                }
                //R.id.testGo -> startActivity(Intent(this, TestActivity::class.java))
                else -> return false
            }
            item.isChecked = true
            drawer.closeDrawers()
        }
        return true
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawers() else {

            val top = FragmentUtils.getTop(supportFragmentManager)
            LogUtils.wTag(TAG, top)
            if (top !is ProfileListFragment) {
                FragmentUtils.replace(
                    supportFragmentManager,
                    ProfileListFragment(),
                    R.id.fragment
                )
                navigation.menu.findItem(R.id.profileConfig).isChecked = true
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500

        if (needQueryVersion()) {
            queryCurrentVersionIsOn()
        }

        if (BuildConfig.closeBeta) {
            if (!SPUtils.getInstance().contains("firstCheck")) {
                queryUseUserActive()
            } else {
                val isActive = DataStore.publicStore.getBoolean("isActive", false)
                if (!isActive) {
                    showActiveCodeInputDialog()
                }
            }
        }

    }

    //closeBeta
    /* active */


    //是否应该查询
    private fun needQueryVersion(): Boolean {
        val time = DataStore.publicStore.getLong(isONKey + "time", 0)
        if ((System.currentTimeMillis() - time) > 24 * 60 * 60L) {
            return true
        }
        return false
    }

    //查询当前版本是否过期
    private fun queryCurrentVersionIsOn() {
        var query = AVQuery<AVObject>("settings")
        query.whereEqualTo("Key", isONKey)
        query.findInBackground(object : FindCallback<AVObject>() {
            override fun done(avObjects: MutableList<AVObject>?, avException: AVException?) {
                avObjects?.takeIf { avObjects.isNotEmpty() }
                    ?.also {
                        val avObject = it[0]
                        val value = avObject.getString("value")
                        val on = value.toBoolean()
                        app.mAppExecutors.diskIO.execute {
                            val flag = DataStore.publicStore.getBoolean(isONKey, true)
                            DataStore.publicStore.putBoolean(isONKey, on)
                            //on才更新时间
                            if (flag) {
                                DataStore.publicStore.putLong(isONKey + "time", System.currentTimeMillis())
                            }

                        }
                        if (!on) {
                            ToastUtils.showLong("The verson outdated, please update")
                        }
                    }
            }
        })
    }

    private fun showActiveCodeInputDialog() {
        LogUtils.w("showActiveCodeInputDialog")
        var dialog = EditTextDialog.newInstance(
            title = "please input active code",
            isMultiline = false, hasOnNeutral = false
        )
        dialog.onOk = { editText, tag ->
            saveActivetoServer(editText.text.toString())
        }
        dialog.isCancelable = false
        dialog.show(supportFragmentManager, "")
    }

    private fun queryUseUserActive() {
        LogUtils.w("queryUseUserActive")
        var query = AVQuery<AVObject>("UseUser")
        query.whereEqualTo("AndroidId", DeviceUtils.getAndroidID())
        query.findInBackground(object : FindCallback<AVObject>() {
            override fun done(avObjects: MutableList<AVObject>?, avException: AVException?) {
                SPUtils.getInstance().put("firstCheck", false)
                if (avObjects == null || avObjects.size <= 0) {
                    DataStore.publicStore.putBoolean("isActive", false)
                    showActiveCodeInputDialog()
                } else {
                    DataStore.publicStore.putBoolean("isActive", true)
                }
            }
        })
    }


    private fun saveActivetoServer(activeCode: String) {
        LogUtils.w("saveActivetoServer")
        if (TextUtils.isEmpty(activeCode)) {
            ToastUtils.showLong("code 无效")
            return
        }
        var query = AVQuery<AVObject>("UseUser")
        query.whereEqualTo("useCode", activeCode)

        query.findInBackground(object : FindCallback<AVObject>() {
            override fun done(avObjects: MutableList<AVObject>?, avException: AVException?) {
                if (avObjects == null || avObjects.size <= 0) {
                    DataStore.publicStore.putBoolean("isActive", false)
                    ToastUtils.showLong("code 无效")
                } else {
                    val avObject = avObjects[0]
                    if (TextUtils.isEmpty(avObject.getString("AndroidId"))) {
                        DataStore.publicStore.putBoolean("isActive", true)
                        avObject.put("Model", DeviceUtils.getModel())
                        avObject.put("Manufacturer", DeviceUtils.getManufacturer())
                        avObject.put("AndroidId", DeviceUtils.getAndroidID())
                        avObject.saveInBackground()
                        ToastUtils.showLong("code 有效")
                    } else {
                        ToastUtils.showLong("code used")
                    }

                }
            }
        })

    }
    /* active */
    //end closeBeta

    override fun onStop() {
        connection.bandwidthTimeout = 0
        super.onStop()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
//        menu?.add(1, 7, 1, "抛出异常")
        return true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when {
            requestCode != REQUEST_CONNECT -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> app.startService()
            else -> {
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
        stopHttpServer()
    }


    private fun initHttpServer() {
        app.mAppExecutors.diskIO.execute {
            val host = "0.0.0.0"
            var port = 8881

            server = AndServer.serverBuilder(this)
                .inetAddress(InetAddress.getByName(host))
                .port(port)
                .timeout(5, TimeUnit.SECONDS)
                .listener(object : Server.ServerListener {
                    override fun onException(e: Exception?) {
                        LogUtils.iTag(TAG, e)
                    }

                    override fun onStarted() {
                        LogUtils.iTag(TAG, "HTTP server started")
                    }

                    override fun onStopped() {
                        LogUtils.iTag(TAG, "HTTP server Stopped")
                    }

                })
                .build()
            server?.startup()
        }
    }

    private fun stopHttpServer() {
        if (server?.isRunning == true) {
            server?.shutdown()
        }
    }

}
