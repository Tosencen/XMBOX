package com.github.tvbox.osc.callback;

import com.xmbox.app.R;
import com.kingja.loadsir.callback.Callback;

/**
 * 订阅管理页面空状态回调
 */
public class EmptySubscriptionCallback extends Callback {
    @Override
    protected int onCreateView() {
        return R.layout.loadsir_empty_subscription_layout;
    }
}
