package com.github.tvbox.osc.ui.adapter;

import com.blankj.utilcode.util.LogUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Movie;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class CastDevicesAdapter extends BaseQuickAdapter<Device, BaseViewHolder> implements RegistryListener {

    public CastDevicesAdapter() {
        super(R.layout.item_title);
    }

    @Override
    protected void convert(BaseViewHolder helper, Device item) {
        helper.setText(R.id.title, item.getDetails().getFriendlyName());
    }

    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
        // 远程设备发现开始
    }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
        // 远程设备发现失败
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        // 添加远程设备
        addData(device);
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
        // 更新远程设备
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        // 移除远程设备
        List<Device> data = getData();
        if (data.contains(device)) {
            data.remove(device);
            notifyDataSetChanged();
        }
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice device) {
        // 添加本地设备
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
        // 移除本地设备
    }

    @Override
    public void beforeShutdown(Registry registry) {
        // 关闭前
    }

    @Override
    public void afterShutdown() {
        // 关闭后
    }
}