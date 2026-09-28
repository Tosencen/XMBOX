package com.fongmi.android.tv.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityCrashBinding;
import com.fongmi.android.tv.databinding.DialogCrashDetailsBinding;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.github.catvod.utils.Prefers;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class CrashActivity extends BaseActivity {

    private ActivityCrashBinding mBinding;

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCrashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCrash();
    }

    @Override
    protected void initEvent() {
        mBinding.details.setOnClickListener(v -> showError());
        mBinding.restart.setOnClickListener(v -> CustomActivityOnCrash.restartApplication(this, Objects.requireNonNull(CustomActivityOnCrash.getConfigFromIntent(getIntent()))));
    }

    private void setCrash() {
        String log = CustomActivityOnCrash.getActivityLogFromIntent(getIntent());
        if (TextUtils.isEmpty(log)) return;
        String[] lines = log.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].isEmpty()) continue;
            if (lines[i].contains(HomeActivity.class.getSimpleName())) {
                Prefers.put("crash", true);
                break;
            }
        }
    }

    private void showError() {
        String errorDetails = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, getIntent());
        DialogCrashDetailsBinding binding = DialogCrashDetailsBinding.inflate(LayoutInflater.from(this));
        binding.text.setText(errorDetails);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.crash_details_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.crash_details_close, null)
                .setNeutralButton(R.string.crash_copy, (dialog, which) -> {
                    copyErrorToClipboard(errorDetails);
                })
                .show();
    }

    private void copyErrorToClipboard(String errorDetails) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.crash_details_title), errorDetails);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.crash_copy_success, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.crash_copy_fail, Toast.LENGTH_SHORT).show();
        }
    }
}
