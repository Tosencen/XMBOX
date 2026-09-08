package com.fongmi.android.tv.ui.dialog;

import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.DialogSyncSettingsBinding;
import com.fongmi.android.tv.event.ScanEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.activity.ScanActivity;
import com.fongmi.android.tv.ui.adapter.DeviceAdapter;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.ScanTask;
import com.github.catvod.net.OkHttp;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * 观看同步设置对话框
 * 整合了同步模式、设备扫描、自动同步等功能
 */
public class SyncSettingsDialog extends BaseDialog implements DeviceAdapter.OnClickListener, ScanTask.Listener {

    private final FormBody.Builder body;
    private final OkHttpClient client;
    private final ScanTask scanTask;
    private final TypedArray mode;
    private DialogSyncSettingsBinding binding;
    private DeviceAdapter adapter;
    private String[] modeNames;

    public static SyncSettingsDialog create() {
        return new SyncSettingsDialog();
    }

    public SyncSettingsDialog() {
        body = new FormBody.Builder();
        scanTask = new ScanTask(this);
        client = OkHttp.client(Constant.TIMEOUT_SYNC);
        mode = ResUtil.getTypedArray(R.array.cast_mode);
        modeNames = new String[]{"双向同步", "上传", "下载"};
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSyncSettingsBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setRecyclerView();
        getDevice();
        setMode();
        setAutoSyncStatus();
    }

    @Override
    protected void initEvent() {
        // 移除同步模式点击事件，保持默认双向同步
        // binding.mode.setOnClickListener(v -> onMode());
        binding.scan.setOnClickListener(v -> onScan());
        binding.refresh.setOnClickListener(v -> onRefresh());
        binding.autoSyncSwitch.setOnClickListener(v -> setAutoSync());
        binding.syncInterval.setOnClickListener(v -> setSyncInterval());
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter = new DeviceAdapter(this));
    }

    private void getDevice() {
        // 设备列表读数据库移到后台线程，避免主线程访问 Room
        App.execute(() -> {
            List<Device> devices = Device.getAll();
            App.post(() -> {
                adapter.addAll(devices);
                if (adapter.getItemCount() == 0) App.post(this::onRefresh, 1000);
            });
        });
    }

    private void setMode() {
        // 强制设置为双向同步模式（index=0）
        int index = 0;
        Setting.putSyncMode(index);
        binding.mode.setImageResource(mode.getResourceId(index, 0));
        binding.mode.setTag(String.valueOf(index));
        binding.modeText.setText(modeNames[index]);
    }

    private void setAutoSyncStatus() {
        binding.autoSyncSwitch.setChecked(Setting.isAutoSync());
        binding.syncIntervalText.setText(Setting.getSyncInterval() + "分钟");
    }

    private void onMode() {
        int index = Setting.getSyncMode();
        Setting.putSyncMode(index = index == mode.length() - 1 ? 0 : ++index);
        binding.mode.setImageResource(mode.getResourceId(index, 0));
        binding.mode.setTag(String.valueOf(index));
        binding.modeText.setText(modeNames[index]);
    }

    private void onScan() {
        ScanActivity.start(getActivity());
    }

    private void onRefresh() {
        scanTask.start(adapter.getIps());
        adapter.clear();
    }

    private void setAutoSync() {
        boolean isChecked = !Setting.isAutoSync();
        Setting.putAutoSync(isChecked);
        if (isChecked) {
            Notify.show("自动同步已启用");
        } else {
            Notify.show("自动同步已关闭");
        }
    }

    private void setSyncInterval() {
        String[] intervals = new String[]{"10分钟", "30分钟", "60分钟", "120分钟"};
        int[] values = new int[]{10, 30, 60, 120};
        int currentInterval = Setting.getSyncInterval();
        int currentIndex = 1; // 默认30分钟
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentInterval) {
                currentIndex = i;
                break;
            }
        }
        
        new MaterialAlertDialogBuilder(getActivity())
            .setTitle("同步间隔")
            .setNegativeButton(R.string.dialog_negative, null)
            .setSingleChoiceItems(intervals, currentIndex, (dialog, which) -> {
                Setting.putSyncInterval(values[which]);
                binding.syncIntervalText.setText(intervals[which]);
                dialog.dismiss();
            }).show();
    }

    private void onSuccess() {
        Notify.show("同步成功");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScanEvent(ScanEvent event) {
        Notify.show("正在连接设备...");
        scanTask.start(event.getAddress());
    }

    @Override
    public void onFind(List<Device> devices) {
        if (!devices.isEmpty()) {
            adapter.addAll(devices);
            // 扫码后自动启用同步功能
            if (!Setting.isAutoSync()) {
                Setting.putAutoSync(true);
            }
            // 确保同步间隔为30分钟
            if (Setting.getSyncInterval() != 30) {
                Setting.putSyncInterval(30);
            }
            // 扫码后自动触发第一个设备的同步
            if (devices.size() == 1) {
                Device device = devices.get(0);
                Notify.show("发现设备: " + device.getName() + "，正在同步...");
                App.post(() -> onItemClick(device), 500);
            }
        } else {
            Notify.show("未发现设备，请确保TV版已开启");
        }
    }

    @Override
    public void onItemClick(Device item) {
        // 先在主线程取 UI 状态
        String mode = binding.mode.getTag().toString();
        // 同步数据（设备/配置/历史）读取移到后台线程，避免主线程访问数据库
        App.execute(() -> {
            FormBody.Builder builder = new FormBody.Builder();
            builder.add("device", Device.get().toString());
            builder.add("config", Config.vod().toString());
            builder.add("targets", App.gson().toJson(History.get()));

            // 发送同步请求
            String url = String.format(Locale.getDefault(),
                "%s/action?do=sync&mode=%s&type=history",
                item.getIp(), mode);

            OkHttp.newCall(client, url, builder.build()).enqueue(getCallback());
        });
    }

    @Override
    public boolean onLongClick(Device item) {
        String modeStr = binding.mode.getTag().toString();
        if (modeStr.equals("0")) return false;
        // 清空历史与构建同步数据都移到后台线程（含数据库读写）
        App.execute(() -> {
            if (modeStr.equals("2")) History.delete(VodConfig.getCid());

            FormBody.Builder builder = new FormBody.Builder();
            builder.add("device", Device.get().toString());
            builder.add("config", Config.vod().toString());
            builder.add("targets", App.gson().toJson(History.get()));

            // 发送强制同步请求
            String url = String.format(Locale.getDefault(),
                "%s/action?do=sync&mode=%s&type=history&force=true",
                item.getIp(), modeStr);

            OkHttp.newCall(client, url, builder.build()).enqueue(getCallback());
        });
        return true;
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                App.post(() -> onSuccess());
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                App.post(() -> Notify.show(e.getMessage()));
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanTask.stop();
    }
}
