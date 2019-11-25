package com.github.cgg.clasha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.WorkManager
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.LogUtils
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.aidl.ClashAConnection
import com.github.cgg.clasha.data.ConfigManager
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.data.ProfileConfig
import com.github.cgg.clasha.utils.Action
import com.github.cgg.clasha.utils.AppExecutors
import com.github.cgg.clasha.utils.DeviceStorageApp
import io.fabric.sdk.android.Fabric
import java.io.File

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2018-12-25
 * @describe
 */

class App : Application() {
    companion object {
        lateinit var app: App
        internal const val TAG = "ClashAndroidApplication"
    }

    val handler by lazy { Handler(Looper.getMainLooper()) }
    val mAppExecutors by lazy { AppExecutors() }

    //加密data分区 用于支持
    val deviceStorage by lazy { if (Build.VERSION.SDK_INT < 24) this else DeviceStorageApp(this) }
    val directBootSupported by lazy {
        Build.VERSION.SDK_INT >= 24 && getSystemService<DevicePolicyManager>()?.storageEncryptionStatus ==
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
    }

    val currentProfileConfig: ProfileConfig?
        get() {
            return ConfigManager.getProfileConfig(DataStore.profileId) ?: return null
        }


    fun getPackageInfo(packageName: String) = packageManager.getPackageInfo(
        packageName,
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
    )!!


    fun startService() = ContextCompat.startForegroundService(app, Intent(app, ClashAConnection.serviceClass))

    fun reloadService() = sendBroadcast(Intent(Action.RELOAD))
    fun stopService() = sendBroadcast(Intent(Action.CLOSE))

    override fun onCreate() {
        super.onCreate()
        app = this
        Fabric.with(deviceStorage, Crashlytics())

        initClash()
        initLog()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        WorkManager.initialize(deviceStorage, androidx.work.Configuration.Builder().build())

        updateNotificationChannels()
    }

    private fun initClash() {
        var isNeedCopy = false
        var file = File(app.filesDir, "Country.mmdb")
        if (file.exists()) {
            if (file.length() <= 3741143) isNeedCopy = true
        } else {
            isNeedCopy = true
        }

        if (isNeedCopy) {
            mAppExecutors.diskIO.execute {
                val inputStream = assets.open("Country.mmdb")
                FileIOUtils.writeFileFromIS(file, inputStream)
            }
        }

    }

    // init it in ur application
    private fun initLog() {
        val config = LogUtils.getConfig()
            .setLogSwitch(BuildConfig.DEBUG)// 设置 log 总开关，包括输出到控制台和文件，默认开
            .setConsoleSwitch(BuildConfig.DEBUG)// 设置是否输出到控制台开关，默认开
            .setGlobalTag("ClashA")// 设置 log 全局标签，默认为空
            // 当全局标签不为空时，我们输出的 log 全部为该 tag，
            // 为空时，如果传入的 tag 为空那就显示类名，否则显示 tag
            .setLogHeadSwitch(true)// 设置 log 头信息开关，默认为开
            .setLog2FileSwitch(true)// 打印 log 时是否存到文件的开关，默认关
            .setDir("")// 当自定义路径为空时，写入应用的/cache/log/目录中
            .setFilePrefix("ClashA")// 当文件前缀为空时，默认为"util"，即写入文件为"util-yyyy-MM-dd.txt"
            .setBorderSwitch(false)// 输出日志是否带边框开关，默认开
            .setSingleTagSwitch(false)// 一条日志仅输出一条，默认开，为美化 AS 3.1 的 Logcat
            .setConsoleFilter(LogUtils.V)// log 的控制台过滤器，和 logcat 过滤器同理，默认 Verbose
            .setFileFilter(LogUtils.V)// log 文件过滤器，和 logcat 过滤器同理，默认 Verbose
            .setStackDeep(3)// log 栈深度，默认为 1
            .setStackOffset(0)// 设置栈偏移，比如二次封装的话就需要设置，默认为 0
            .setSaveDays(3)// 设置日志可保留天数，默认为 -1 表示无限时长
            .addFormatter(object : LogUtils.IFormatter<List<*>>() {
                override fun format(list: List<*>?): String {
                    return "LogUtils Formatter ArrayList { " + list.toString() + " }"
                }
            })
        LogUtils.d(config.toString())
    }

    private fun updateNotificationChannels() {
        if (Build.VERSION.SDK_INT >= 26) @RequiresApi(26) {
            val nm = getSystemService<NotificationManager>()!!
            nm.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        "service-vpn", getText(R.string.service_vpn),
                        NotificationManager.IMPORTANCE_LOW
                    ),
                    NotificationChannel(
                        "service-proxy", getText(R.string.service_proxy),
                        NotificationManager.IMPORTANCE_LOW
                    ),
                    NotificationChannel(
                        "service-transproxy", getText(R.string.service_transproxy),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            )
            nm.deleteNotificationChannel("service-nat") // NAT mode is gone for good
        }
    }
}