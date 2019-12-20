/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.cgg.clasha.data

import android.os.Binder
import androidx.appcompat.app.AppCompatDelegate
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.utils.Key
import com.github.cgg.clasha.utils.parsePort
import com.github.shadowsocks.database.PublicDatabase
import org.json.JSONObject

object DataStore {
    val publicStore = RoomPreferenceDataStore(PublicDatabase.kvPairDao)
    // privateStore will only be used as temp storage for ProfileListFragment
    //val privateStore = RoomPreferenceDataStore(PrivateDatabase.kvPairDao)

    // hopefully hashCode = mHandle doesn't change, currently this is true from KitKat to Nougat
    private val userIndex by lazy { Binder.getCallingUserHandle().hashCode() }

    private fun getLocalPort(key: String, default: Int): Int {
        val value = publicStore.getInt(key)
        return if (value != null) {
            publicStore.putString(key, value.toString())
            value
        } else parsePort(publicStore.getString(key), default + userIndex)
    }

    var profileId: Long
        get() = publicStore.getLong(Key.id) ?: 0
        set(value) {
            publicStore.putLong(Key.id, value)
            // if (DataStore.directBootAware) DirectBoot.update()
        }
    val canToggleLocked: Boolean get() = publicStore.getBoolean(Key.directBootAware) == true
    val directBootAware: Boolean get() = app.directBootSupported && canToggleLocked
    //val tcpFastOpen: Boolean get() = TcpFastOpen.sendEnabled && DataStore.publicStore.getBoolean(Key.tfo, true)
    @AppCompatDelegate.NightMode
    val nightMode
        get() = when (publicStore.getString(Key.nightMode)) {
            Key.nightModeAuto -> AppCompatDelegate.MODE_NIGHT_AUTO
            Key.nightModeOff -> AppCompatDelegate.MODE_NIGHT_NO
            Key.nightModeOn -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    val listenAddress get() = if (publicStore.getBoolean(Key.shareOverLan, false)) "0.0.0.0" else "127.0.0.1"
    var serviceMode: String
        get() = publicStore.getString(Key.serviceMode) ?: Key.modeVpn
        set(value) = publicStore.putString(Key.serviceMode, value)

    var dnsMode: String
        get() = publicStore.getString(Key.dnsMode) ?: Key.modeFakeip
        set(value) = publicStore.putString(Key.dnsMode, value)

    var clashMode: String
        get() = publicStore.getString(Key.clashMode, Key.clashModeRule)!!
        set(value) = publicStore.putString(Key.clashMode, value)
    var clashLoglevel: String
        get() = publicStore.getString(Key.clashLoglevel, Key.clashLogInfo)!!
        set(value) = publicStore.putString(Key.clashLoglevel, value)

    var metered: Boolean
        get() = publicStore.getBoolean(Key.metered, false)
        set(value) = publicStore.putBoolean(Key.metered, value)
    var portProxy: Int
        get() = getLocalPort(Key.portProxy, 7891)
        set(value) = publicStore.putString(Key.portProxy, value.toString())
    var portLocalDns: Int
        get() = getLocalPort(Key.portLocalDns, 5450)
        set(value) = publicStore.putString(Key.portLocalDns, value.toString())
    var portHttpProxy: Int
        get() = getLocalPort(Key.portHttpProxy, 7890)
        set(value) = publicStore.putString(Key.portHttpProxy, value.toString())
    var portApi: Int
        get() = getLocalPort(Key.portApi, 7892)
        set(value) = publicStore.putString(Key.portApi, value.toString())

    var dnsConfig: String
        get() = publicStore.getString(
            Key.dnsConfig,
            "{\"nameserver\":[\"223.5.5.5\",\"119.29.29.29\",\"https://ndns.233py.com/dns-query\",\"tls://dns.rubyfish.cn\"],\"enhanced-mode\":\"redir-host\",\"fallback\":[\"tls://1.0.0.1:853\",\"tls://1.1.1.1:853\",\"tls://8.8.8.8:853\"],\"enable\":true,\"ipv6\":false,\"listen\":\"0.0.0.0:5450\"}"
        )!!
        set(value) = publicStore.putString(Key.dnsConfig, value.toString())

    var allowLan: Boolean
        get() = publicStore.getBoolean(Key.allowLan, false)
        set(value) = publicStore.putBoolean(Key.allowLan, value)

    var ipv6Enable: Boolean
        get() = publicStore.getBoolean(Key.ipv6, false)
        set(value) = publicStore.putBoolean(Key.ipv6, value)

    var isBypassPrivateNetwork: Boolean
        get() = publicStore.getBoolean(Key.KEY_BYPASS_PRIVATE_NETWORK, true)
        set(value) = publicStore.putBoolean(Key.KEY_BYPASS_PRIVATE_NETWORK, value)


    /**
     * Initialize settings that have complicated default values.
     */
    fun initGlobal() {
        if (publicStore.getBoolean("BypassPrivateNetworkInit", true)) {
            publicStore.putBoolean("BypassPrivateNetworkInit", false)
            isBypassPrivateNetwork = true
        }
        if (publicStore.getString(Key.portProxy) == null) portProxy = portProxy
        if (publicStore.getString(Key.portLocalDns) == null) portLocalDns = portLocalDns
        if (publicStore.getString(Key.portApi) == null) portApi = portApi
        if (publicStore.getString(Key.portHttpProxy) == null) portHttpProxy = portHttpProxy
        if (publicStore.getString(Key.dnsConfig) == null) dnsConfig = dnsConfig
    }


}
