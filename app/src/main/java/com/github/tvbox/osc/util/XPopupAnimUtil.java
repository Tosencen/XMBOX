package com.github.tvbox.osc.util;

import android.content.Context;

import com.github.tvbox.osc.R;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.enums.PopupAnimation;

/**
 * XPopup动画工具类，提供更丝滑的弹窗动画
 */
public class XPopupAnimUtil {

    /**
     * 创建一个带有Material Design 3风格动画的XPopup.Builder
     * @param context 上下文
     * @return XPopup.Builder实例
     */
    public static XPopup.Builder createMD3Builder(Context context) {
        XPopup.Builder builder = new XPopup.Builder(context)
                .isDarkTheme(Utils.isDarkTheme())
                .popupAnimation(PopupAnimation.ScaleAlphaFromCenter);

        // 在白天模式下使用浅色背景
        if (!Utils.isDarkTheme()) {
            builder.isLightStatusBar(true);
        }

        return builder;
    }
}
