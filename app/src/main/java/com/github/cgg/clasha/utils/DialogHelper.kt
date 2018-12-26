package com.github.cgg.clasha.utils

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.PermissionUtils
import com.github.cgg.clasha.R


/**
 * <pre>
 * author: Blankj
 * blog  : http://blankj.com
 * time  : 2018/01/10
 * desc  : 对话框工具类
</pre> *
 */
object DialogHelper {

    inline fun showRationaleDialog(shouldRequest: PermissionUtils.OnRationaleListener.ShouldRequest, activity: Activity?) {
        activity?.let {
            AlertDialog.Builder(it)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.permission_rationale_message)
                    .setPositiveButton(android.R.string.ok) { dialog, which -> shouldRequest.again(true) }
                    .setNegativeButton(android.R.string.cancel) { dialog, which -> shouldRequest.again(false) }
                    .setCancelable(false)
                    .create()
                    .show()
        }


    }

    fun showOpenAppSettingDialog(activity: Activity?) {
        activity?.let {
            AlertDialog.Builder(it)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.permission_denied_forever_message)
                    .setPositiveButton(android.R.string.ok) { dialog, which -> PermissionUtils.launchAppDetailsSettings() }
                    .setNegativeButton(android.R.string.cancel) { dialog, which -> }
                    .setCancelable(false)
                    .create()
                    .show()
        }

    }

}
