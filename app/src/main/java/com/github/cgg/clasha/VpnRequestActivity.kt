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

package com.github.cgg.clasha

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import com.blankj.utilcode.util.LogUtils
import com.crashlytics.android.Crashlytics
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.aidl.ClashAConnection
import com.github.cgg.clasha.aidl.IClashAService
import com.github.cgg.clasha.bg.BaseService
import com.github.cgg.clasha.data.DataStore
import com.github.cgg.clasha.utils.Key
import com.github.cgg.clasha.utils.broadcastReceiver

class VpnRequestActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "VpnRequestActivity"
        private const val REQUEST_CONNECT = 1
    }

    private var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DataStore.serviceMode != Key.modeVpn) {
            finish()
            return
        }
        if (getSystemService<KeyguardManager>()!!.isKeyguardLocked) {
            receiver = broadcastReceiver { _, _ -> request() }
            registerReceiver(receiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        } else request()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) app.startService() else {
            Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_LONG).show()
            LogUtils.w(TAG, "Failed to start VpnService from onActivityResult: $data")
            Crashlytics.log(Log.WARN, TAG, "Failed to start VpnService from onActivityResult: $data")
        }
        finish()
    }

    private fun request() {
        val intent = VpnService.prepare(this)
        if (intent == null) onActivityResult(REQUEST_CONNECT, RESULT_OK, null)
        else startActivityForResult(intent, REQUEST_CONNECT)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiver != null) unregisterReceiver(receiver)
    }
}
