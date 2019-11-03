package com.github.cgg.clasha.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.webkit.WebSettings.ZoomDensity;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.blankj.utilcode.util.LogUtils;
import com.github.cgg.clasha.BuildConfig;
import com.github.cgg.clasha.R;

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: BottomSheets
 * @create: 2018-12-19
 * @describe
 */
public class ClashAWebviewBottomSheetDialog extends ClashABottomSheetDialog {


    private ProgressBar mProgress;
    private LinearLayout llToolbarClose;
    private ImageView ivToolbarNavigation;
    private TextView tvToolbarTitle;
    private ClashABottomSheetWebview mWebview;
    private boolean isCanBack = true;
    private boolean isShowNavBack = true;
    private String port;

    public ClashAWebviewBottomSheetDialog(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ClashAWebviewBottomSheetDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init(context);
    }

    protected ClashAWebviewBottomSheetDialog(@NonNull Context context, boolean cancelable, DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.echat_dialog_layout_webview, null);

        //获得主要的主体颜色
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        //设置控件
        final RelativeLayout toolbar = (RelativeLayout) view.findViewById(R.id.toolbarLayout);
        ivToolbarNavigation = (ImageView) view.findViewById(R.id.ivToolbarNavigation);
        tvToolbarTitle = (TextView) view.findViewById(R.id.tvToolbarTitle);
        llToolbarClose = (LinearLayout) view.findViewById(R.id.llToolbarClose);
        mWebview = (ClashABottomSheetWebview) view.findViewById(R.id.webview);
        mProgress = (ProgressBar) view.findViewById(android.R.id.progress);

        //设置toolbar颜色 圆角
        float[] outerRadius = {10, 10, 10, 10, 0, 0, 0, 0};
        RectF inset = new RectF(0, 0, 0, 0);
        float[] innerRadius = {0, 0, 0, 0, 0, 0, 0, 0};//内矩形 圆角半径
        RoundRectShape roundRectShape = new RoundRectShape(outerRadius, inset, innerRadius);
        ShapeDrawable drawable = new ShapeDrawable(roundRectShape);
        drawable.getPaint().setColor(colorPrimary);
        toolbar.setBackground(drawable);


        if (!isShowNavBack) ivToolbarNavigation.setVisibility(View.GONE);
        //事件
        llToolbarClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        ivToolbarNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCanBack) {
                    if (mWebview.canGoBack()) {
                        mWebview.goBack();
                    } else {
                        dismiss();
                    }
                } else {
                    dismiss();
                }
            }
        });
        tvToolbarTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        //设置ContentView
        setContentView(view);

        //初始化Webview
        mWebview.setWebViewClient(mWebViewClient);
        mWebview.setWebChromeClient(mWebChromeClient);
        final WebSettings settings = mWebview.getSettings();
        //开启JavaScript支持
        settings.setJavaScriptEnabled(true);
        //默认设置为true，即允许在 File 域下执行任意 JavaScript 代码
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        //Disable zoom
        settings.setSupportZoom(true);
        //提高渲染优先级
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        // 开启DOM storage API 功能
        settings.setDomStorageEnabled(true);
        // 开启database storage API功能
        settings.setDatabaseEnabled(true);
        // 开启Application Cache功能
        settings.setAppCacheEnabled(true);
        //设置脚本是否允许自动打开弹窗
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        //设置WebView是否支持多屏窗口
        settings.setSupportMultipleWindows(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (BuildConfig.DEBUG) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {//这个版本之后 被默认禁止
            settings.setAllowUniversalAccessFromFileURLs(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        //允许iframe与外部域名不一致的时候出现的 请求丢失cookie
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebview, true);
        }

    }


    private WebViewClient mWebViewClient = new WebViewClient() {

        //>= Android 5.0
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            loadJS("window.localStorage.setItem('" + "externalControllerHost" + "','" + "127.0.0.1" + "');");
            loadJS("window.localStorage.setItem('" + "externalControllerPort" + "','" + port + "');");
            Uri url = request.getUrl();
            if (url != null) {
                view.loadUrl(url.toString());
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            loadJS("window.localStorage.setItem('" + "externalControllerHost" + "','" + "127.0.0.1" + "');");
            loadJS("window.localStorage.setItem('" + "externalControllerPort" + "','" + port + "');");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (mProgress != null) {
                mProgress.setVisibility(View.GONE);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            LogUtils.e("WebView error: " + errorCode + " + " + description);

        }

    };

    private WebChromeClient mWebChromeClient = new WebChromeClient() {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (mProgress != null) {
                mProgress.setMax(100);
                mProgress.setProgress(newProgress);
                // 如果进度大于或者等于100，则隐藏进度条
                if (newProgress >= 100) {
                    mProgress.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage cm) {
            LogUtils.i(cm.message() + " -- From line "
                    + cm.lineNumber() + " of "
                    + cm.sourceId());
            return true;
        }
    };

    public void loadUrl(String url) {
        if (mWebview == null) return;
        mWebview.loadUrl(url);
    }

    private void loadJS(final String trigger) {
        mWebview.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWebview.evaluateJavascript(trigger, null);
                } else {
                    mWebview.loadUrl(trigger);
                }
            }
        });
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setCanBack(boolean canBack) {
        isCanBack = canBack;
    }

    public void setShowBackNav(boolean show) {
        isShowNavBack = show;
        ivToolbarNavigation.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setTitle(@StringRes final int resId) {
        setTitle(getContext().getResources().getText(resId).toString());
    }

    public void setTitle(String title) {
        if (tvToolbarTitle != null) {
            tvToolbarTitle.setText(title);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWebview != null) {
            mWebview.clearHistory();
            ((ViewGroup) mWebview.getParent()).removeView(mWebview);
            mWebview.loadUrl("about:blank");
            mWebview.stopLoading();
            mWebview.setWebChromeClient(null);
            mWebview.setWebViewClient(null);
            mWebview.destroy();
        }
    }

    @Override
    public void show() {
        super.show();
        mWebview.bindBottomSheetDialog(getContainer());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
