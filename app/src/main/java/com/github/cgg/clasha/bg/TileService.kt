package com.github.cgg.clasha.bg

import android.app.KeyguardManager
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.blankj.utilcode.util.LogUtils
import com.github.cgg.clasha.App.Companion.app
import com.github.cgg.clasha.R
import com.github.cgg.clasha.aidl.ClashAConnection
import com.github.cgg.clasha.aidl.IClashAService
import com.github.cgg.clasha.data.DataStore
import android.service.quicksettings.TileService as BaseTileService

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-03-11
 * @describe
 */
@RequiresApi(24)
class TileService : BaseTileService(), ClashAConnection.Callback {
    //todo test clash logo
    private val iconIdle by lazy { Icon.createWithResource(this, R.drawable.ic_clash_logo) }//ide
    private val iconBusy by lazy { Icon.createWithResource(this, R.drawable.ic_clash_logo) }//busy
    private val iconConnected by lazy { Icon.createWithResource(this, R.drawable.ic_clash_logo) }//active

    private val keyguard by lazy { getSystemService<KeyguardManager>()!! }
    private var tapPending = false

    private val connection = ClashAConnection()


    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        LogUtils.w("TileService", state, profileName, msg)
        updateTile(state) { profileName }
    }

    override fun onServiceConnected(service: IClashAService) {
        LogUtils.w("TileService", "onServiceConnected ")

        updateTile(BaseService.State.values()[service.state]) { service.profileName }
        if (tapPending) {
            tapPending = false
            onClick()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        connection.connect(this, this)
    }

    override fun onStopListening() {
        connection.disconnect(this)
        super.onStopListening()
    }

    override fun onClick() {
        if (isLocked && !DataStore.canToggleLocked) unlockAndRun(this::toggle) else toggle()
    }

    private fun updateTile(serviceState: BaseService.State, profileName: () -> String?) {
        qsTile?.apply {
            label = null
            when (serviceState) {
                BaseService.State.Idle -> throw IllegalStateException("serviceState")
                BaseService.State.Connecting -> {
                    icon = iconBusy
                    state = Tile.STATE_ACTIVE
                }
                BaseService.State.Connected -> {
                    icon = iconConnected
                    if (!keyguard.isDeviceLocked) label = profileName()
                    state = Tile.STATE_ACTIVE
                }
                BaseService.State.Stopping -> {
                    icon = iconBusy
                    state = Tile.STATE_UNAVAILABLE
                }
                BaseService.State.Stopped -> {
                    icon = iconIdle
                    state = Tile.STATE_INACTIVE
                }
            }
            label = label ?: getString(R.string.app_name)
            updateTile()
        }
    }

    private fun toggle() {
        val service = connection.service
        if (service == null) tapPending = true else BaseService.State.values()[service.state].let { state ->
            when {
                state.canStop -> app.stopService()
                state == BaseService.State.Stopped -> app.startService()
            }
        }
    }
}