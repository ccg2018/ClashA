package com.github.cgg.clasha.bg

import android.app.Service
import android.content.Intent

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-01-06
 * @describe
 */

class ProxyService : Service(), BaseService.Interface {
    override val data = BaseService.Data(this)


    override val tag: String get() = "ClashAProxyService"
    override fun createNotification(profileName: String): ServiceNotification =
        ServiceNotification(this, "$profileName proxy mode", "service-proxy", true)

    override fun onBind(intent: Intent) = super.onBind(intent)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        super<BaseService.Interface>.onStartCommand(intent, flags, startId)
    override fun onDestroy() {
        super.onDestroy()
        data.binder.close()
    }
}