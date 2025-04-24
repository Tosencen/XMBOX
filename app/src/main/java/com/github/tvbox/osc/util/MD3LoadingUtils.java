package com.github.tvbox.osc.util;

import android.content.Context;

import com.github.tvbox.osc.ui.dialog.MD3LoadingPopupView;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;

/**
 * Material Design 3 风格的加载弹窗工具类
 */
public class MD3LoadingUtils {

    /**
     * 创建一个 Material Design 3 风格的加载弹窗
     * @param context 上下文
     * @return 加载弹窗实例
     */
    public static BasePopupView createLoadingPopup(Context context) {
        return new XPopup.Builder(context)
                .isLightNavigationBar(true)
                .hasShadowBg(true)  // 启用阴影背景
                .asCustom(new MD3LoadingPopupView(context));
    }
}
