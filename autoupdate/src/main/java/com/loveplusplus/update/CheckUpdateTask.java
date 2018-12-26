package com.loveplusplus.update;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.loveplusplus.update.Constants.*;


/**
 *  feicien (ithcheng@gmail.com)
 * @since 2016-07-05 19:21
 */
class CheckUpdateTask extends AsyncTask<Void, Void, Map<String, String>> {

    private ProgressDialog dialog;
    private Context mContext;
    private int mType;
    private boolean mShowProgressDialog;
    private boolean mForceUpdate;
    private static final String url = Constants.UPDATE_URL;

    CheckUpdateTask(Context context, int type, boolean showProgressDialog, boolean isForceUpdate) {

        this.mContext = context;
        this.mType = type;
        this.mShowProgressDialog = showProgressDialog;
        this.mForceUpdate = mForceUpdate;
    }


    protected void onPreExecute() {
        if (mShowProgressDialog) {
            if (dialog == null) {
                dialog = new ProgressDialog(mContext);
            }
            dialog.setCancelable(false);
            dialog.setMessage(mContext.getString(R.string.android_auto_update_dialog_checking));
            dialog.show();
        }
    }


    @Override
    protected void onPostExecute(Map<String, String> result) {

        if (dialog != null && dialog.isShowing()) {

            try {
                if (((Activity) mContext).isFinishing()) return;
            } catch (Exception e) {
            }
            dialog.dismiss();
        }

        if (result.size() == 2) {
            parseJson(result);
        }
    }

    /**
     * val result = JSONObject()
     * result.put("versionCode", 1)
     * result.put("versionName", "0.0.1-pre1")
     * var download = JSONObject()
     * //'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
     * <p>
     * download.put("armeabi-v7a", 10326378)
     * download.put("arm64-v8a", 10326377)
     * download.put("x86", 10326383)
     * download.put("x86_64", 10326381)
     * download.put("all",10326380)
     * result.put("info", download)
     * result.put("apiUrl","https://api.github.com/repos/ccg2018/ClashA/releases/tags/0.0.1-pre1")
     * println(result.toString())
     *
     * @param result
     */

    private void parseJson(Map<String, String> result) {
        try {
            final String apiResult = result.get(APK_API_RESULT);
            final String githubResult = result.get(APK_GITHUB_RESULT);

            JSONObject obj = new JSONObject(apiResult);

            final int versioncode = obj.optInt(APK_VERSIONCODE, 0);
            final String versionName = obj.optString(APK_VERSIONNAME, "");
            final JSONObject download = obj.getJSONObject(APK_ALL_DOWNLOADS_INFO);

            int currentVersionCode = AppUtils.getVersionCode(mContext);

            if (versioncode > currentVersionCode) {
                //判断当前abi
                String abi = null;
                String[] abis = Build.SUPPORTED_ABIS;
                if (abis != null && abis.length > 0) {
                    abi = abis[0];
                }
                //获得对应版本
                String downloadUrl = "";
                String updateMessage = "";
                final int id = download.optInt(abi, 0);
                if (id == 0) return;
                if (!TextUtils.isEmpty(githubResult)) {
                    JSONObject github = new JSONObject(githubResult);
                    final JSONArray assets = github.getJSONArray(APK_GITHUB_ASSETS);
                    for (int i = 0; i < assets.length(); i++) {
                        final JSONObject jsonObject = assets.getJSONObject(i);
                        if (jsonObject.optInt(APK_GITHUB_ASSETS_ID, 0) == id) {
                            downloadUrl = jsonObject.getString(APK_DOWNLOAD_URL);
                            break;
                        }
                    }
                    updateMessage = github.optString(APK_GITHUB_MD);
                } else return;


                if (mType == Constants.TYPE_NOTIFICATION) {
                    new NotificationHelper(mContext).showNotification(updateMessage, downloadUrl);
                } else if (mType == Constants.TYPE_DIALOG) {
                    if (TextUtils.isEmpty(versionName)) {
                        showDialog(mContext, null, updateMessage, downloadUrl, mForceUpdate);
                    }
                    showDialog(mContext, mContext.getString(R.string.android_auto_update_dialog_title) + " " + versionName, updateMessage, downloadUrl, mForceUpdate);
                }
            } else if (mShowProgressDialog) {
                Toast.makeText(mContext, mContext.getString(R.string.android_auto_update_toast_no_new_update), Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            Log.e(Constants.TAG, "parse json error");
        }
    }


    /**
     * Show dialog
     */
    private void showDialog(Context context, String title, String content, String apkUrl, boolean isCanCancel) {
        UpdateDialog.show(context, title, content, apkUrl, isCanCancel, new UpdateDialog.CallbackShowSomething() {
            @Override
            public void show() {
                dialog = new ProgressDialog(mContext);
                dialog.setCancelable(false);
                dialog.setMessage("正在下载");
                dialog.show();
            }
        });
    }


    @Override
    protected Map<String, String> doInBackground(Void... args) {
        final String updateResult = HttpUtils.get(url);
        Map<String, String> data = new HashMap<>();
        data.put(APK_API_RESULT, updateResult);
        try {
            JSONObject obj = new JSONObject(updateResult);
            final String githubApi = obj.optString(APK_DOWNLOAD_API);
            final String githubResult = HttpUtils.get(githubApi);
            data.put(APK_GITHUB_RESULT, githubResult);
        } catch (JSONException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }

        return data;
    }
}
