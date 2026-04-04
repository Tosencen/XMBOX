package com.fongmi.android.tv.utils;
import com.github.catvod.utils.Logger;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Rational;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.media3.ui.R;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.receiver.ActionReceiver;

import java.util.ArrayList;
import java.util.List;

public class PiP {

    private PictureInPictureParams.Builder builder;

    public static boolean noPiP() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !App.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private RemoteAction buildRemoteAction(Activity activity, @DrawableRes int icon, @StringRes int title, String action) {
        return new RemoteAction(Icon.createWithResource(activity, icon), activity.getString(title), "", ActionReceiver.getPendingIntent(activity, action));
    }

    private RemoteAction getPlayPauseAction(Activity activity, boolean play) {
        if (play) return buildRemoteAction(activity, R.drawable.exo_icon_pause, R.string.exo_controls_pause_description, ActionEvent.PAUSE);
        return buildRemoteAction(activity, R.drawable.exo_icon_play, R.string.exo_controls_play_description, ActionEvent.PLAY);
    }

    public PiP() {
        if (noPiP()) return;
        this.builder = new PictureInPictureParams.Builder();
    }

    public boolean isInMode(Activity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode();
    }

    public void update(Activity activity, View view) {
        if (noPiP()) return;
        Rect sourceRectHint = new Rect();
        view.getGlobalVisibleRect(sourceRectHint);
        builder.setSourceRectHint(sourceRectHint);
        try {
            activity.setPictureInPictureParams(builder.build());
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    public void update(Activity activity, boolean play) {
        if (noPiP()) return;
        List<RemoteAction> actions = new ArrayList<>();
        actions.add(buildRemoteAction(activity, R.drawable.exo_icon_previous, R.string.exo_controls_previous_description, ActionEvent.PREV));
        actions.add(getPlayPauseAction(activity, play));
        actions.add(buildRemoteAction(activity, R.drawable.exo_icon_next, R.string.exo_controls_next_description, ActionEvent.NEXT));
        try {
            activity.setPictureInPictureParams(builder.setActions(actions).build());
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    public void enter(Activity activity, int width, int height, int scale) {
        enter(activity, width, height, scale, false);
    }

    public void enter(Activity activity, int width, int height, int scale, boolean force) {
        try {
            if (noPiP() || activity.isInPictureInPictureMode()) return;
            // force=true 表示手动触发，不检查设置；force=false 表示自动触发，需要检查设置
            if (!force && !Setting.isBackgroundPiP()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setSeamlessResizeEnabled(true);
            if (scale == 1) builder.setAspectRatio(new Rational(16, 9));
            else if (scale == 2) builder.setAspectRatio(new Rational(4, 3));
            else builder.setAspectRatio(getRational(width, height));
            activity.enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            Logger.e("Error", e);
        }
    }

    private Rational getRational(int width, int height) {
        Rational limitWide = new Rational(239, 100);
        Rational limitTall = new Rational(100, 239);
        Rational rational = new Rational(width, height);
        if (rational.isInfinite()) return new Rational(16, 9);
        if (rational.floatValue() > limitWide.floatValue()) return limitWide;
        if (rational.floatValue() < limitTall.floatValue()) return limitTall;
        return rational;
    }
}
