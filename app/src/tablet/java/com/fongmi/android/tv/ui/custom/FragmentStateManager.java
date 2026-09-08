package com.fongmi.android.tv.ui.custom;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fongmi.android.tv.ui.base.BaseFragment;

public abstract class FragmentStateManager {

    private final FragmentManager fm;
    private final ViewGroup container;

    public FragmentStateManager(ViewGroup container, FragmentManager fm) {
        this.container = container;
        this.fm = fm;
    }

    public abstract Fragment getItem(int position);

    public boolean change(int position) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(getTag(position));
        if (fragment == null) {
            fragment = getItem(position);
            ft.add(container.getId(), fragment, getTag(position));
        }
        // 用 replace 确保 ViewPager 正确刷新
        Fragment current = fm.getPrimaryNavigationFragment();
        if (current != null && current != fragment) {
            ft.hide(current);
        }
        ft.show(fragment);
        ft.setPrimaryNavigationFragment(fragment);
        ft.setReorderingAllowed(true);
        ft.commitNowAllowingStateLoss();
        // 切换后强制刷新容器布局
        container.post(() -> container.requestLayout());
        return true;
    }

    private String getTag(int position) {
        return "android:switcher:" + position;
    }

    public BaseFragment getFragment(int position) {
        return (BaseFragment) fm.findFragmentByTag(getTag(position));
    }

    public boolean isVisible(int position) {
        Fragment fragment = getFragment(position);
        return fragment != null && fragment.isVisible();
    }

    public boolean canBack(int position) {
        BaseFragment fragment = getFragment(position);
        return fragment != null && (fragment.canBack() || fragment.isHidden());
    }
}
