package com.loveplusplus.update;

import android.content.Context;
import android.util.Log;

public class UpdateChecker {



    public static void checkForDialog(Context context, String updateUrl) {
        checkForDialog(context, updateUrl, false);
    }

    public static void checkForDialog(Context context, String updateUrl, boolean isForceUpdate) {
        if (context != null) {
            Constants.UPDATE_URL = updateUrl;
            new CheckUpdateTask(context, Constants.TYPE_DIALOG, true, isForceUpdate).execute();
        } else {
            Log.e(Constants.TAG, "The arg context is null");
        }
    }


    public static void checkForNotification(Context context, String updateUrl) {
        if (context != null) {
            Constants.UPDATE_URL = updateUrl;
            new CheckUpdateTask(context, Constants.TYPE_NOTIFICATION, false, false).execute();
        } else {
            Log.e(Constants.TAG, "The arg context is null");
        }

    }


}
