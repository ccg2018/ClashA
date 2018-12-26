package com.github.cgg.clasha.bg

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.blankj.utilcode.util.LogUtils
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.App.Companion.TAG
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.R
import com.github.cgg.clasha.aidl.IClashAService
import com.github.cgg.clasha.aidl.IClashAServiceCallback
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.utils.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.BindException
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import javax.sql.DataSource

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
        override fun getProfileName(): String = "ClashA Temp"//data!!.proxy?.profile?.name ?: "Idle"

        override fun registerCallback(cb: IClashAServiceCallback) {
            callbacks.register(cb)
        }

        private fun broadcast(work: (IClashAServiceCallback) -> Unit) {
            repeat(callbacks.beginBroadcast()) {
                try {
                    work(callbacks.getBroadcastItem(it))
                } catch (e: Exception) {
                    printLog(e)
                }
            }
            callbacks.finishBroadcast()
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

//            val configRoot = (if (Build.VERSION.SDK_INT < 24 || app.getSystemService<UserManager>()
//                    ?.isUserUnlocked != false) app else app.deviceStorage).noBackupFilesDir

            data.proxy!!.start(this, File(app.deviceStorage.noBackupFilesDir, "stat_main"), File(""), null)

        }

        fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val data = data
            if (data.state != State.Stopped) return Service.START_NOT_STICKY

            this as Context

            //TrafficMonitor.reset()
//            val thread = TrafficMonitorThread()
//            thread.start()
            //data.trafficMonitorThread = thread

            var proxy = ProxyInstance()
            data.proxy = proxy

            //判断有没有配置文件
            if (DataStore.tempConfigPath == "") {
                data.changeState(State.Stopped, "Config.xml is none, please download or import")
                return Service.START_NOT_STICKY
            }

            val file = File(DataStore.tempConfigPath)
            if (!file.exists()) {
                data.changeState(State.Stopped, "Config.xml is none, please download or import")
                return Service.START_NOT_STICKY
            }

            file.copyTo(File(app.filesDir, "config.yml"), true)


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
                    data.connectingJob = null
                }
            }
            /*thread("$tag-Connecting") {
                try {
                    startProcesses()
                    data.changeState(State.Connected)
                } catch (_: UnknownHostException) {
                    stopRunner(true, getString(R.string.invalid_server))
                } catch (_: VpnService.NullConnectionException) {
                    stopRunner(true, getString(R.string.reboot_required))
                } catch (exc: Throwable) {
                    printLog(exc)
                    stopRunner(true, "${getString(R.string.service_failed)}: ${exc.message}")
                }
            }*/
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