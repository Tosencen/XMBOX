package com.fongmi.android.tv.ui.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.event.StateEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.receiver.ShortcutReceiver;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.FragmentStateManager;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.google.android.material.navigation.NavigationBarView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private FragmentStateManager mManager;
    private ActivityHomeBinding mBinding;
    private int orientation;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        // 检查隐私协议
        if (!Setting.isPrivacyAgreed()) {
            Intent intent = new Intent(this, PrivacyAgreementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // 确保通知渠道已创建（用户已同意协议的情况）
        com.fongmi.android.tv.utils.Notify.createChannel();
        
        orientation = getResources().getConfiguration().orientation;
        // Updater.create().release().start(this); // 移除自动检查更新，只在点击版本号时检查
        initFragment(savedInstanceState);
        Server.get().start();
        initConfig();
        setNavigation();
    }

    @Override
    protected void initEvent() {
        NavigationBarView navigation = getNavigationView();
        if (navigation != null) {
            navigation.setOnItemSelectedListener(this);
            View liveView = navigation.findViewById(R.id.live);
            if (liveView != null) {
                liveView.setOnLongClickListener(this::addShortcut);
            }
        }
        bindFab();
    }
    
    private NavigationBarView getNavigationView() {
        // 平板首页已改用 FAB 悬浮导航，不再使用底部/侧边 NavigationBarView
        return null;
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
                loadLive("file:/" + FileChooser.getPathFromUri(this, intent.getData()));
            } else {
                VideoActivity.push(this, intent.getData().toString());
            }
        }
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0) return VodFragment.newInstance();
                if (position == 1) return SettingFragment.newInstance();
                return null;
            }
        };
        if (savedInstanceState == null) mManager.change(0);
    }

    private void initConfig() {
        // 把配置的 Room 读取移到后台线程，避免主线程访问数据库
        App.execute(() -> {
            Config wall = Config.wall();
            Config live = Config.live();
            Config vod = Config.vod();
            App.post(() -> {
                WallConfig.get().init(wall);
                LiveConfig.get().init(live).load();
                VodConfig.get().init(vod).load(getCallback());
            });
        });
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success(String result) {
                Notify.show(result);
            }

            @Override
            public void success() {
                checkAction(getIntent());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(String msg) {
                RefreshEvent.config();
                StateEvent.empty();
                Notify.show(msg);
            }
        };
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                openLive();
            }
        });
    }

    private void setNavigation() {
        NavigationBarView navigation = getNavigationView();
        if (navigation != null) {
            navigation.getMenu().findItem(R.id.vod).setVisible(true);
            navigation.getMenu().findItem(R.id.setting).setVisible(true);
            navigation.getMenu().findItem(R.id.live).setVisible(LiveConfig.hasUrl() && !Setting.isLiveTabVisible());
        }
        syncFabLive();
    }

    private boolean openLive() {
        LiveActivity.start(this);
        return false;
    }

    private boolean addShortcut(View view) {
        ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, getString(R.string.nav_live)).setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher)).setIntent(new Intent(Intent.ACTION_VIEW, null, this, LiveActivity.class)).setShortLabel(getString(R.string.nav_live)).build();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, ShortcutReceiver.class).setAction(ShortcutReceiver.ACTION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ShortcutManagerCompat.requestPinShortcut(this, info, pendingIntent.getIntentSender());
        return true;
    }

    public void change(int position) {
        mManager.change(position);
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (event.getType().equals(RefreshEvent.Type.CONFIG)) setNavigation();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.getType() != ServerEvent.Type.PUSH) return;
        VideoActivity.push(this, event.getText());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        NavigationBarView navigation = getNavigationView();
        if (navigation == null || navigation.getSelectedItemId() == item.getItemId()) return false;
        return switchPage(item.getItemId());
    }

    private boolean switchPage(int id) {
        int position = -1;
        if (id == R.id.setting) position = 1;
        else if (id == R.id.vod) position = 0;
        else if (id == R.id.live) {
            if (LiveConfig.isEmpty()) {
                Notify.showCenter(R.string.error_no_live);
                return false;
            }
            return openLive();
        }
        if (position < 0) return false;
        // 如果当前已经是目标页面，强制刷新
        if (mManager.isVisible(position)) {
            BaseFragment fragment = mManager.getFragment(position);
            if (fragment != null && fragment.getView() != null) {
                fragment.getView().post(() -> {
                    fragment.getView().requestLayout();
                    fragment.onHiddenChanged(false);
                });
            }
            return true;
        }
        mManager.change(position);
        return true;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        App.post(() -> checkOrientation(newConfig), 100);
    }

    private void checkOrientation(Configuration newConfig) {
        if (orientation != newConfig.orientation) {
            orientation = newConfig.orientation;
            RefreshEvent.video();
        }
    }

    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        if (mManager.isVisible(1)) {
            mManager.change(0);
        } else if (mManager.canBack(0)) {
            finish();
        }
    }

    private boolean menuOpen;

    private void bindFab() {
        mBinding.fabMain.setOnClickListener(v -> toggleFabMenu());
        mBinding.fabScrim.setOnClickListener(v -> closeFabMenu());
        View.OnClickListener page = v -> {
            closeFabMenu();
            int id = v.getId();
            if (id == R.id.fab_vod) switchPage(R.id.vod);
            else if (id == R.id.fab_live) switchPage(R.id.live);
            else if (id == R.id.fab_setting) switchPage(R.id.setting);
        };
        mBinding.fabVod.setOnClickListener(page);
        mBinding.fabLive.setOnClickListener(page);
        mBinding.fabSetting.setOnClickListener(page);
        syncFabLive();
    }

    private void syncFabLive() {
        if (mBinding.fabLive != null) {
            mBinding.fabLive.setVisibility(LiveConfig.hasUrl() && !Setting.isLiveTabVisible() ? View.VISIBLE : View.GONE);
        }
    }

    private void toggleFabMenu() {
        if (menuOpen) closeFabMenu();
        else openFabMenu();
    }

    private void openFabMenu() {
        menuOpen = true;
        mBinding.fabScrim.setVisibility(View.VISIBLE);
        mBinding.fabMenu.setVisibility(View.VISIBLE);
        int count = mBinding.fabMenu.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mBinding.fabMenu.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(40f);
            child.animate().alpha(1f).translationY(0f).setDuration(180).setStartDelay((count - 1 - i) * 40L).start();
        }
        mBinding.fabMain.setImageResource(R.drawable.ic_fab_close);
    }

    private void closeFabMenu() {
        menuOpen = false;
        mBinding.fabScrim.setVisibility(View.GONE);
        mBinding.fabMenu.setVisibility(View.GONE);
        mBinding.fabMain.setImageResource(R.drawable.ic_fab_menu);
    }

    @Override
    protected void onDestroy() {
        WallConfig.get().clear();
        LiveConfig.get().clear();
        VodConfig.get().clear();
        OkHttp.get().clear();
        AppDatabase.backup();
        Source.get().exit();
        Server.get().stop();
        super.onDestroy();
    }
}
