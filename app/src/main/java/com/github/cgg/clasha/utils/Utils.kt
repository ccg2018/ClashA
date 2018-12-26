package com.github.cgg.clasha.utils

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.system.Os
import android.system.OsConstants
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.App
import com.github.cgg.clasha.JniHelper
import com.github.cgg.clasha.MainActivity
import com.github.cgg.clasha.data.DataStore
import java.net.InetAddress
import java.net.URLConnection
import java.util.*


val Throwable.readableMessage get() = localizedMessage ?: javaClass.name

private val parseNumericAddress by lazy {
    InetAddress::class.java.getDeclaredMethod("parseNumericAddress", String::class.java).apply {
        isAccessible = true
    }
}

/**
 * A slightly more performant variant of InetAddress.parseNumericAddress.
 *
 * Bug: https://issuetracker.google.com/issues/123456213
 */
fun String?.parseNumericAddress(): InetAddress? = Os.inet_pton(OsConstants.AF_INET, this)
    ?: Os.inet_pton(OsConstants.AF_INET6, this)?.let { parseNumericAddress.invoke(null, this) as InetAddress }

fun parsePort(str: String?, default: Int, min: Int = 1025): Int {
    val value = str?.toIntOrNull() ?: default
    return if (value < min || value > 65535) default else value
}

fun broadcastReceiver(callback: (Context, Intent) -> Unit): BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = callback(context, intent)
}

/**
 * Wrapper for kotlin.concurrent.thread that tracks uncaught exceptions.
 */
fun thread(
    name: String? = null, start: Boolean = true, isDaemon: Boolean = false,
    contextClassLoader: ClassLoader? = null, priority: Int = -1, block: () -> Unit
): Thread {
    val thread = kotlin.concurrent.thread(false, isDaemon, contextClassLoader, name, priority, block)
    thread.setUncaughtExceptionHandler { _, t -> printLog(t) }
    if (start) thread.start()
    return thread
}

val URLConnection.responseLength: Long
    get() = if (Build.VERSION.SDK_INT >= 24) contentLengthLong else contentLength.toLong()

fun ContentResolver.openBitmap(uri: Uri) =
    if (Build.VERSION.SDK_INT >= 28) ImageDecoder.decodeBitmap(ImageDecoder.createSource(this, uri))
    else BitmapFactory.decodeStream(openInputStream(uri))

val PackageInfo.signaturesCompat
    get() =
        if (Build.VERSION.SDK_INT >= 28) signingInfo.apkContentsSigners else @Suppress("DEPRECATION") signatures

/**
 * Based on: https://stackoverflow.com/a/26348729/2245107
 */
fun Resources.Theme.resolveResourceId(@AttrRes resId: Int): Int {
    val typedValue = TypedValue()
    if (!resolveAttribute(resId, typedValue, true)) throw Resources.NotFoundException()
    return typedValue.resourceId
}

val Intent.datas get() = listOfNotNull(data) + (clipData?.asIterable()?.mapNotNull { it.uri } ?: emptyList())

fun printLog(t: Throwable) {
    t.printStackTrace()
}

fun loadPortFromConfig(value: Any?) {
    try {
        value as Map<String, Objects>

        if (value.containsKey("socks-port")) {
            val port = value.get("socks-port") as Int
            DataStore.portProxy = port
        }

        if (value.containsKey("external-controller")) {
            val hostAndPort = value.get("external-controller") as String
            val port = hostAndPort.split(":")[1]
            DataStore.portController = port.toInt()
        }

        if (value.containsKey("port")) {
            val i = value.get("port") as Int
            DataStore.portHttpProxy = i
        }


    } catch (e: Exception) {
        //xiaomi xposed crash
        Crashlytics.logException(e)
        Crashlytics.log(Log.ERROR, App.TAG, e.localizedMessage)
    }
}