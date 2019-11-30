package com.github.cgg.clasha.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.github.cgg.clasha.R;

/**
 * @Author: ccg
 * @Email: ccg
 * @program: BottomSheets
 * @create: 2018-12-19
 * @describe
 */
public class ClashABottomSheetWebview extends WebView {

    private CoordinatorLayout bottomCoordinator;
    private float downY;
    private float moveY;

    public ClashABottomSheetWebview(Context context) {
        super(context);
    }

    public ClashABottomSheetWebview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClashABottomSheetWebview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ClashABottomSheetWebview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void bindBottomSheetDialog(FrameLayout container, final OnTouchListener listener) {
        try {
            bottomCoordinator =
                    (CoordinatorLayout) container.findViewById(R.id.coordinator);
            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (bottomCoordinator != null) {
                        bottomCoordinator.requestDisallowInterceptTouchEvent(true);
                    }

                    if (listener != null) {
                        listener.onTouch(v, event);
                    }
                    return false;

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
