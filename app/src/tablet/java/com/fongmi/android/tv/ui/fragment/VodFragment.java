package com.fongmi.android.tv.ui.fragment;
import com.github.catvod.utils.Logger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.event.CastEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.StateEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.FilterCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.HistoryActivity;
import com.fongmi.android.tv.ui.activity.KeepActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.fongmi.android.tv.ui.adapter.HistoryCardAdapter;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.FilterDialog;
import com.fongmi.android.tv.ui.dialog.LastWatchToast;
import com.fongmi.android.tv.ui.dialog.LinkDialog;
import com.fongmi.android.tv.ui.dialog.ReceiveDialog;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Response;

public class VodFragment extends BaseFragment implements SiteCallback, FilterCallback, TypeAdapter.OnClickListener, ConfigCallback {

    private FragmentVodBinding mBinding;
    private SiteViewModel mViewModel;
    private TypeAdapter mAdapter;
    private HistoryCardAdapter mHistoryAdapter;
    private Runnable mRunnable;
    private List<String> mHots;
    private Result mResult;

    public static VodFragment newInstance() {
        return new VodFragment();
    }

    private TypeFragment getFragment() {
        return (TypeFragment) mBinding.pager.getAdapter().instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    private Site getSite() {
        return VodConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setRecyclerView();
        setViewModel();
        setupHistoryRecycler();
        initStartupState(); // 根据是否已有配置来设置初始状态
        setLogo();
        initHot();
        getHot();
        loadHistory();
        // 检查是否需要显示上次播放弹窗
        checkLastWatchDialog();
    }
    
    // 初始化启动状态：区分已有配置和无配置的情况
    private void initStartupState() {
        // 检查是否已经有保存的配置，添加空值检查
        boolean hasExistingConfig = false;
        try {
            Config config = VodConfig.get().getConfig();
            hasExistingConfig = config != null && 
                               config.getUrl() != null && 
                               !config.getUrl().isEmpty();
        } catch (Exception e) {
            // 如果获取配置时出错，认为没有配置
            hasExistingConfig = false;
        }
        
        if (hasExistingConfig) {
            // 已有配置：显示加载状态，确保不显示添加源提示
            showProgress();
            mBinding.emptySourceHint.setVisibility(View.GONE);
        } else {
            // 无配置：立即显示空源提示，不显示加载状态
            hideProgress();
            checkEmptySource();
        }
    }

    @Override
    protected void initEvent() {
        mBinding.hot.setOnClickListener(this::onHot);
        mBinding.top.setOnClickListener(this::onTop);
        mBinding.link.setOnClickListener(this::onLink);
        mBinding.logo.setOnClickListener(this::onLogo);
        mBinding.keep.setOnClickListener(this::onKeep);
        mBinding.retry.setOnClickListener(this::onRetry);
        mBinding.filter.setOnClickListener(this::onFilter);
        mBinding.search.setOnClickListener(this::onSearch);
        mBinding.history.setOnClickListener(this::onHistory);
        mBinding.historyMore.setOnClickListener(this::onHistory);
        mBinding.filter.setOnLongClickListener(this::onLink);
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.type.smoothScrollToPosition(position);
                mAdapter.setActivated(position);
                setFabVisible(position);
            }
        });
    }

    // 添加检查上次播放历史并显示弹窗的方法
    private void checkLastWatchDialog() {
        if (App.isAppJustLaunched()) {
            // 历史记录读数据库移到后台线程
            App.execute(() -> {
                List<History> histories = History.get();
                App.post(() -> {
                    if (!histories.isEmpty() && getActivity() != null) {
                        LastWatchToast.create(getActivity(), histories.get(0)).show();
                    }
                    App.setAppLaunched();
                }, 1000);
            });
        }
    }

    private void setRecyclerView() {
        mBinding.type.setHasFixedSize(true);
        mBinding.type.setItemAnimator(null);
        mBinding.type.setAdapter(mAdapter = new TypeAdapter(this));
        mBinding.pager.setAdapter(new PageAdapter(getChildFragmentManager()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), result -> setAdapter(mResult = result));
    }

    private void initHot() {
        mHots = Hot.get(Setting.getHot());
        App.post(mRunnable = this::updateHot, 0);
    }

    private void getHot() {
        OkHttp.newCall("https://api.web.360kan.com/v1/rank?cat=1", Headers.of(HttpHeaders.REFERER, "https://www.360kan.com/rank/general")).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                mHots = Hot.get(response.body().string());
            }
        });
    }

    private void updateHot() {
        App.post(mRunnable, TimeUnit.SECONDS.toMillis(10));
        if (mHots.isEmpty() || mHots.size() < 10) return;
        mBinding.hot.setText(mHots.get(new Random().nextInt(11)));
    }

    private Result handle(Result result) {
        List<Class> types = new ArrayList<>();
        for (Class type : result.getTypes()) if (result.getFilters().containsKey(type.getTypeId())) type.setFilters(result.getFilters().get(type.getTypeId()));
        for (String cate : getSite().getCategories()) for (Class type : result.getTypes()) if (cate.equals(type.getTypeName())) types.add(type);
        result.setTypes(types);
        return result;
    }

    private void setAdapter(Result result) {
        mAdapter.addAll(handle(result));
        mBinding.pager.getAdapter().notifyDataSetChanged();
        setFabVisible(0);
        hideProgress();
        checkRetry();
        checkEmptySource(); // 添加检查是否显示空源提示
    }

    // 修改checkEmptySource方法，增强鲁棒性
    private void checkEmptySource() {
        // 检查是否有基础配置文件，添加空值检查
        boolean hasBaseConfig = false;
        try {
            Config config = VodConfig.get().getConfig();
            hasBaseConfig = config != null && 
                           config.getUrl() != null && 
                           !config.getUrl().isEmpty();
        } catch (Exception e) {
            hasBaseConfig = false;
        }
        
        // 检查是否有有效的站点配置
        boolean hasValidSites = false;
        boolean hasValidHome = false;
        try {
            hasValidSites = VodConfig.get().getSites().size() > 0;
            Site site = getSite();
            hasValidHome = site != null && site.getKey() != null && !site.getKey().isEmpty();
        } catch (Exception e) {
            hasValidSites = false;
            hasValidHome = false;
        }
        
        // 只有在完全没有配置文件或配置文件无效时才显示空源提示
        boolean isEmpty = !hasBaseConfig || (!hasValidSites || !hasValidHome);
        
        if (mBinding.emptySourceHint != null) {
            mBinding.emptySourceHint.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (isEmpty) {
                // 设置整个布局的点击事件
                mBinding.emptySourceHint.setOnClickListener(this::onAddSource);
                // 设置按钮的点击事件
                if (mBinding.addSourceBtn != null) {
                    mBinding.addSourceBtn.setOnClickListener(this::onAddSource);
                }
                // 空源状态下隐藏所有悬浮按钮
                hideFabButtons();
                // 启动Lottie动画
                try {
                    LottieAnimationView lottieView = mBinding.emptySourceHint.findViewById(R.id.lottieAnimation);
                    if (lottieView != null) {
                        lottieView.playAnimation();
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        }
    }
    
    // 添加源按钮点击事件处理
    private void onAddSource(View view) {
        ConfigDialog.create(this).type(0).show();
    }
    
    // 实现ConfigCallback接口
    @Override
    public void setConfig(Config config) {
        android.util.Log.d("VodFragment", "setConfig called with: " + (config != null ? config.toString() : "null"));
        
        if (config == null || config.isEmpty()) {
            android.util.Log.d("VodFragment", "Config is null or empty, returning");
            return;
        }
        
        // 检查Fragment是否还在活动状态，增强检查
        if (!isValidFragmentState()) {
            android.util.Log.d("VodFragment", "Fragment state invalid, returning");
            return;
        }
        
        android.util.Log.d("VodFragment", "Fragment state valid, proceeding with config load");
        
        // 安全地隐藏空源提示
        try {
            if (mBinding != null && mBinding.emptySourceHint != null) {
                mBinding.emptySourceHint.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Logger.e("Error", e);
        }
        
        Notify.progress(getActivity());
        android.util.Log.d("VodFragment", "Calling VodConfig.load");
        VodConfig.load(config, new Callback() {
            @Override
            public void success() {
                android.util.Log.d("VodFragment", "VodConfig.load success callback");
                // 双重检查Fragment是否还在活动状态
                if (!isValidFragmentState()) {
                    android.util.Log.d("VodFragment", "Fragment state invalid in success callback");
                    return;
                }
                
                try {
                    android.util.Log.d("VodFragment", "Success: dismissing notify and refreshing");
                    Notify.dismiss();
                    RefreshEvent.config();
                    RefreshEvent.video();
                    homeContent();
                } catch (Exception e) {
                    android.util.Log.e("VodFragment", "Error in success callback", e);
                    Logger.e("Error", e);
                }
            }
            
            @Override
            public void error(String msg) {
                android.util.Log.e("VodFragment", "VodConfig.load error: " + msg);
                // 双重检查Fragment是否还在活动状态
                if (!isValidFragmentState()) {
                    android.util.Log.d("VodFragment", "Fragment state invalid in error callback");
                    return;
                }
                
                try {
                    Notify.dismiss();
                    Notify.show(msg);
                    // 加载失败时重新显示空源提示
                    checkEmptySource();
                } catch (Exception e) {
                    android.util.Log.e("VodFragment", "Error in error callback", e);
                    Logger.e("Error", e);
                }
            }
        });
    }
    
    // 添加Fragment状态检查方法
    private boolean isValidFragmentState() {
        return getActivity() != null && 
               !getActivity().isFinishing() && 
               !getActivity().isDestroyed() && 
               isAdded() && 
               !isDetached() && 
               !isRemoving() &&
               getView() != null &&
               mBinding != null;
    }

    private void setFabVisible(int position) {
        // 检查是否为空源状态 - 使用与checkEmptySource相同的逻辑，添加空值检查
        boolean hasBaseConfig = false;
        boolean hasValidSites = false;
        boolean hasValidHome = false;
        
        try {
            Config config = VodConfig.get().getConfig();
            hasBaseConfig = config != null && 
                           config.getUrl() != null && 
                           !config.getUrl().isEmpty();
            
            hasValidSites = VodConfig.get().getSites().size() > 0;
            
            Site site = getSite();
            hasValidHome = site != null && site.getKey() != null && !site.getKey().isEmpty();
        } catch (Exception e) {
            hasBaseConfig = false;
            hasValidSites = false;
            hasValidHome = false;
        }
        
        boolean isEmpty = !hasBaseConfig || (!hasValidSites || !hasValidHome);
        
        if (isEmpty) {
            // 空源状态下隐藏所有悬浮按钮
            hideFabButtons();
        } else if (mAdapter.getItemCount() == 0) {
            mBinding.top.setVisibility(View.INVISIBLE);
            mBinding.link.setVisibility(View.VISIBLE);
            mBinding.filter.setVisibility(View.GONE);
        } else if (!mAdapter.get(position).getFilters().isEmpty()) {
            mBinding.top.setVisibility(View.INVISIBLE);
            mBinding.link.setVisibility(View.GONE);
            mBinding.filter.show();
        } else if (position == 0 || mAdapter.get(position).getFilters().isEmpty()) {
            mBinding.top.setVisibility(View.INVISIBLE);
            mBinding.filter.setVisibility(View.GONE);
            mBinding.link.show();
        }
    }
    
    // 隐藏所有悬浮按钮的方法
    private void hideFabButtons() {
        mBinding.top.setVisibility(View.GONE);
        mBinding.link.setVisibility(View.GONE);
        mBinding.filter.setVisibility(View.GONE);
    }

    private void checkRetry() {
        mBinding.retry.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void onTop(View view) {
        getFragment().scrollToTop();
        mBinding.top.setVisibility(View.INVISIBLE);
        if (mBinding.filter.getVisibility() == View.INVISIBLE) mBinding.filter.show();
        else if (mBinding.link.getVisibility() == View.INVISIBLE) mBinding.link.show();
    }

    private boolean onLink(View view) {
        LinkDialog.create(this).show();
        return true;
    }

    private void onLogo(View view) {
        SiteDialog.create(this).change().show();
    }

    private void onKeep(View view) {
        KeepActivity.start(getActivity());
    }

    private void onRetry(View view) {
        homeContent();
    }

    private void onFilter(View view) {
        if (mAdapter.getItemCount() > 0) FilterDialog.create().filter(mAdapter.get(mBinding.pager.getCurrentItem()).getFilters()).show(this);
    }

    private void onHot(View view) {
        CollectActivity.start(getActivity());
    }

    private void onSearch(View view) {
        CollectActivity.start(getActivity(), mBinding.hot.getText().toString());
    }

    private void onHistory(View view) {
        HistoryActivity.start(getActivity());
    }

    private void setupHistoryRecycler() {
        mBinding.historyRecycler.setLayoutManager(
            new androidx.recyclerview.widget.LinearLayoutManager(
                getContext(), 
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, 
                false
            )
        );
        mHistoryAdapter = new HistoryCardAdapter(item -> {
            VideoActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
        });
        mBinding.historyRecycler.setAdapter(mHistoryAdapter);
    }

    private void loadHistory() {
        // 检查是否显示历史记录
        if (!Setting.isHistoryVisible()) {
            mBinding.historySection.setVisibility(View.GONE);
            return;
        }

        // 历史记录读数据库移到后台线程，避免主线程访问 Room
        App.execute(() -> {
            List<History> histories = History.get();
            App.post(() -> {
                if (histories == null || histories.isEmpty()) {
                    mBinding.historySection.setVisibility(View.GONE);
                } else {
                    mBinding.historySection.setVisibility(View.VISIBLE);
                    mHistoryAdapter.setItems(histories);
                }
            });
        });
    }

    private void showProgress() {
        mBinding.retry.setVisibility(View.GONE);
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mBinding.progress.getRoot().setVisibility(View.GONE);
    }

    private void homeContent() {
        showProgress();
        setFabVisible(0);
        // 安全地隐藏空源提示
        try {
            if (mBinding != null && mBinding.emptySourceHint != null) {
                mBinding.emptySourceHint.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Logger.e("Error", e);
        }
        mAdapter.clear();
        mViewModel.homeContent();
        mBinding.pager.setAdapter(new PageAdapter(getChildFragmentManager()));
    }

    public Result getResult() {
        return mResult == null ? new Result() : mResult;
    }

    private void setLogo() {
        Config config = VodConfig.get().getConfig();
        if (config == null) return;
        Glide.with(App.get()).load(UrlUtil.convert(config.getLogo())).circleCrop().override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).error(R.drawable.ic_logo).listener(getListener()).into(mBinding.logo);
    }

    private RequestListener<Drawable> getListener() {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                mBinding.logo.getLayoutParams().width = ResUtil.dp2px(24);
                mBinding.logo.getLayoutParams().height = ResUtil.dp2px(24);
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                mBinding.logo.getLayoutParams().width = ResUtil.dp2px(36);
                mBinding.logo.getLayoutParams().height = ResUtil.dp2px(36);
                return false;
            }
        };
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case CONFIG:
                setLogo();
                break;
            case VIDEO:
            case SIZE:
                homeContent();
                break;
            case HISTORY:
                loadHistory();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStateEvent(StateEvent event) {
        switch (event.getType()) {
            case EMPTY:
                hideProgress();
                checkEmptySource(); // 添加检查是否显示空源提示
                break;
            case PROGRESS:
                showProgress();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCastEvent(CastEvent event) {
        ReceiveDialog.create().event(event).show(this);
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        homeContent();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void onItemClick(int position, Class item) {
        mBinding.pager.setCurrentItem(position);
        mAdapter.setActivated(position);
    }

    @Override
    public void setFilter(String key, Value value) {
        getFragment().setFilter(key, value);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        VideoActivity.file(getActivity(), FileChooser.getPathFromUri(getContext(), data.getData()));
    }

    @Override
    public boolean canBack() {
        if (mBinding.pager.getAdapter() == null) return true;
        if (mBinding.pager.getAdapter().getCount() == 0) return true;
        return getFragment().canBack();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && mBinding.pager.getAdapter() != null) {
            mBinding.pager.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.removeCallbacks(mRunnable);
        EventBus.getDefault().unregister(this);
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            return TypeFragment.newInstance(getSite().getKey(), type.getTypeId(), type.getStyle(), type.getExtend(true), "1".equals(type.getTypeFlag()));
        }

        @Override
        public int getCount() {
            return mAdapter.getItemCount();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
