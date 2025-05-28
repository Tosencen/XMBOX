package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.tvbox.osc.util.MD3ToastUtils;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.constant.CacheConst;
import com.github.tvbox.osc.databinding.ActivityLocalPlayBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.MyVideoView;
import com.github.tvbox.osc.player.controller.LocalVideoController;
import com.github.tvbox.osc.receiver.BatteryReceiver;
import com.github.tvbox.osc.ui.dialog.AllLocalSeriesDialog;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.PlayerHelper;
import com.google.common.reflect.TypeToken;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.enums.PopupPosition;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.doikki.videoplayer.player.ProgressManager;
import xyz.doikki.videoplayer.player.VideoView;

public class LocalPlayActivity extends BaseVbActivity<ActivityLocalPlayBinding> {


    private MyVideoView mVideoView;
    LocalVideoController mController;
    JSONObject mVodPlayerCfg;
    private List<VideoInfo> mVideoList = new ArrayList<>();
    private int mPosition;
    BatteryReceiver mBatteryReceiver = new BatteryReceiver();
    private BasePopupView mAllSeriesRightDialog;
    @Override
    protected void init() {
        // 设置默认为竖屏播放
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        registerReceiver(mBatteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mVideoView = mBinding.player;
        // 不自动进入全屏模式，保持竖屏
        // mVideoView.startFullScreen();
        Bundle bundle = getIntent().getExtras();
        String videoListJson =  bundle.getString("videoList");
        mVideoList = GsonUtils.fromJson(videoListJson, new TypeToken<List<VideoInfo>>(){}.getType());
        mPosition = bundle.getInt("position", 0);

        initController();
        initPlayerCfg();
        mVideoView.setVideoController(mController); //设置控制器
        play(false);

        // 使用View的postDelayed方法避免内存泄漏
        mBinding.player.postDelayed(() -> {
            if (mVideoView != null && mVideoView.getCurrentPlayState() == VideoView.STATE_PREPARED) {
                //不知道为啥部分长视频(不确定是不是因为时长/大小)会卡在准备完成状态,所以延迟重置下状态
                mVideoView.pause();
                mVideoView.resume();
            }
        }, 500);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_BATTERY_CHANGE && mController.mMyBatteryView!=null){
            mController.mMyBatteryView.updateBattery((int) event.obj);
        }
    }


    /**
     * 跳转到上/下一集,需重新播放
     */
    private void play(boolean fromSkip) {
        try {
            VideoInfo videoInfo = mVideoList.get(mPosition);

            String path = videoInfo.getPath();

            String uri = "";
            File file = new File(path);
            if(file.exists()){
                uri = Uri.parse("file://"+file.getAbsolutePath()).toString();
            } else {
                MD3ToastUtils.showToast("视频文件不存在：" + videoInfo.getDisplayName());
                return;
            }

            if (uri.isEmpty()) {
                MD3ToastUtils.showToast("无法播放该视频文件");
                return;
            }

            mController.setTitle(videoInfo.getDisplayName());
            mVideoView.setUrl(uri); //设置视频地址

        mVideoView.setProgressManager(new ProgressManager() {
            @Override
            public void saveProgress(String url, long progress) {// 就本地视频页面用sp,其余用Hawk
                //有点本地文件确实总时长,设置下总时长,为什么用path,因为电影列表要通过媒体文件的path获取缓存的时长/进度,存取报纸缓存的key一直
                SPUtils.getInstance(CacheConst.VIDEO_DURATION_SP).put(path, mVideoView.getDuration());
                SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).put(path, progress);
            }

            @Override
            public long getSavedProgress(String url) {
                return SPUtils.getInstance(CacheConst.VIDEO_PROGRESS_SP).getLong(path);
            }
        });

            PlayerHelper.updateCfg(mVideoView, mVodPlayerCfg);

            if (fromSkip){
                mVideoView.replay(true);
            }else {
                mVideoView.start(); //开始播放，不调用则不自动播放
            }
        } catch (Exception e) {
            e.printStackTrace();
            MD3ToastUtils.showToast("播放视频时发生错误");
            finish();
        }
    }

    private void initController() {
        mController = new LocalVideoController(this);
        mController.setListener(new LocalVideoController.VodControlListener() {

            @Override
            public void chooseSeries() {
                showAllSeriesDialog();
            }

            @Override
            public void playNext(boolean rmProgress) {
//                String preProgressKey = progressKey;
//                LocalPlayActivity.this.playNext(rmProgress);
                if (mPosition == mVideoList.size() - 1){
                    MD3ToastUtils.showToast("当前已经是最后一集了");
                } else {
                    mPosition++;
                    play(true);
                }
            }

            @Override
            public void playPre() {
                //playPrevious();
                if (mPosition == 0){
                    MD3ToastUtils.showToast("当前已经是第一集了");
                }else {
                    mPosition--;
                    play(true);
                }
            }

            @Override
            public void changeParse(ParseBean pb) {

            }

            @Override
            public void updatePlayerCfg() {

            }

            @Override
            public void replay(boolean replay) {

            }

            @Override
            public void errReplay() {

            }

            @Override
            public void selectSubtitle() {

            }

            @Override
            public void selectAudioTrack() {

            }

            @Override
            public void prepared() {

            }

            @Override
            public void toggleFullScreen() {
                // 实现真正的横竖屏切换
                int currentOrientation = getRequestedOrientation();
                if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT ||
                    currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                    currentOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                    // 当前是竖屏，切换到横屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    mVideoView.startFullScreen();
                } else {
                    // 当前是横屏，切换到竖屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    mVideoView.stopFullScreen();
                }

                // 延迟更新控制器按钮文本，等待屏幕方向变化完成
                new Handler().postDelayed(() -> {
                    if (mController != null) {
                        mController.initLandscapePortraitBtnInfo();
                    }
                }, 500);
            }

            @Override
            public void exit() {
                finish();
            }
        });

    }

    void initPlayerCfg() {
        mVodPlayerCfg = new JSONObject();
        try {
            if (!mVodPlayerCfg.has("pl")) {
                mVodPlayerCfg.put("pl", Hawk.get(HawkConfig.PLAY_TYPE, 1));
            }
            if (!mVodPlayerCfg.has("pr")) {
                mVodPlayerCfg.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0));
            }
            if (!mVodPlayerCfg.has("ijk")) {
                mVodPlayerCfg.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, "硬解码"));
            }
            if (!mVodPlayerCfg.has("sc")) {
                mVodPlayerCfg.put("sc", 0);  // 强制使用默认缩放模式
            }
            if (!mVodPlayerCfg.has("sp")) {
                mVodPlayerCfg.put("sp", 1.0f);
            }
            if (!mVodPlayerCfg.has("st")) {
                mVodPlayerCfg.put("st", 0);
            }
            if (!mVodPlayerCfg.has("et")) {
                mVodPlayerCfg.put("et", 0);
            }
        } catch (Throwable th) {

        }
        mController.setPlayerConfig(mVodPlayerCfg);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBatteryReceiver);
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
    }


    @Override
    public void onBackPressed() {
        if (!mVideoView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 屏幕方向改变时更新控制器按钮
        if (mController != null) {
            new Handler().postDelayed(() -> {
                mController.initLandscapePortraitBtnInfo();
            }, 100);
        }
    }

    @Override
    public void finish() {
        super.finish();
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_REFRESH, ""));
    }

    public void showAllSeriesDialog(){
        // 根据屏幕方向选择不同的弹窗样式
        int orientation = getResources().getConfiguration().orientation;

        if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏时使用右侧弹窗
            mAllSeriesRightDialog = new XPopup.Builder(this)
                    .isViewMode(true)
                    .hasNavigationBar(false)
                    .popupHeight(com.blankj.utilcode.util.ScreenUtils.getScreenHeight())
                    .popupWidth(com.blankj.utilcode.util.ScreenUtils.getAppScreenWidth() / 2)
                    .popupPosition(PopupPosition.Right)
                    .asCustom(new AllLocalSeriesDialog(this, convertLocalVideo(), (position, text) -> {
                        mPosition = position;
                        play(true);
                        mAllSeriesRightDialog.dismiss();
                    }));
            mAllSeriesRightDialog.show();
        } else {
            // 竖屏时使用底部弹窗
            mAllSeriesRightDialog = new XPopup.Builder(this)
                    .isViewMode(true)
                    .hasNavigationBar(false)
                    .maxHeight(com.blankj.utilcode.util.ScreenUtils.getScreenHeight() - (com.blankj.utilcode.util.ScreenUtils.getScreenHeight() / 4))
                    .asCustom(new AllLocalSeriesDialog(this, convertLocalVideo(), (position, text) -> {
                        mPosition = position;
                        play(true);
                        mAllSeriesRightDialog.dismiss();
                    }));
            mAllSeriesRightDialog.show();
        }
    }

    private List<VodInfo.VodSeries> convertLocalVideo(){
        List<VodInfo.VodSeries> seriesList = new ArrayList<>();
        for (int i = 0; i < mVideoList.size(); i++) {
            VideoInfo local = mVideoList.get(i);
            // 使用显示名称作为集数名称，如果没有则使用文件名
            String displayName = local.getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                // 从文件路径中提取文件名（不包含扩展名）
                String fileName = local.getPath();
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                if (fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                }
                displayName = fileName;
            }

            // 如果显示名称仍然为空或者太长，使用序号
            if (displayName == null || displayName.trim().isEmpty() || displayName.length() > 20) {
                displayName = "第" + (i + 1) + "集";
            }

            VodInfo.VodSeries vodSeries = new VodInfo.VodSeries(displayName, local.getPath());
            vodSeries.selected = (i == mPosition);
            seriesList.add(vodSeries);
        }
        return seriesList;
    }
}