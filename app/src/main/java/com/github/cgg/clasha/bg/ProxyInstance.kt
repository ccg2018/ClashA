package com.github.cgg.clasha.bg

import android.content.Context
import android.content.Intent
import android.util.Log
import com.blankj.utilcode.util.LogUtils
import com.crashlytics.android.Crashlytics
import com.github.ccg.clasha.bg.TrafficMonitor
import com.github.cgg.clasha.App
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.data.ProfileConfig
import com.github.cgg.clasha.utils.getGson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-01
 * @describe
 */

class ProxyInstance(val profile: ProfileConfig) {
    private var configFile: File? = null
    var trafficMonitor: TrafficMonitor? = null
    suspend fun init(service: BaseService.Interface) {


        //todo 这里配置DNS为空 解析系统的DNS吧
        // it's hard to resolve DNS on a specific interface so we'll do it here
        /*if (profile.host.parseNumericAddress() == null) {
            var retries = 0
            while (true) try {
                val io = GlobalScope.async(Dispatchers.IO) { service.resolver(profile.host) }
                profile.host = io.await().firstOrNull()?.hostAddress ?: throw UnknownHostException()
                return
            } catch (e: UnknownHostException) {
                // retries are only needed on Chrome OS where arc0 is brought up/down during VPN changes
                if (!DataStore.hasArc0) throw e
                Thread.yield()
                Crashlytics.log(Log.WARN, "ProxyInstance-resolver", "Retry resolving attempt #${++retries}")
            }
        }*/


    }

    fun start(service: BaseService.Interface, stat: File, configFile: File, extraFlag: String? = null) {
        trafficMonitor = TrafficMonitor(stat)

        this.configFile = configFile
        val config = profile.toJson()
        val yaml = Yaml()
        configFile.writeText(yaml.dump(yaml.load(getGson().toJson(config))))
        LogUtils.i(App.TAG, "Clash 配置文件准备完毕")
        5450
        val cmd = service.buildAdditionalArguments(
            arrayListOf(
                File((service as Context).applicationInfo.nativeLibraryDir, Executable.CLASH).absolutePath,
                "-d",
                configFile?.parentFile.absolutePath
            )
        )

        service.data.processes!!.start(cmd)
    }

    fun shutdown(scope: CoroutineScope) {
        trafficMonitor?.apply {
            thread.shutdown(scope)
            // Make sure update total traffic when stopping the runner
            try {
                // profile may have host, etc. modified and thus a re-fetch is necessary (possible race condition)
//                val profile = ProfileManager.getProfile(profile.id) ?: return
//                profile.tx += current.txTotal
//                profile.rx += current.rxTotal
//                ProfileManager.updateProfile(profile)
            } catch (e: IOException) {
//                if (!DataStore.directBootAware) throw e // we should only reach here because we're in direct boot
//                val profile = DirectBoot.getDeviceProfile()!!.toList().filterNotNull().single { it.id == profile.id }
//                profile.tx += current.txTotal
//                profile.rx += current.rxTotal
//                profile.dirty = true
//                DirectBoot.update(profile)
//                DirectBoot.listenForUnlock()
            }
        }
        trafficMonitor = null
//        configFile?.delete()    // remove old config possibly in device storage
        configFile = null
    }

}