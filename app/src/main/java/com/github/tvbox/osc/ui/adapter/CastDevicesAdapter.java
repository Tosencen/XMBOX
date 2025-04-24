package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/23
 * @description: 投屏设备适配器（已禁用DLNA功能）
 */
public class CastDevicesAdapter extends BaseQuickAdapter<Object, BaseViewHolder> {

    public CastDevicesAdapter() {
        super(R.layout.item_title);
    }

    @Override
    protected void convert(BaseViewHolder helper, Object item) {
        helper.setText(R.id.title, "投屏功能已禁用");
    }
}
