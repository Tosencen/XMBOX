package com.github.tvbox.osc.callback;

import com.github.tvbox.osc.R;
import com.kingja.loadsir.callback.Callback;

/**
 * 观看历史页面空状态回调
 */
public class EmptyHistoryCallback extends Callback {
    @Override
    protected int onCreateView() {
        return R.layout.loadsir_empty_history_layout;
    }
}
