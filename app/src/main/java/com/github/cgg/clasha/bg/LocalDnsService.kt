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

package com.github.cgg.clasha.bg

import com.blankj.utilcode.util.LogUtils
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.utils.parseNumericAddress
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet6Address


object LocalDnsService {

    interface Interface : BaseService.Interface {
        override suspend fun startProcesses() {
            super.startProcesses()
            val data = data

            fun makeDns(name: String, address: String, timeout: Int, edns: Boolean = true) = JSONObject().apply {
                put("Name", name)
                put(
                    "Address", when (address.parseNumericAddress()) {
                        is Inet6Address -> "[$address]"
                        else -> address
                    }
                )
                put("Timeout", timeout)
                put("EDNSClientSubnet", JSONObject().put("Policy", "disable"))
                put(
                    "Protocol", if (edns) {
                        put("Socks5Address", "127.0.0.1:" + DataStore.portProxy)
                        "tcp"
                    } else "udp"
                )
            }
            fun makeDns2(name: String, address: String, timeout: Int, edns: Boolean = true) = JSONObject().apply {
                put("Name", name)
                put(
                    "Address", when (address.parseNumericAddress()) {
                        is Inet6Address -> "[$address]"
                        else -> address
                    }
                )
                put("Timeout", timeout)
                put("EDNSClientSubnet", JSONObject().put("Policy", "auto"))
//                put(
//                    "Protocol", if (edns) {
//                        put("Socks5Address", "127.0.0.1:" + DataStore.portProxy)
//                        "tcp"
//                    } else "udp"
//                )
//                put("Socks5Address", "")
                put("Protocol","udp")
            }

            fun buildOvertureConfig(file: String) = file.also {
                val file1 = File(app.filesDir, it)
                LogUtils.i("overture.conf -> ${file1.absolutePath}")
                file1.writeText(JSONObject().run {
                    put("BindAddress", "${DataStore.listenAddress}:${DataStore.portLocalDns}")
                    put("RedirectIPv6Record", true)
                    put("DomainBase64Decode", false)
                    put("HostsFile", "hosts")
                    put("MinimumTTL", 120)
                    put("CacheSize", 4096)
                    //val remoteDns = JSONArray(profile.remoteDns.split(",")
                    //   .mapIndexed { i, dns -> makeDns("UserDef-$i", dns.trim() + ":53", 12) })
                    val remoteDns = JSONArray(
                        arrayListOf(
//                            makeDns2("UserDef-0", "127.0.0.1:5548", 12)
                            makeDns("UserDef-0", "1.1.1.1:53", 12),
                            makeDns("UserDef-1", "8.8.8.8:53", 12)
                        )
                    )
                    val localDns = JSONArray(
                        arrayOf(
                            makeDns("Primary-1", "208.67.222.222:443", 9, false),
                            makeDns("Primary-2", "119.29.29.29:53", 9, false),
                            makeDns("Primary-3", "114.114.114.114:53", 9, false)
                        )
                    )
//                    put("PrimaryDNS", remoteDns)
//                    put("OnlyPrimaryDNS", true)
                    put("PrimaryDNS", localDns)
                    put("AlternativeDNS", remoteDns)
                    put("IPNetworkFile", "china_ip_list.txt")
                    //put("OnlyPrimaryDNS", true)
//                    put("IPNetworkFile", "china_ip_list.txt")
//                    when (profile.route) {
//                        Acl.BYPASS_CHN, Acl.BYPASS_LAN_CHN, Acl.GFWLIST, Acl.CUSTOM_RULES -> {
//                            put("PrimaryDNS", localDns)
//                            put("AlternativeDNS", remoteDns)
//                            put("IPNetworkFile", "china_ip_list.txt")
//                        }
//                        Acl.CHINALIST -> {
//                            put("PrimaryDNS", localDns)
//                            put("AlternativeDNS", remoteDns)
//                        }
//                        else -> {
//                            put("PrimaryDNS", remoteDns)
//                            // no need to setup AlternativeDNS in Acl.ALL/BYPASS_LAN mode
//                            put("OnlyPrimaryDNS", true)
//                        }
//                    }
                    toString()
                })
            }
//            LogUtils.i("#### 启动overture ${buildOvertureConfig("overture.conf")}")

//            data.processes!!.start(
//                buildAdditionalArguments(
//                    arrayListOf(
//                        File(app.applicationInfo.nativeLibraryDir, Executable.OVERTURE).absolutePath,
//                        "-c",
//                        File(app.filesDir, buildOvertureConfig("overture.conf")).absolutePath
//                    )
//                )
//            )
        }
    }
}
