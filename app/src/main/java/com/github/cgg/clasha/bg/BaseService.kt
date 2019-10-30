package com.github.cgg.clasha.bg

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.content.getSystemService
import com.blankj.utilcode.util.LogUtils
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.App.Companion.TAG
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.BuildConfig
import com.github.cgg.clasha.R
import com.github.cgg.clasha.aidl.IClashAService
import com.github.cgg.clasha.aidl.IClashAServiceCallback
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.data.LogMessage
import com.github.cgg.clasha.data.LogsDatabase
import com.github.cgg.clasha.utils.*
import com.github.cgg.clasha.utils.Key.isONKey
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.*
import java.net.BindException
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @Author: CCG
 * @Email:
 * @program: ClashA
 * @create: 2018-12-26
 * @describe
 */

/**
 * This object uses WeakMap to simulate the effects of multi-inheritance.
 */
object BaseService {

    enum class State(val canStop: Boolean = false) {
        /**
         * Idle state is only used by UI and will never be returned by BaseService.
         */
        Idle,
        Connecting(true),
        Connected(true),
        Stopping,
        Stopped,
    }

    const val tempformattedName = "Clash"

    private val instances = WeakHashMap<Interface, Data>()
    internal fun register(instance: Interface) = instances.put(instance, Data(instance))


    class Data internal constructor(private val service: Interface) {
        var state = State.Stopped
        var processes: GuardedProcessPool? = null
        //todo
        var proxy: ProxyInstance? = null
        //var udpFallback: ProxyInstance? = null

        var notification: ServiceNotification? = null


        var closeReceiverRegistered = false
        val closeReceiver = broadcastReceiver { _, intent ->
            when (intent.action) {
                Action.RELOAD -> service.forceLoad()
                else -> service.stopRunner()
            }
        }

        val binder = Binder(this)
        var connectingJob: Job? = null
        var updateJob: Job? = null
        var currentRunDate: Date? = null

        fun changeState(s: State, msg: String? = null) {
            if (state == s && msg == null) return
            binder.stateChanged(s, msg)
            state = s
        }
    }

    //todo 需要修改
    class Binder(private var data: Data? = null) : IClashAService.Stub(), AutoCloseable {
        val callbacks = object : RemoteCallbackList<IClashAServiceCallback>() {
            override fun onCallbackDied(callback: IClashAServiceCallback?, cookie: Any?) {
                super.onCallbackDied(callback, cookie)
                stopListeningForBandwidth(callback ?: return)
            }
        }
        private val bandwidthListeners = mutableMapOf<IBinder, Long>()  // the binder is the real identifier
        private val handler = Handler()

        override fun getState(): Int = data!!.state.ordinal
        override fun getProfileName(): String = data!!.proxy?.profile?.configName ?: "Idle"

        override fun registerCallback(cb: IClashAServiceCallback) {
            callbacks.register(cb)
        }

        private fun broadcast(work: (IClashAServiceCallback) -> Unit) {
            val count = callbacks.beginBroadcast()
            try {
                repeat(count) {
                    try {
                        work(callbacks.getBroadcastItem(it))
                    } catch (e: Exception) {
                        printLog(e)
                    }
                }
            } finally {
                callbacks.finishBroadcast()
            }
        }

        private fun registerTimeout() {
            handler.postDelayed(this::onTimeout, bandwidthListeners.values.min() ?: return)
        }

        private fun onTimeout() {
//        val proxies = listOfNotNull(data?.proxy, data?.udpFallback)
//        val stats = proxies
//            .map { Pair(it.profile.id, it.trafficMonitor?.requestUpdate()) }
//            .filter { it.second != null }
//            .map { Triple(it.first, it.second!!.first, it.second!!.second) }
//        if (stats.any { it.third } && data!!.state == State.Connected && bandwidthListeners.isNotEmpty()) {
//            val sum = stats.fold(TrafficStats()) { a, b -> a + b.second }
//            broadcast { item ->
//                if (bandwidthListeners.contains(item.asBinder())) {
//                    stats.forEach { (id, stats) -> item.trafficUpdated(id, stats) }
//                    item.trafficUpdated(0, sum)
//                }
//            }
//        }
//        registerTimeout()
        }

        override fun startListeningForBandwidth(cb: IClashAServiceCallback, timeout: Long) {
//        val wasEmpty = bandwidthListeners.isEmpty()
//        if (bandwidthListeners.put(cb.asBinder(), timeout) == null) {
//            if (wasEmpty) registerTimeout()
//            if (data!!.state != State.Connected) return
//            var sum = TrafficStats()
//            val proxy = data!!.proxy ?: return
//            proxy.trafficMonitor?.out.also { stats ->
//                cb.trafficUpdated(proxy.profile.id, if (stats == null) sum else {
//                    sum += stats
//                    stats
//                })
//            }
//            data!!.udpFallback?.also { udpFallback ->
//                udpFallback.trafficMonitor?.out.also { stats ->
//                    cb.trafficUpdated(udpFallback.profile.id, if (stats == null) TrafficStats() else {
//                        sum += stats
//                        stats
//                    })
//                }
//            }
//            cb.trafficUpdated(0, sum)
//        }
        }

        override fun stopListeningForBandwidth(cb: IClashAServiceCallback) {
            if (bandwidthListeners.remove(cb.asBinder()) != null && bandwidthListeners.isEmpty()) {
                handler.removeCallbacksAndMessages(null)
            }
        }

        override fun unregisterCallback(cb: IClashAServiceCallback) {
            stopListeningForBandwidth(cb)   // saves an RPC, and safer
            callbacks.unregister(cb)
        }

        fun stateChanged(s: State, msg: String?) {
            val profileName = profileName
            broadcast { it.stateChanged(s.ordinal, profileName, msg) }
        }

        fun trafficPersisted(ids: List<Long>) {
            if (bandwidthListeners.isNotEmpty() && ids.isNotEmpty()) broadcast { item ->
                if (bandwidthListeners.contains(item.asBinder())) ids.forEach(item::trafficPersisted)
            }
        }

        override fun close() {
            callbacks.kill()
            handler.removeCallbacksAndMessages(null)
            data = null
        }
    }

    interface Interface {
        val tag: String
        val data: Data

        fun onBind(intent: Intent): IBinder? = if (intent.action == Action.SERVICE) data.binder else null

        fun createNotification(profileName: String): ServiceNotification


        fun stopRunner(restart: Boolean = false, msg: String? = null) {
            if (data.state == State.Stopping) return
            data.changeState(State.Stopping)
            GlobalScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED) {
                data.connectingJob?.cancelAndJoin() // ensure stop connecting first
                data.updateJob?.cancelAndJoin()
                this@Interface as Service
                // we use a coroutineScope here to allow clean-up in parallel
                coroutineScope {
                    killProcesses(this)
                    // clean up receivers
                    val data = data
                    if (data.closeReceiverRegistered) {
                        unregisterReceiver(data.closeReceiver)
                        data.closeReceiverRegistered = false
                    }

                    data.notification?.destroy()
                    data.notification = null

                    val ids = listOfNotNull(data.proxy).map {
                        it.shutdown(this)
                        1L
                        //todo config
                        //it.profile.id
                    }
                    data.proxy = null
                    data.binder.trafficPersisted(ids)
                }

                // change the state
                data.changeState(State.Stopped, msg)

                // stop the service if nothing has bound to it
                if (restart) startRunner() else stopSelf()
            }
        }

        fun forceLoad() {
            //todo 强制加载新的配置 要判断有没有配置
            val s = data.state
            when {
                s == State.Stopped -> startRunner()
                s.canStop -> stopRunner(true)
                else -> Crashlytics.log(Log.WARN, tag, "Illegal state when invoking use: $s")
            }
        }

        fun startRunner() {
            this as Context
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(Intent(this, javaClass))
            else startService(Intent(this, javaClass))
        }

        fun buildAdditionalArguments(cmd: ArrayList<String>): ArrayList<String> = cmd

        suspend fun preInit() {}
        suspend fun resolver(host: String) = InetAddress.getAllByName(host)
        suspend fun openConnection(url: URL) = url.openConnection()


        suspend fun startProcesses() {

            val configRoot = (if (Build.VERSION.SDK_INT < 24 || app.getSystemService<UserManager>()
                    ?.isUserUnlocked != false
            ) app else app.deviceStorage).noBackupFilesDir
            data.updateJob?.start()
            LogUtils.w("configRoot ： ${configRoot.absolutePath}")
            data.proxy!!.start(
                this,
                File(app.deviceStorage.noBackupFilesDir, "stat_main"),
                File(configRoot, "config.yml"),
                null
            )

        }

        class DelayRetryInterceptor(var maxTryCount: Int = 0, var tryInterval: Long = 0) : Interceptor {


            override fun intercept(chain: Interceptor.Chain): Response? {
                val request = chain.request()
                var response = doRequest(chain, request)

                var retryCount = 0
                while ((response == null || !response.isSuccessful) && retryCount <= maxTryCount) {
                    try {
                        Thread.sleep(tryInterval)
                    } catch (e: Exception) {
                        Thread.currentThread().interrupt()
                        throw InterruptedIOException()
                    }
                    retryCount++
                    response = doRequest(chain, request)
                }

                return response
            }

            private fun doRequest(chain: Interceptor.Chain, request: Request): Response? {
                return try {
                    chain.proceed(request)
                } catch (e: Exception) {
                    null
                }
            }

        }

        suspend fun startUpdateLogs() {
            try {
                var tryInterceptor = DelayRetryInterceptor(22, 70)
                val mOkHttpClient = OkHttpClient.Builder()
                    .addInterceptor(tryInterceptor)
                    .connectTimeout(24, TimeUnit.HOURS)
                    .readTimeout(24, TimeUnit.HOURS)
                    .retryOnConnectionFailure(true)
                    .build()
                val host = "http://127.0.0.1:${DataStore.portApi}"
                val debug = DataStore.clashLoglevel
                LogUtils.i("launch-${Thread.currentThread().name}")
                val req = Request.Builder().url("$host/logs?level=$debug").get().build()
                val call = mOkHttpClient.newCall(req)
                val response = call.execute()
                printReader2(response.body()?.charStream())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                data.updateJob = null
            }
        }

        fun printReader2(reader: Reader?) {
            val br = BufferedReader(reader)
            try {
                var contentLine: String?
                do {
                    contentLine = br.readLine()
                    LogUtils.eTag(TAG, "okhttp: $contentLine")
                    if (isJSON(contentLine)) {
                        val jsonObject = JSONObject(contentLine)
                        val payload = jsonObject.optString("payload")
                        val type = jsonObject.optString("type")
                        // DNS or rule log
                        var log = LogMessage(
                            time = System.currentTimeMillis(),
                            currentRunDate = data.currentRunDate!!,
                            profileId = app.currentProfileConfig?.id ?: 0
                        )
                        log.originContent = payload
                        if ("debug" == type && payload.contains("DNS")) {
                            log.logType = Key.LOG_TYPE_DNS
                        } else if ("info" == type && payload.contains("match") && payload.contains("using")) {
                            log.logType = Key.LOG_TYPE_RULE
                        } else {
                            log.logType = Key.LOG_TYPE_OTHER
                        }
                        log.content = payload
                        LogsDatabase.logMessageDao.create(log)

                    }

                } while (contentLine != null)

            } catch (e: IOException) {
            }
        }

        /*----------*/
        fun isJSON(content: String?): Boolean {
            return try {
                JSONObject(content)
                true
            } catch (e: Exception) {
                false
            }
        }
        /*----------*/

        fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val data = data
            if (data.state != State.Stopped) return Service.START_NOT_STICKY

            this as Context

            //TrafficMonitor.reset()
//            val thread = TrafficMonitorThread()
//            thread.start()
            //data.trafficMonitorThread = thread

            //closeBeta
            /* active */
            if (BuildConfig.closeBeta && DataStore.publicStore.getBoolean("isActive") == false) {
                stopRunner(false, "not active")
                return Service.START_NOT_STICKY
            }
            val flag = DataStore.publicStore.getBoolean(isONKey, true)
            val time = DataStore.publicStore.getLong(isONKey + "time", 0)
            if (!flag && ((System.currentTimeMillis() - time) > 24 * 60 * 60)) {//
                stopRunner(false, "verison outdated")
                return Service.START_NOT_STICKY
            }
            /* active */
            //end closeBeta

            //判断有没有配置文件
            val profile = app.currentProfileConfig
            if (profile == null) {
                data.notification = createNotification("")
                stopRunner(false, getString(R.string.profile_empty))
                return Service.START_NOT_STICKY
            }

            var proxy = ProxyInstance(profile)
            data.proxy = proxy

//            if (DataStore.tempConfigPath == "") {
//                data.changeState(State.Stopped, "Config.xml is none, please download or import")
//                return Service.START_NOT_STICKY
//            }
//
//            val file = File(DataStore.tempConfigPath)
//            if (!file.exists()) {
//                data.changeState(State.Stopped, "Config.xml is none, please download or import")
//                return Service.START_NOT_STICKY
//            }
//            file.copyTo(File(app.filesDir, "config.yml"), true)


            if (!data.closeReceiverRegistered) {
                // register close receiver
                registerReceiver(data.closeReceiver, IntentFilter().apply {
                    addAction(Action.RELOAD)
                    addAction(Intent.ACTION_SHUTDOWN)
                    addAction(Action.CLOSE)
                })
                data.closeReceiverRegistered = true
            }
            data.notification = createNotification(tempformattedName)


            data.changeState(State.Connecting)
            data.currentRunDate = Date()
            data.updateJob = GlobalScope.launch(context = Dispatchers.IO, start = CoroutineStart.LAZY) {
                startUpdateLogs()
            }
            data.connectingJob = GlobalScope.launch(Dispatchers.Main) {
                try {
                    Executable.killAll()   // clean up old processes
                    preInit()
                    proxy.init(this@Interface)
                    //data.udpFallback?.init(this@Interface)

                    data.processes = GuardedProcessPool {
                        printLog(it)
                        stopRunner(false, it.readableMessage)
                    }
                    startProcesses()

                    //proxy.scheduleUpdate()
                    //data.udpFallback?.scheduleUpdate()

                    data.changeState(State.Connected)
                } catch (_: CancellationException) {
                    // if the job was cancelled, it is canceller's responsibility to call stopRunner
                } catch (_: UnknownHostException) {
                    stopRunner(false, getString(R.string.invalid_server))
                } catch (exc: Throwable) {
                    //if (exc !is PluginManager.PluginNotFoundException &&
                    if (exc !is BindException &&
                        exc !is VpnService.NullConnectionException
                    ) {
                        printLog(exc)
                    }
                    stopRunner(false, "${getString(R.string.service_failed)}: ${exc.readableMessage}")
                } finally {
                    data.updateJob = null
                    data.connectingJob = null
                }
            }

            return Service.START_NOT_STICKY
        }

        fun killProcesses(scope: CoroutineScope) {
            data.processes?.run {
                close(scope)
                data.processes = null
            }
        }

    }
}