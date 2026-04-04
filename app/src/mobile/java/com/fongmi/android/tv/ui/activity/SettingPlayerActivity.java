package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.ActivitySettingPlayerBinding;
import com.fongmi.android.tv.impl.BufferCallback;
import com.fongmi.android.tv.impl.SpeedCallback;
import com.fongmi.android.tv.impl.UaCallback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.BufferDialog;
import com.fongmi.android.tv.ui.dialog.SpeedDialog;
import com.fongmi.android.tv.ui.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;

import java.text.DecimalFormat;

public class SettingPlayerActivity extends BaseActivity implements UaCallback, BufferCallback, SpeedCallback {

    private ActivitySettingPlayerBinding mBinding;
    private DecimalFormat format;
    private String[] background;
    private String[] caption;
    private String[] render;
    private String[] scale;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPlayerActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingPlayerBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        format = new DecimalFormat("0.#");
        mBinding.uaText.setText(Setting.getUa());
        mBinding.tunnelSwitch.setChecked(Setting.isTunnel());
        mBinding.audioDecodeSwitch.setChecked(Setting.isAudioPrefer());
        mBinding.aacSwitch.setChecked(Setting.isPreferAAC());
        mBinding.danmakuLoadSwitch.setChecked(Setting.isDanmakuLoad());
        mBinding.speedText.setText(format.format(Setting.getSpeed()));
        mBinding.bufferText.setText(String.valueOf(Setting.getBuffer()));
        mBinding.caption.setVisibility(Setting.hasCaption() ? View.VISIBLE : View.GONE);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Setting.getScale()]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Setting.getRender()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.select_caption))[Setting.isCaption() ? 1 : 0]);
        // 修复数组越界：确保 background 索引在有效范围内
        background = ResUtil.getStringArray(R.array.select_background);
        int bgIndex = Setting.getBackground();
        if (bgIndex < 0 || bgIndex >= background.length) {
            bgIndex = 0;
            Setting.putBackground(0);
        }
        mBinding.backgroundText.setText(background[bgIndex]);
        mBinding.playerEngineText.setText(getPlayerEngineText());
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> finish());
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.scale.setOnClickListener(this::onScale);
        mBinding.speed.setOnClickListener(this::onSpeed);
        mBinding.buffer.setOnClickListener(this::onBuffer);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.playerEngine.setOnClickListener(this::setPlayerEngine);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.background.setOnClickListener(this::onBackground);
        
        // 直接给开关按钮设置点击监听器，避免双重点击冲突
        mBinding.tunnelSwitch.setOnClickListener(this::setTunnel);
        mBinding.audioDecodeSwitch.setOnClickListener(this::setAudioDecode);
        mBinding.aacSwitch.setOnClickListener(this::setAAC);
        mBinding.danmakuLoadSwitch.setOnClickListener(this::setDanmakuLoad);
    }

    private void onUa(View view) {
        UaDialog.create(this).show();
    }

    @Override
    public void setUa(String ua) {
        mBinding.uaText.setText(ua);
        Setting.putUa(ua);
    }

    private void setAAC(View view) {
        boolean isChecked = !Setting.isPreferAAC();
        Setting.putPreferAAC(isChecked);
        // 不需要再次调用 setChecked，因为点击已经触发了状态变化
    }

    private void onScale(View view) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.player_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(scale, Setting.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(scale[which]);
            Setting.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void onSpeed(View view) {
        SpeedDialog.create(this).show();
    }

    @Override
    public void setSpeed(float speed) {
        mBinding.speedText.setText(format.format(speed));
        Setting.putSpeed(speed);
    }

    private void onBuffer(View view) {
        BufferDialog.create(this).show();
    }

    @Override
    public void setBuffer(int times) {
        mBinding.bufferText.setText(String.valueOf(times));
        Setting.putBuffer(times);
    }

    private void setRender(View view) {
        int index = Setting.getRender();
        Setting.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
        if (Setting.isTunnel() && Setting.getRender() == 1) setTunnel(view);
    }

    private String getPlayerEngineText() {
        int engine = Setting.getPlayerEngine();
        switch (engine) {
            case 0: return "ExoPlayer (软解)";
            case 1: return "ExoPlayer (硬解)";
            case 2: return "ExoPlayer (自动)";
            case 3: return "MPV";
            default: return "ExoPlayer (自动)";
        }
    }

    private void setPlayerEngine(View view) {
        String[] engines = new String[]{"ExoPlayer (软解)", "ExoPlayer (硬解)", "ExoPlayer (自动)", "MPV"};
        int current = Setting.getPlayerEngine();
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("播放器引擎").setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(engines, current, (dialog, which) -> {
            Setting.putPlayerEngine(which);
            mBinding.playerEngineText.setText(engines[which]);
            dialog.dismiss();
        }).show();
    }

    private void setTunnel(View view) {
        boolean isChecked = !Setting.isTunnel();
        Setting.putTunnel(isChecked);
        // 不需要再次调用 setChecked，因为点击已经触发了状态变化
        if (isChecked && Setting.getRender() == 1) setRender(view);
    }

    private void setCaption(View view) {
        Setting.putCaption(!Setting.isCaption());
        mBinding.captionText.setText(caption[Setting.isCaption() ? 1 : 0]);
    }

    private boolean onCaption(View view) {
        if (Setting.isCaption()) startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        return Setting.isCaption();
    }

    private void onBackground(View view) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle(R.string.player_background).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(background, Setting.getBackground(), (dialog, which) -> {
            mBinding.backgroundText.setText(background[which]);
            Setting.putBackground(which);
            dialog.dismiss();
        }).show();
    }

    private void setAudioDecode(View view) {
        boolean isChecked = !Setting.isAudioPrefer();
        Setting.putAudioPrefer(isChecked);
        // 不需要再次调用 setChecked，因为点击已经触发了状态变化
    }

    private void setDanmakuLoad(View view) {
        boolean isChecked = !Setting.isDanmakuLoad();
        Setting.putDanmakuLoad(isChecked);
        // 不需要再次调用 setChecked，因为点击已经触发了状态变化
    }
} 