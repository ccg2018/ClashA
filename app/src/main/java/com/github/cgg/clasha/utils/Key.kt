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

package com.github.cgg.clasha.utils

import android.util.Log
import com.github.cgg.clasha.BuildConfig

object Key {
    /**
     * Public config that doesn't need to be kept secret.
     */
    const val DB_PUBLIC = "pubConfig.db"
    const val DB_PRIVATE_CONFIG = "priConfig.db"
    const val DB_LOGS = "logs.db"

    const val LOG_TYPE_DNS = "dns"
    const val LOG_TYPE_RULE = "rule"
    const val LOG_TYPE_OTHER = "other"
    val isONKey = "versionCode${BuildConfig.VERSION_CODE}isOn"


    const val id = "profileId"
    const val name = "profileName"

    const val individual = "Proxyed"

    const val nightMode = "nightMode"
    const val nightModeSystem = "system"
    const val nightModeAuto = "auto"
    const val nightModeOff = "off"
    const val nightModeOn = "on"

    const val clashMode = "clashMode"
    const val clashModeRule = "Rule"

    const val clashLoglevel = "clashLoglevel"
    const val clashLogInfo = "info"

    const val dnsMode = "dnsMode"
    const val modeRedirhost = "redir-host"
    const val modeFakeip = "fake-ip"

    const val serviceMode = "serviceMode"
    const val modeProxy = "proxy"
    const val modeVpn = "vpn"
    const val modeTransproxy = "transproxy"
    const val shareOverLan = "shareOverLan"
    const val portProxy = "portProxy"
    const val portHttpProxy = "portHttpProxy"
    const val portLocalDns = "portLocalDns"
    const val portTransproxy = "portTransproxy"
    const val portApi = "portApi"
    const val metered = "metered"
    const val KEY_BYPASS_PRIVATE_NETWORK = "key_vpn_setting_bypass_private_network"

    const val route = "route"

    const val isAutoConnect = "isAutoConnect"
    const val directBootAware = "directBootAware"

    const val proxyApps = "isProxyApps"
    const val bypass = "isBypassApps"
    const val udpdns = "isUdpDns"
    const val ipv6 = "ipv6Enable"

    const val host = "proxy"
    const val password = "sitekey"
    const val method = "encMethod"
    const val remotePort = "remotePortNum"
    const val remoteDns = "remoteDns"

    const val plugin = "plugin"
    const val pluginConfigure = "plugin.configure"

    const val dirty = "profileDirty"

    const val tfo = "tcp_fastopen"
    const val assetUpdateTime = "assetUpdateTime"


    const val dnsConfig = "dnsConfig"
    const val allowLan = "allowLan"


    const val LOG_PAGESIZE = 10
}

object Action {
    const val SERVICE = "com.github.ccg.clasha.SERVICE"
    const val CLOSE = "com.github.ccg.clasha.CLOSE"
    const val RELOAD = "com.github.ccg.clasha.RELOAD"
}
