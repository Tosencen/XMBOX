package com.github.tvbox.osc.ui.fragment;

import android.view.View;
import com.blankj.utilcode.util.AppUtils;
import com.github.tvbox.osc.base.BaseVbFragment;
import com.xmbox.app.databinding.FragmentMyBinding;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;
import com.github.tvbox.osc.ui.activity.MovieFoldersActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.activity.SubscriptionActivity;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import com.github.tvbox.osc.ui.dialog.ChooseSourceDialog;
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog;
import com.github.tvbox.osc.ui.dialog.VersionHistoryDialog;
import com.github.tvbox.osc.util.AppUpdateManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MaterialSymbolsLoader;
import com.github.tvbox.osc.bean.Source;
import com.github.tvbox.osc.bean.Subscription;
import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lxj.xpopup.XPopup;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class MyFragment extends BaseVbFragment<FragmentMyBinding> {


    private AppUpdateManager updateManager;
    private ArrayList<Subscription> mSubscriptions = new ArrayList<>();
    private ArrayList<Source> mSources = new ArrayList<>();
    private String mSelectedUrl = "";
    private View updateBadge; // 更新提醒标识

    @Override
    protected void init() {
        mBinding.tvVersion.setText("v"+ AppUtils.getAppVersionName());

        // 初始化Material Symbols字体
        MaterialSymbolsLoader.apply(mBinding.iconUpdate);

        // 获取更新提醒标识
        updateBadge = mBinding.updateBadge;

        // 初始化更新管理器
        updateManager = new AppUpdateManager(mActivity);

        // 静默检查更新
        checkUpdateSilently();

        // 设置版本号点击事件，显示版本历史
        View.OnClickListener versionClickListener = v -> {
            new XPopup.Builder(mActivity)
                    .asCustom(new VersionHistoryDialog(mActivity))
                    .show();
        };

        // 为XMBOX标题和版本号设置点击事件
        View headerLayout = mBinding.getRoot().findViewById(com.xmbox.app.R.id.header_layout);
        if (headerLayout != null) {
            headerLayout.setOnClickListener(versionClickListener);
        }
        mBinding.tvVersion.setOnClickListener(versionClickListener);

        // 播放链接功能已移除
        // 直播功能已移除

        mBinding.tvSetting.setOnClickListener(v -> jumpActivity(SettingActivity.class));

        mBinding.tvHistory.setOnClickListener(v -> jumpActivity(HistoryActivity.class));

        mBinding.tvFavorite.setOnClickListener(v -> jumpActivity(CollectActivity.class));

        // 本地播放功能
        mBinding.llLocalPlay.setOnClickListener(v -> jumpActivity(MovieFoldersActivity.class));

        mBinding.llSubscription.setOnClickListener(v -> jumpActivity(SubscriptionActivity.class));

        // 检查更新
        mBinding.llCheckUpdate.setOnClickListener(v -> {
            // 显示正在检查更新的提示
            com.github.tvbox.osc.util.MD3ToastUtils.showToast("正在检查更新...");
            // 检查更新，并显示无更新提示
            updateManager.checkUpdate(true);
            // 点击后隐藏提醒标识
            if (updateBadge != null) {
                updateBadge.setVisibility(View.GONE);
            }
        });

        mBinding.llAbout.setOnClickListener(v -> {
            new XPopup.Builder(mActivity)
                    .asCustom(new AboutDialog(mActivity))
                    .show();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 释放更新管理器资源
        if (updateManager != null) {
            updateManager.release();
            updateManager = null;
        }

        // 取消网络请求
        OkGo.getInstance().cancelTag("get_subscription");
    }

    // 本地视频相关权限方法已移除

    // 播放链接相关方法已移除

    /**
     * 添加订阅
     * @param name 订阅名称
     * @param url 订阅地址
     * @param checked 是否选中
     */
    private void addSubscription(String name, String url, boolean checked) {
        // 获取当前订阅列表
        mSubscriptions = Hawk.get(HawkConfig.SUBSCRIPTIONS, new ArrayList<>());

        if (url.startsWith("clan://")) {
            // 本地订阅直接添加
            addSub2List(name, url, checked);
            // 提示用户
            com.github.tvbox.osc.util.MD3ToastUtils.showToast("添加订阅成功");
        } else if (url.startsWith("http")) {
            // 对于特定的URL直接添加，不尝试解析内容
            if (url.equals("http://ok321.top/tv") || url.equals("https://7213.kstore.vip/吃猫的鱼") || url.startsWith("https://7213.kstore.vip/") || url.equals("http://www.饭太硬.com/tv")) {
                addSub2List(name, url, checked);
                com.github.tvbox.osc.util.MD3ToastUtils.showToast("添加订阅成功");
                return;
            }

            // 显示加载提示
            com.github.tvbox.osc.util.MD3ToastUtils.showToast("正在解析订阅...");

            OkGo.<String>get(url)
                .tag("get_subscription")
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            // 多线路?
                            if (json.has("urls") && json.get("urls").isJsonArray()) { // 多线路
                                if (checked) {
                                    ToastUtils.showLong("多条线路请主动选择");
                                }

                                if (json.get("urls").getAsJsonArray().size() > 0
                                    && json.get("urls").getAsJsonArray().get(0).isJsonObject()
                                    && json.get("urls").getAsJsonArray().get(0).getAsJsonObject().has("url")
                                    && json.get("urls").getAsJsonArray().get(0).getAsJsonObject().has("name")) { //多线路格式

                                    for (int i = 0; i < json.get("urls").getAsJsonArray().size(); i++) {
                                        JsonObject obj = json.get("urls").getAsJsonArray().get(i).getAsJsonObject();
                                        String subName = obj.get("name").getAsString().trim()
                                            .replaceAll("<|>|《|》|-", "");
                                        String subUrl = obj.get("url").getAsString().trim();
                                        mSubscriptions.add(new Subscription(subName, subUrl));
                                    }

                                    // 保存订阅列表
                                    Hawk.put(HawkConfig.SUBSCRIPTIONS, mSubscriptions);

                                    // 提示用户
                                    com.github.tvbox.osc.util.MD3ToastUtils.showToast("添加多线路订阅成功");
                                }
                            } else if (json.has("storeHouse") && json.get("storeHouse").isJsonArray()) { // 多仓
                                if (json.get("storeHouse").getAsJsonArray().size() > 0
                                    && json.get("storeHouse").getAsJsonArray().get(0).isJsonObject()
                                    && json.get("storeHouse").getAsJsonArray().get(0).getAsJsonObject().has("sourceName")
                                    && json.get("storeHouse").getAsJsonArray().get(0).getAsJsonObject().has("sourceUrl")) { //多仓格式

                                    mSources.clear();
                                    for (int i = 0; i < json.get("storeHouse").getAsJsonArray().size(); i++) {
                                        JsonObject obj = json.get("storeHouse").getAsJsonArray().get(i).getAsJsonObject();
                                        String sourceName = obj.get("sourceName").getAsString().trim()
                                            .replaceAll("<|>|《|》|-", "");
                                        String sourceUrl = obj.get("sourceUrl").getAsString().trim();
                                        mSources.add(new Source(sourceName, sourceUrl));
                                    }

                                    new XPopup.Builder(mActivity)
                                        .asCustom(
                                            new ChooseSourceDialog(
                                                mActivity,
                                                mSources,
                                                (position, sourceName) -> {
                                                    // 再根据多线路格式获取配置
                                                    addSubscription(
                                                        mSources.get(position).getSourceName(),
                                                        mSources.get(position).getSourceUrl(),
                                                        checked
                                                    );
                                                }
                                            )
                                        )
                                        .show();
                                }
                            } else { // 单线路/其余
                                addSub2List(name, url, checked);
                                // 提示用户
                                com.github.tvbox.osc.util.MD3ToastUtils.showToast("添加订阅成功");
                            }
                        } catch (Exception e) {
                            // 异常情况下作为单线路处理
                            addSub2List(name, url, checked);
                            // 提示用户
                            com.github.tvbox.osc.util.MD3ToastUtils.showToast("添加订阅成功");
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        ToastUtils.showLong("订阅失败,请检查地址或网络状态");
                    }
                });
        } else {
            ToastUtils.showShort("订阅格式不正确");
        }
    }

    /**
     * 添加订阅到列表
     */
    private void addSub2List(String name, String url, boolean checkNewest) {
        if (checkNewest) { //选中最新的,清除以前的选中订阅
            for (Subscription subscription : mSubscriptions) {
                if (subscription.isChecked()) {
                    subscription.setChecked(false);
                }
            }
            mSelectedUrl = url;
            mSubscriptions.add(new Subscription(name, url).setChecked(true));
        } else {
            mSubscriptions.add(new Subscription(name, url).setChecked(false));
        }

        // 保存订阅列表
        Hawk.put(HawkConfig.API_URL, mSelectedUrl);
        Hawk.put(HawkConfig.SUBSCRIPTIONS, mSubscriptions);
    }

    /**
     * 静默检查更新（不显示对话框）
     */
    private void checkUpdateSilently() {
        if (updateManager != null) {
            updateManager.checkUpdateSilently(new AppUpdateManager.UpdateCheckCallback() {
                @Override
                public void onUpdateAvailable(String newVersion) {
                    // 有新版本可用，显示提醒标识
                    if (mActivity != null && !mActivity.isFinishing() && updateBadge != null) {
                        mActivity.runOnUiThread(() -> {
                            updateBadge.setVisibility(View.VISIBLE);
                        });
                    }
                }

                @Override
                public void onNoUpdateAvailable() {
                    // 没有新版本，隐藏提醒标识
                    if (mActivity != null && !mActivity.isFinishing() && updateBadge != null) {
                        mActivity.runOnUiThread(() -> {
                            updateBadge.setVisibility(View.GONE);
                        });
                    }
                }

                @Override
                public void onCheckFailed() {
                    // 检查失败，不做任何处理
                }
            });
        }
    }
}