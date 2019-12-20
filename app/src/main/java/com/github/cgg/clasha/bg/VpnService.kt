package com.github.cgg.clasha.bg

import android.app.Service
import android.content.Intent
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.Network
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import com.blankj.utilcode.util.LogUtils
import com.crashlytics.android.Crashlytics
import com.github.ccg.clasha.net.DefaultNetworkListener
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.MainActivity
import com.github.cgg.clasha.R
import com.github.cgg.clasha.VpnRequestActivity
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.net.ConcurrentLocalSocketListener
import com.github.cgg.clasha.utils.Key
import com.github.cgg.clasha.utils.printLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.lang.reflect.Method
import java.net.URL
import android.net.VpnService as BaseVpnService

/**
 * @Author: CCG
 * @Email:
 * @program: ClashA
 * @create: 2018-12-26
 * @describe
 */

class VpnService : BaseVpnService(), LocalDnsService.Interface {
    companion object {
        private const val VPN_MTU = 1500
        private const val PRIVATE_VLAN4_CLIENT = "172.19.0.1"
        private const val PRIVATE_VLAN4_ROUTER = "172.19.0.2"//DNS
        private const val PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1"
        private const val PRIVATE_VLAN6_ROUTER = "fdfe:dcba:9876::2"

        /**
         * https://android.googlesource.com/platform/prebuilts/runtime/+/94fec32/appcompat/hiddenapi-light-greylist.txt#9466
         */
        private val getInt: Method = FileDescriptor::class.java.getDeclaredMethod("getInt$")
    }


    class CloseableFd(val fd: FileDescriptor) : Closeable {
        override fun close() = Os.close(fd)
    }

    inner class NullConnectionException : NullPointerException() {
        override fun getLocalizedMessage() = getString(R.string.reboot_required)
    }

    private inner class ProtectWorker : ConcurrentLocalSocketListener(
        "ClashVpnThread",
        File(app.deviceStorage.noBackupFilesDir, "protect_path")
    ) {
        override fun acceptInternal(socket: LocalSocket) {
            socket.inputStream.read()
            val fd = socket.ancillaryFileDescriptors!!.single()!!
            CloseableFd(fd).use {
                socket.outputStream.write(if (underlyingNetwork.let { network ->
                        if (network != null && Build.VERSION.SDK_INT >= 23) try {
                            network.bindSocket(fd)
                            true
                        } catch (e: IOException) {
                            // suppress ENONET (Machine is not on the network)
                            if ((e.cause as? ErrnoException)?.errno != 64) printLog(e)
                            false
                        } else protect(getInt.invoke(fd) as Int)
                    }) 0 else 1)
            }
        }
    }

    override val data = BaseService.Data(this)
    override val tag: String get() = "ClashAVpnService"
    override fun createNotification(profileName: String): ServiceNotification =
        ServiceNotification(this, profileName, "service-vpn")

    private var conn: ParcelFileDescriptor? = null
    private var worker: ProtectWorker? = null
    private var active = false
    private var metered = false
    private var underlyingNetwork: Network? = null
        set(value) {
            field = value
            if (active && Build.VERSION.SDK_INT >= 22) setUnderlyingNetworks(underlyingNetworks)
        }
    private val underlyingNetworks
        get() =
            // clearing underlyingNetworks makes Android 9 consider the network to be metered
            if (Build.VERSION.SDK_INT == 28 && metered) null else underlyingNetwork?.let { arrayOf(it) }

    override fun onBind(intent: Intent) = when (intent.action) {
        SERVICE_INTERFACE -> super<BaseVpnService>.onBind(intent)
        else -> {
            super<LocalDnsService.Interface>.onBind(intent)
        }
    }

    override fun onRevoke() = stopRunner(true)


    override fun killProcesses(scope: CoroutineScope) {
        super.killProcesses(scope)
        active = false
        scope.launch { DefaultNetworkListener.stop(this) }
        worker?.shutdown(scope)
        worker = null
        conn?.close()
        conn = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DataStore.serviceMode == Key.modeVpn)
            if (BaseVpnService.prepare(this) != null)
                startActivity(
                    Intent(this, VpnRequestActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            else return super<LocalDnsService.Interface>.onStartCommand(intent, flags, startId)
        stopRunner(true)
        return Service.START_NOT_STICKY
    }

    override suspend fun preInit() = DefaultNetworkListener.start(this) { underlyingNetwork = it }
    override suspend fun resolver(host: String) = DefaultNetworkListener.get().getAllByName(host)
    override suspend fun openConnection(url: URL) = DefaultNetworkListener.get().openConnection(url)

    override suspend fun startProcesses() {
        worker = ProtectWorker().apply { start() }
        super.startProcesses()
        sendFd(startVpn())
        data.updateJob?.start()
    }


    private suspend fun startVpn(): FileDescriptor {
        val builder = Builder()
            .addAddress()
            .setConfigureIntent(MainActivity.pendingIntent(this))
            .setMtu(VPN_MTU)
            .setBlocking(false)
            .addBypassApplications()
            .addBypassPrivateRoute()
            .addDnsServer(PRIVATE_VLAN4_ROUTER)

        if (DataStore.ipv6Enable) {
            builder.addAddress(PRIVATE_VLAN6_CLIENT, 126)
            builder.addRoute("::", 0)
        }

        metered = DataStore.metered
        active = true
        if (Build.VERSION.SDK_INT >= 22) {
            builder.setUnderlyingNetworks(underlyingNetworks)
            if (Build.VERSION.SDK_INT >= 29) builder.setMetered(metered)
        }

        val conn = builder.establish() ?: throw NullConnectionException()
        this.conn = conn
        val fd = conn.fd
        LogUtils.i("#### fd.toString -> $fd")

        val cmd = arrayListOf(
            File(applicationInfo.nativeLibraryDir, Executable.TUN2SOCKS).absolutePath,
            "--netif-ipaddr", PRIVATE_VLAN4_ROUTER,
            "--socks-server-addr", "127.0.0.1:${DataStore.portProxy}",
            "--tunmtu", VPN_MTU.toString(),
            "--sock-path", "sock_path",
            "--dnsgw", "127.0.0.1:${DataStore.portLocalDns}",
            "--loglevel", "warning"
        )
        if (DataStore.ipv6Enable) {
            cmd += "--netif-ip6addr"
            cmd += PRIVATE_VLAN6_ROUTER
        }
        cmd += "--enable-udprelay"

        data.processes!!.start(cmd, onRestartCallback = {
            try {
                sendFd(conn.fileDescriptor)
            } catch (e: ErrnoException) {
                stopRunner(false, e.message)
            }
        })
        return conn.fileDescriptor
    }


    private suspend fun sendFd(fd: FileDescriptor) {
        var tries = 0
        val path = File(app.deviceStorage.noBackupFilesDir, "sock_path").absolutePath
        while (true) try {
            delay(50L shl tries)
            LocalSocket().use { localSocket ->
                localSocket.connect(LocalSocketAddress(path, LocalSocketAddress.Namespace.FILESYSTEM))
                localSocket.setFileDescriptorsForSend(arrayOf(fd))
                localSocket.outputStream.write(42)
            }
            return
        } catch (e: IOException) {
            if (tries > 5) throw e
            tries += 1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        data.binder.close()
    }

    private fun Builder.addBypassPrivateRoute(): Builder {
        runCatching {
            // IPv4
            if (DataStore.isBypassPrivateNetwork) {
                resources.getStringArray(R.array.bypass_private_route).forEach {
                    val address = it.split("/")
                    addRoute(address[0], address[1].toInt())
                }
            } else {
                addRoute("0.0.0.0", 0)
            }

            // IPv6
            if (DataStore.ipv6Enable) {
                addRoute("::", 0)
            }
        }.onFailure {
            Crashlytics.logException(it)
        }

        return this
    }

    private fun Builder.addAddress(): Builder {
        runCatching {
            addAddress(PRIVATE_VLAN4_CLIENT, 30)
            if (DataStore.ipv6Enable) {
                addAddress(PRIVATE_VLAN6_CLIENT, 126)
            }
        }.onFailure {
            Crashlytics.logException(it)
        }
        return this
    }

    private fun Builder.addBypassApplications(): Builder {
        for (app in resources.getStringArray(R.array.default_disallow_application)) {
            runCatching {
                addDisallowedApplication(app)
            }
        }
        return this
    }
}
