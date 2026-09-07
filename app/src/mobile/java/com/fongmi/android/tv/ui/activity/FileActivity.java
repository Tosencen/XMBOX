package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.databinding.ActivityFileBinding;
import com.fongmi.android.tv.ui.adapter.FileAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.github.catvod.utils.Path;
import com.permissionx.guolindev.PermissionX;

import java.io.File;

public class FileActivity extends BaseActivity implements FileAdapter.OnClickListener {

    private ActivityFileBinding mBinding;
    private FileAdapter mAdapter;
    private File dir;
    private boolean pendingPermission;

    private boolean isRoot() {
        return Path.root().equals(dir);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityFileBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        checkPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingPermission) {
            pendingPermission = false;
            if (PermissionUtil.hasManageStorage()) {
                update(Path.root());
            } else {
                finish();
            }
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (PermissionUtil.hasManageStorage()) {
                update(Path.root());
            } else {
                pendingPermission = true;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + App.get().getPackageName()));
                startActivity(intent);
            }
        } else {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> {
                if (allGranted) update(Path.root());
                else finish();
            });
        }
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mAdapter = new FileAdapter(this));
    }

    private void update(File dir) {
        mBinding.recycler.scrollToPosition(0);
        mAdapter.addAll(Path.list(this.dir = dir));
        mBinding.progressLayout.showContent(true, mAdapter.getItemCount());
    }

    @Override
    public void onItemClick(File file) {
        if (file.isDirectory()) {
            update(file);
        } else {
            setResult(RESULT_OK, new Intent().setData(Uri.fromFile(file)));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isRoot()) {
            super.onBackPressed();
        } else {
            update(dir.getParentFile());
        }
    }
}
