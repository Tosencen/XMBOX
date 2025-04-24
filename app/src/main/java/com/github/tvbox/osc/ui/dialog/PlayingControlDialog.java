package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ColorUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.databinding.DialogPlayingControlBinding;
import com.github.tvbox.osc.player.MyVideoView;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.lxj.xpopup.core.BottomPopupView;

import org.jetbrains.annotations.NotNull;

public class PlayingControlDialog extends BottomPopupView {

    @NonNull
    private final DetailActivity mDetailActivity;
    private final VodController mController;
    MyVideoView mPlayer;
    private com.github.tvbox.osc.databinding.DialogPlayingControlBinding mBinding;

    public PlayingControlDialog(@NonNull @NotNull Context context, VodController controller, MyVideoView videoView) {
        super(context);
        mDetailActivity = (DetailActivity) context;
        mController = controller;
        mPlayer = videoView;
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_playing_control_new;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mBinding = DialogPlayingControlBinding.bind(getPopupImplView());

        initView();
        initListener();
    }

    private void initView(){
        mBinding.scale.setText(mController.mPlayerScaleBtn.getText());
        mBinding.playTimeStart.setText(mController.mPlayerTimeStartBtn.getText());
        mBinding.playTimeEnd.setText(mController.mPlayerTimeSkipBtn.getText());
        mBinding.player.setText(mController.mPlayerBtn.getText());
        mBinding.decode.setText(mController.mPlayerIJKBtn.getText());
        updateAboutIjkVisible();
        updateSpeedUi();
    }

    private void initListener(){
        // 关闭按钮
        View btnClose = getPopupImplView().findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(view -> dismiss());
        }

        //倍速
        mBinding.speed0.setOnClickListener(view -> setSpeed(mBinding.speed0));
        mBinding.speed1.setOnClickListener(view -> setSpeed(mBinding.speed1));
        mBinding.speed1a.setOnClickListener(view -> setSpeed(mBinding.speed1a));
        mBinding.speed2.setOnClickListener(view -> setSpeed(mBinding.speed2));
        mBinding.speed3.setOnClickListener(view -> setSpeed(mBinding.speed3));
        mBinding.speed4.setOnClickListener(view -> setSpeed(mBinding.speed4));
        mBinding.speed5.setOnClickListener(view -> setSpeed(mBinding.speed5));

        //播放器
        mBinding.scale.setOnClickListener(view -> changeAndUpdateText(mBinding.scale,mController.mPlayerScaleBtn));
        mBinding.playTimeStart.setOnClickListener(view -> changeAndUpdateText(mBinding.playTimeStart,mController.mPlayerTimeStartBtn));
        mBinding.playTimeEnd.setOnClickListener(view -> changeAndUpdateText(mBinding.playTimeEnd,mController.mPlayerTimeSkipBtn));
        mBinding.playTimeStart.setOnLongClickListener(view -> {
            mController.mPlayerTimeStartBtn.performLongClick();
            mBinding.playTimeStart.setText(mController.mPlayerTimeStartBtn.getText());
            return true;
        });
        mBinding.playTimeEnd.setOnLongClickListener(view -> {
            mController.mPlayerTimeSkipBtn.performLongClick();
            mBinding.playTimeEnd.setText(mController.mPlayerTimeSkipBtn.getText());
            return true;
        });
        mBinding.increaseStart.setOnClickListener(view -> {
            mController.increaseTime("st");
            updateSkipText(true);
        });
        mBinding.decreaseStart.setOnClickListener(view -> {
            mController.decreaseTime("st");
            updateSkipText(true);
        });
        mBinding.increaseEnd.setOnClickListener(view -> {
            mController.increaseTime("et");
            updateSkipText(false);
        });
        mBinding.decreaseEnd.setOnClickListener(view -> {
            mController.decreaseTime("et");
            updateSkipText(false);
        });
        mBinding.player.setOnClickListener(view -> changeAndUpdateText(mBinding.player,mController.mPlayerBtn));
        mBinding.decode.setOnClickListener(view -> changeAndUpdateText(mBinding.decode,mController.mPlayerIJKBtn));

        //其他
        mBinding.startEndReset.setOnClickListener(view -> resetSkipStartEnd());
        mBinding.replay.setOnClickListener(view -> changeAndUpdateText(null,mController.mPlayRetry));
        mBinding.refresh.setOnClickListener(view -> changeAndUpdateText(null,mController.mPlayRefresh));
        mBinding.subtitle.setOnClickListener(view -> dismissWith(() -> changeAndUpdateText(null,mController.mZimuBtn)));
        mBinding.voice.setOnClickListener(view -> dismissWith(() -> changeAndUpdateText(null,mController.mAudioTrackBtn)));
        mBinding.download.setOnClickListener(view -> dismissWith(mDetailActivity::use1DMDownload));
    }

    private void updateSkipText(boolean start){
        if (start){
            mBinding.playTimeStart.setText(mController.mPlayerTimeStartBtn.getText());
        }else {
            mBinding.playTimeEnd.setText(mController.mPlayerTimeSkipBtn.getText());
        }
    }
    /**
     * 点击直接调用controller里面声明好的点击事件,(不改动原逻辑,隐藏controller里的设置view,全由弹窗设置)
     * @param view 不为空变更配置文字,如更换播放器/缩放, 为空只操作点击之间,不需改变文字,如刷新/重播
     * @param targetView
     */
    private void changeAndUpdateText(View view, TextView targetView){
        targetView.performClick();
        if (view!=null){
            if (view instanceof TextView) {
                ((TextView) view).setText(targetView.getText());
            } else if (view instanceof MaterialButton) {
                ((MaterialButton) view).setText(targetView.getText());
            }
            if (view == mBinding.player){
                updateAboutIjkVisible();
            }
        }
    }

    private void setSpeed(View view){
        String speedText = "";
        if (view instanceof TextView) {
            speedText = ((TextView) view).getText().toString().replace("x","");
        } else if (view instanceof MaterialButton) {
            speedText = ((MaterialButton) view).getText().toString().replace("x","");
        }
        mController.setSpeed(speedText);
        updateSpeedUi();
    }

    private void updateSpeedUi(){
        for (int i = 0; i <mBinding.containerSpeed.getChildCount(); i++) {
            View child = mBinding.containerSpeed.getChildAt(i);
            String speedText = "";
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                speedText = tv.getText().toString().replace("x","");
                tv.setSelected(String.valueOf(mPlayer.getSpeed()).equals(speedText));
            } else if (child instanceof MaterialButton) {
                MaterialButton btn = (MaterialButton) child;
                speedText = btn.getText().toString().replace("x","");
                btn.setSelected(String.valueOf(mPlayer.getSpeed()).equals(speedText));
            }
        }
    }

    /**
     * 如切换/使用的是ijk,解码和音轨按钮才显示
     */
    public void updateAboutIjkVisible(){
        mBinding.decode.setVisibility(mController.mPlayerIJKBtn.getVisibility());
    }

    /**
     * 重置片头/尾,刷新文字
     */
    private void resetSkipStartEnd(){
        changeAndUpdateText(null,mController.mPlayerTimeResetBtn);
        mBinding.playTimeStart.setText(mController.mPlayerTimeStartBtn.getText());
        mBinding.playTimeEnd.setText(mController.mPlayerTimeSkipBtn.getText());
    }

}