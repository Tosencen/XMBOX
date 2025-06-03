package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.xmbox.app.R;
import com.github.tvbox.osc.bean.CastVideo;
import com.github.tvbox.osc.cast.CastDevice;
import com.github.tvbox.osc.cast.CastManager;
import com.github.tvbox.osc.util.MD3ToastUtils;
import com.lxj.xpopup.core.CenterPopupView;

import java.util.ArrayList;
import java.util.List;

/**
 * 投屏设备选择对话框
 */
public class CastDeviceDialog extends CenterPopupView implements CastManager.CastDeviceListener {

    private RecyclerView rvDevices;
    private TextView tvNoDevices;
    private DeviceAdapter adapter;
    private CastVideo castVideo;
    private CastManager castManager;

    public CastDeviceDialog(@NonNull Context context, CastVideo castVideo) {
        super(context);
        this.castVideo = castVideo;
        this.castManager = CastManager.getInstance();
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_cast_device;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        rvDevices = findViewById(R.id.rv_devices);
        tvNoDevices = findViewById(R.id.tv_no_devices);

        // 设置RecyclerView
        rvDevices.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeviceAdapter();
        rvDevices.setAdapter(adapter);

        // 设置点击事件
        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                CastDevice device = (CastDevice) adapter.getItem(position);
                if (device != null) {
                    // 投屏功能已禁用
                    // castManager.castToDevice(device, castVideo);
                    MD3ToastUtils.showLongToast("投屏功能已禁用");
                    dismiss();
                }
            }
        });

        // 投屏功能已禁用
        /*
        // 注册设备监听器
        castManager.addDeviceListener(this);

        // 开始搜索设备
        castManager.startDiscovery();
        */

        // 显示禁用消息
        tvNoDevices.setText("投屏功能已禁用");
        tvNoDevices.setVisibility(View.VISIBLE);
        rvDevices.setVisibility(View.GONE);
    }

    /**
     * 更新设备列表
     */
    private void updateDeviceList() {
        List<CastDevice> devices = castManager.getDevices();
        adapter.setNewData(devices);

        if (devices.isEmpty()) {
            tvNoDevices.setVisibility(View.VISIBLE);
            rvDevices.setVisibility(View.GONE);
        } else {
            tvNoDevices.setVisibility(View.GONE);
            rvDevices.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDeviceAdded(CastDevice device) {
        updateDeviceList();
    }

    @Override
    public void onDeviceRemoved(CastDevice device) {
        updateDeviceList();
    }

    @Override
    protected void onDismiss() {
        super.onDismiss();
        // 投屏功能已禁用
        /*
        castManager.removeDeviceListener(this);
        castManager.stopDiscovery();
        */
    }

    /**
     * 设备适配器
     */
    private static class DeviceAdapter extends BaseQuickAdapter<CastDevice, BaseViewHolder> {
        public DeviceAdapter() {
            super(R.layout.item_cast_device, new ArrayList<>());
        }

        @Override
        protected void convert(BaseViewHolder helper, CastDevice item) {
            helper.setText(R.id.tv_device_name, item.getName());
            helper.setText(R.id.tv_device_type, item.getTypeName());
        }
    }
}
