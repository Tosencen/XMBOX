package com.github.tvbox.osc.callback;

import com.xmbox.app.R;
import com.kingja.loadsir.callback.Callback;

/**
 * 收藏页面空状态回调
 */
public class EmptyCollectCallback extends Callback {
    @Override
    protected int onCreateView() {
        return R.layout.loadsir_empty_collect_layout;
    }
}
