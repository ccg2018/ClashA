package com.loveplusplus.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.noties.markwon.Markwon;

class UpdateDialog {

    interface CallbackShowSomething {
        void show();
    }


    static void show(final Context context, String title, String content, final String downloadUrl, final boolean isCanCancel, final CallbackShowSomething callback) {
        if (isContextValid(context)) {
            LinearLayout linearLayout =new LinearLayout(context);
            final LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            linearLayout.setPadding(dp2px(context,24),0,dp2px(context,24),0);
            linearLayout.setLayoutParams(layoutParams);

            TextView textView = new TextView(context);
            Markwon.setMarkdown(textView,content);
            linearLayout.addView(textView);
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(TextUtils.isEmpty(title) ? context.getString(R.string.android_auto_update_dialog_title) : title)
                    .setView(linearLayout)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setPositiveButton(R.string.android_auto_update_dialog_btn_download, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            goToDownload(context, downloadUrl, isCanCancel);
                            if (!isCanCancel) {
                                callback.show();
                            }
                        }
                    }).setCancelable(false);
            if (isCanCancel) {
                builder.setNegativeButton(R.string.android_auto_update_dialog_btn_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
            }
            builder.show();
        }
    }

    private static boolean isContextValid(Context context) {
        return context instanceof Activity && !((Activity) context).isFinishing();
    }


    private static void goToDownload(Context context, String downloadUrl, boolean isCanCancel) {

        Intent intent = new Intent(context.getApplicationContext(), DownloadService.class);
        intent.putExtra(Constants.APK_DOWNLOAD_URL, downloadUrl);
        if (!isCanCancel) {
            //不能取消就一直显示
            intent.putExtra(Constants.MUST_UPDATE, true);
        }
        context.startService(intent);
    }

    private static int dp2px(Context context,final float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
