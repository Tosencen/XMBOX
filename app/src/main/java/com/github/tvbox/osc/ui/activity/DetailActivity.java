package com.github.tvbox.osc.ui.activity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NotificationUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.ServiceUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.xmbox.app.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseVbActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.CastVideo;
import com.github.tvbox.osc.cast.CastManager;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.constant.IntentKey;
import com.xmbox.app.databinding.ActivityDetailBinding;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.receiver.BatteryReceiver;
import com.github.tvbox.osc.service.PlayService;
import com.github.tvbox.osc.ui.adapter.ParseAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesFlagAdapter;
import com.github.tvbox.osc.ui.dialog.AllVodSeriesBottomDialog;
import com.github.tvbox.osc.ui.dialog.AllVodSeriesRightDialog;
import com.github.tvbox.osc.ui.dialog.CastDeviceDialog;
import com.github.tvbox.osc.ui.dialog.QuickSearchDialog;
import com.github.tvbox.osc.ui.dialog.VideoDetailDialog;
import com.github.tvbox.osc.ui.fragment.PlayFragment;
import com.github.tvbox.osc.ui.widget.LinearSpacingItemDecoration;
import com.github.tvbox.osc.util.AppConstants;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.ScreenShotListenManager;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.util.SubtitleHelper;
import com.github.tvbox.osc.util.Utils;
import com.github.tvbox.osc.util.MD3DialogUtils;
import com.github.tvbox.osc.util.MD3ToastUtils;
import com.github.tvbox.osc.util.SpiderExceptionHandler;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.gyf.immersionbar.ImmersionBar;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.enums.PopupPosition;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */

public class DetailActivity extends BaseVbActivity<ActivityDetailBinding> {
    private PlayFragment playFragment = null;
    private SourceViewModel sourceViewModel;
    private Movie.Video mVideo;
    private VodInfo vodInfo;
    public SeriesFlagAdapter seriesFlagAdapter;
    public SeriesAdapter seriesAdapter;
    public String vodId;
    public String sourceKey;
    private View seriesFlagFocus = null;
    private boolean isReverse;
    private String preFlag = "";
    private HashMap<String, String> mCheckSources = null;
    BatteryReceiver mBatteryReceiver = new BatteryReceiver();
    //改为view模式无法自动响应返回键操作,onBackPress时手动dismiss
    private BasePopupView mAllSeriesRightDialog;
    private BasePopupView mAllSeriesBottomDialog;
    /**
     * Home键广播,用于触发后台服务
     */
    private BroadcastReceiver mHomeKeyReceiver;
    /**
     * 是否开启后台播放标记,不在广播开启,onPause根据标记开启
     */
    boolean openBackgroundPlay;
    private BroadcastReceiver mRemoteActionReceiver;

    /**
     * 截屏监听
     */
    ScreenShotListenManager screenShotListenManager;

    @Override
    protected void init() {
        initReceiver();
        initView();
        initViewModel();
        initData();
        registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        ImmersionBar.with(this)
                .statusBarColor(R.color.black)
                .navigationBarColor(R.color.md3_surface)
                .fitsSystemWindows(true)
                .statusBarDarkFont(false)
                .init();
        toggleScreenShotListen(true);
    }

    @Override
    public void initVb() {
        super.initVb();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundPlay = false;
        playServerSwitch(false);
        mBinding.ivPrivateBrowsing.postDelayed(NotificationUtils::cancelAll, 800);
    }

    private void initView() {
        // 调试：检查ViewBinding是否正确
        android.util.Log.d("DetailActivity", "initView 开始");
        android.util.Log.d("DetailActivity", "mBinding: " + (mBinding != null ? "不为null" : "为null"));
        android.util.Log.d("DetailActivity", "mBinding.ivBack: " + (mBinding.ivBack != null ? "不为null" : "为null"));

        // 创建返回操作的点击监听器
        View.OnClickListener backClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d("DetailActivity", "返回按钮被点击 - 层级分析");
                android.util.Log.d("DetailActivity", "点击的View ID: " + v.getId());
                android.util.Log.d("DetailActivity", "返回按钮位置层级：");
                android.util.Log.d("DetailActivity", "1. FrameLayout (根容器)");
                android.util.Log.d("DetailActivity", "   └── LinearLayout (主布局容器)");
                android.util.Log.d("DetailActivity", "       └── RelativeLayout (toolbar工具栏)");
                android.util.Log.d("DetailActivity", "           └── ImageView (iv_back返回按钮) ← 当前点击的按钮");
                android.util.Log.d("DetailActivity", "返回按钮属性：48dp x 48dp，左边距4dp，居中垂直对齐");
                finish();
            }
        };

        // 检查返回按钮是否存在
        if (mBinding.ivBack != null) {
            android.util.Log.d("DetailActivity", "设置返回按钮点击事件");
            android.util.Log.d("DetailActivity", "返回按钮可见性: " + (mBinding.ivBack.getVisibility() == View.VISIBLE ? "可见" : "不可见"));
            android.util.Log.d("DetailActivity", "返回按钮可点击: " + mBinding.ivBack.isClickable());
            android.util.Log.d("DetailActivity", "返回按钮可获得焦点: " + mBinding.ivBack.isFocusable());

            // 设置返回按钮点击事件
            mBinding.ivBack.setOnClickListener(backClickListener);

            // 确保按钮可点击
            mBinding.ivBack.setClickable(true);
            mBinding.ivBack.setFocusable(true);

            // 添加触摸事件监听器来调试
            mBinding.ivBack.setOnTouchListener((v, event) -> {
                android.util.Log.d("DetailActivity", "返回按钮触摸事件: " + event.getAction());
                return false; // 返回false让点击事件继续传递
            });

            // 移除padding扩展，使用原始的点击区域
            // 注释掉padding扩展，因为可能导致点击问题
            /*
            mBinding.ivBack.setPadding(
                mBinding.ivBack.getPaddingLeft() + 20,
                mBinding.ivBack.getPaddingTop() + 20,
                mBinding.ivBack.getPaddingRight() + 100, // 向右扩展更多
                mBinding.ivBack.getPaddingBottom() + 20
            );
            */

            // 检查父容器是否可能拦截触摸事件
            ViewGroup parent = (ViewGroup) mBinding.ivBack.getParent();
            if (parent != null) {
                android.util.Log.d("DetailActivity", "返回按钮父容器: " + parent.getClass().getSimpleName());
                android.util.Log.d("DetailActivity", "父容器可点击: " + parent.isClickable());
            }

            android.util.Log.d("DetailActivity", "返回按钮设置完成");
        } else {
            android.util.Log.e("DetailActivity", "返回按钮为null，无法设置点击事件");
        }

        // 设置标题点击返回功能
        if (mBinding.tvTitle != null) {
            android.util.Log.d("DetailActivity", "设置标题点击返回功能");
            mBinding.tvTitle.setOnClickListener(backClickListener);
        } else {
            android.util.Log.e("DetailActivity", "标题为null");
        }

        mBinding.ivPrivateBrowsing.setVisibility(Hawk.get(HawkConfig.PRIVATE_BROWSING, false) ? View.VISIBLE : View.GONE);
        mBinding.ivPrivateBrowsing.setOnClickListener(view -> MD3ToastUtils.showToast("当前为无痕浏览"));
        // 预览播放器控制
        if (showPreview) {
            mBinding.previewPlayer.setVisibility(View.VISIBLE);
        } else {
            mBinding.previewPlayer.setVisibility(View.GONE);
        }

        mBinding.mGridView.setHasFixedSize(true);
        mBinding.mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        mBinding.mGridView.addItemDecoration(new LinearSpacingItemDecoration(20, false));

        seriesAdapter = new SeriesAdapter(false);
        mBinding.mGridView.setAdapter(seriesAdapter);
        mBinding.mGridViewFlag.setHasFixedSize(true);
        seriesFlagAdapter = new SeriesFlagAdapter();
        mBinding.mGridViewFlag.setAdapter(seriesFlagAdapter);
        isReverse = false;
        preFlag = "";
        if (showPreview) {
            playFragment = new PlayFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.previewPlayer, playFragment).commit();
            getSupportFragmentManager().beginTransaction().show(playFragment).commitAllowingStateLoss();
        }

        // 点击标题区域显示详情对话框
        View.OnClickListener showDetailListener = view -> {
            new XPopup.Builder(this)
                    .isViewMode(true)
                    .hasNavigationBar(false)
                    .asCustom(new VideoDetailDialog(this, vodInfo))
                    .show();
        };
        // 点击标题或整个头部区域都可以显示详情
        mBinding.tvDetail.setOnClickListener(showDetailListener);

        // 设置详情按钮的父容器点击事件
        ViewParent detailParent = findViewById(R.id.tvDetail).getParent();
        if (detailParent instanceof View) {
            ((View) detailParent).setOnClickListener(showDetailListener);
        }

        // 设置下载按钮的点击事件
        findViewById(R.id.tvDownload).setOnClickListener(view -> use1DMDownload());

        // 设置下载按钮的父容器点击事件
        ViewParent downloadParent = findViewById(R.id.tvDownload).getParent();
        if (downloadParent instanceof View) {
            ((View) downloadParent).setOnClickListener(view -> use1DMDownload());
        }

        // 排序按钮
        mBinding.tvSort.setOnClickListener(v -> sortSeries());

        // 设置投屏按钮的点击事件 - 已禁用
        mBinding.tvCast.setOnClickListener(v -> {
            // showCastDialog();
            MD3ToastUtils.showToast(getString(R.string.detail_cast_disabled));
        });

        // 设置投屏按钮的父容器点击事件
        ViewParent castParent = findViewById(R.id.tvCast).getParent();
        if (castParent instanceof View) {
            ((View) castParent).setOnClickListener(v -> {
                // showCastDialog();
                MD3ToastUtils.showToast(getString(R.string.detail_cast_disabled));
            });
        }

        // 设置收藏按钮的点击事件
        mBinding.tvCollect.setOnClickListener(v -> toggleCollect());

        // 设置收藏按钮的父容器点击事件
        ViewParent collectParent = findViewById(R.id.tvCollect).getParent();
        if (collectParent instanceof View) {
            ((View) collectParent).setOnClickListener(v -> toggleCollect());
        }

        seriesFlagAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            chooseFlag(position);
        });

        seriesAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            chooseSeries(position, false);
        });

        mBinding.tvAllSeries.setOnClickListener(view -> {
            showAllSeriesDialog();
        });

        mBinding.tvSite.setOnClickListener(view -> {
            startQuickSearch();
            QuickSearchDialog quickSearchDialog = new QuickSearchDialog(DetailActivity.this);
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, quickSearchData));
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord));
            quickSearchDialog.show();
            if (pauseRunnable != null && pauseRunnable.size() > 0) {
                searchExecutorService = Executors.newFixedThreadPool(5);
                for (Runnable runnable : pauseRunnable) {
                    searchExecutorService.execute(runnable);
                }
                pauseRunnable.clear();
                pauseRunnable = null;
            }
            quickSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    try {
                        if (searchExecutorService != null) {
                            pauseRunnable = searchExecutorService.shutdownNow();
                            searchExecutorService = null;
                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                }
            });
        });
        mBinding.tvChangeLine.setOnClickListener(v -> {
            FastClickCheckUtil.check(v);
            quickLineChange();
        });
        // 设置加载状态视图
        setLoadSir(mBinding.getRoot());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (openBackgroundPlay) {
            playServerSwitch(true);
        }
    }

    private void initReceiver() {
        // 注册广播接收器
        if (mHomeKeyReceiver == null) {
            mHomeKeyReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action != null && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS) &&
                        playFragment != null && playFragment.getPlayer() != null) {
                        openBackgroundPlay = Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 1 &&
                                            playFragment.getPlayer().isPlaying();
                    }
                }
            };
            try {
                registerReceiver(mHomeKeyReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            } catch (Exception e) {
                e.printStackTrace();
                mHomeKeyReceiver = null;
            }
        }
    }

    /**
     * 排序(倒序)
     */
    public void sortSeries() {
        if (vodInfo != null && vodInfo.seriesMap.size() > 0) {
            vodInfo.reverseSort = !vodInfo.reverseSort;
            isReverse = !isReverse;
            vodInfo.reverse();
            vodInfo.playIndex = (vodInfo.seriesMap.get(vodInfo.playFlag).size() - 1) - vodInfo.playIndex;
//                    insertVod(sourceKey, vodInfo);

            seriesAdapter.notifyDataSetChanged();
        }
    }

    public void showCastDialog() {
        // 投屏功能已禁用
        MD3ToastUtils.showToast(getString(R.string.detail_cast_disabled));
        /*
        if (vodInfo == null || playFragment == null) {
            MD3ToastUtils.showLongToast(getString(R.string.detail_no_cast_content));
            return;
        }

        VodInfo.VodSeries vodSeries = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex);
        String url = TextUtils.isEmpty(playFragment.getFinalUrl()) ? vodSeries.url : playFragment.getFinalUrl();
        String title = mVideo.name + " " + vodSeries.name;

        // 创建投屏视频信息
        CastVideo castVideo = new CastVideo(title, url);

        // 显示投屏设备选择对话框
        new XPopup.Builder(this)
                .maxWidth(ConvertUtils.dp2px(360))
                .asCustom(new CastDeviceDialog(this, castVideo))
                .show();
        */
    }

    public void showAllSeriesDialog() {
        if (fullWindows) {
            mAllSeriesRightDialog = new XPopup.Builder(this)
                    .isViewMode(true)//隐藏导航栏(手势条)在dialog模式下会闪一下,改为view模式,但需处理onBackPress的隐藏,下方同理
                    .hasNavigationBar(false)
                    .popupHeight(ScreenUtils.getScreenHeight())
                    .popupWidth(ScreenUtils.getAppScreenWidth() / 2) // 设置宽度为屏幕的1/2
                    .popupPosition(PopupPosition.Right)
                    .enableDrag(false)//禁用拖拽,内部有横向rv
                    .asCustom(new AllVodSeriesRightDialog(this));
            mAllSeriesRightDialog.show();
        } else {
            mAllSeriesBottomDialog = new XPopup.Builder(this)
                    .isViewMode(true)
                    .hasNavigationBar(false)
                    .maxHeight(ScreenUtils.getScreenHeight() - (ScreenUtils.getScreenHeight() / 4))
                    .asCustom(new AllVodSeriesBottomDialog(this, seriesAdapter.getData(), (position, text) -> {
                        chooseSeries(position, false);
                    }));
            mAllSeriesBottomDialog.show();
        }
    }

    private void chooseFlag(int position) {
        //新选中的flag
        String newFlag = seriesFlagAdapter.getData().get(position).name;
        if (vodInfo != null && !vodInfo.playFlag.equals(newFlag)) {
            for (int i = 0; i < vodInfo.seriesFlags.size(); i++) {//遍历flag集合
                VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(i);
                if (flag.name.equals(vodInfo.playFlag)) {//取消当前播放的选中状态
                    flag.selected = false;
                    seriesFlagAdapter.notifyItemChanged(i);
                    break;
                }
            }
            //新选中的flag
            VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(position);
            flag.selected = true;
            //清除上一个线路集数的选中状态
            List<VodInfo.VodSeries> currentSeriesList = vodInfo.seriesMap.get(vodInfo.playFlag);
            if (currentSeriesList.size() > vodInfo.playIndex) {//有效集数
                currentSeriesList.get(vodInfo.playIndex).selected = false;
            }
            vodInfo.playFlag = newFlag;
            seriesFlagAdapter.notifyItemChanged(position);
            refreshList();
        }
    }

    private void chooseSeries(int position, boolean reloadWithChangeLine) {
        if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
            boolean reload = false;
            for (int j = 0; j < vodInfo.seriesMap.get(vodInfo.playFlag).size(); j++) {
                seriesAdapter.getData().get(j).selected = false;
                seriesAdapter.notifyItemChanged(j);
            }
            //解决倒叙不刷新
            if (vodInfo.playIndex != position) {
                seriesAdapter.getData().get(position).selected = true;
                seriesAdapter.notifyItemChanged(position);
                vodInfo.playIndex = position;

                reload = true;
            }
            //解决当前集不刷新的BUG
            if (!preFlag.isEmpty() && !vodInfo.playFlag.equals(preFlag)) {
                reload = true;
            }

            seriesAdapter.getData().get(vodInfo.playIndex).selected = true;
            seriesAdapter.notifyItemChanged(vodInfo.playIndex);

            //选集全屏 想选集不全屏的注释下面一行
            if (!showPreview || reload || reloadWithChangeLine) {
                jumpToPlay();
            }
        }
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    private List<Runnable> pauseRunnable = null;

    private void jumpToPlay() {
        if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
            preFlag = vodInfo.playFlag;
            //更新播放地址
            Bundle bundle = new Bundle();
            //保存历史
            insertVod(sourceKey, vodInfo);
            bundle.putString("sourceKey", sourceKey);
//            bundle.putSerializable("VodInfo", vodInfo);
            App.getInstance().setVodInfo(vodInfo);
            if (previewVodInfo == null) {
                // 使用Gson进行深拷贝，比序列化更高效
                try {
                    Gson gson = new Gson();
                    String json = gson.toJson(vodInfo);
                    previewVodInfo = gson.fromJson(json, VodInfo.class);
                } catch (Exception e) {
                    LogUtils.e("深拷贝VodInfo失败", e);
                    // 如果深拷贝失败，直接使用原对象
                    previewVodInfo = vodInfo;
                }
            }
            if (previewVodInfo != null) {
                previewVodInfo.playerCfg = vodInfo.playerCfg;
                previewVodInfo.playFlag = vodInfo.playFlag;
                previewVodInfo.playIndex = vodInfo.playIndex;
                previewVodInfo.seriesMap = vodInfo.seriesMap;
//                    bundle.putSerializable("VodInfo", previewVodInfo);
                App.getInstance().setVodInfo(previewVodInfo);
            }
            playFragment.setData(bundle);

            //定位选集
            mBinding.mGridView.scrollToPosition(vodInfo.playIndex);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    void refreshList() {
        int seriesSize = vodInfo.seriesMap.get(vodInfo.playFlag).size();
        if (seriesSize > 0 && seriesSize <= vodInfo.playIndex) {//当前集数大于新选线路的总集数,设置为最后一集
            vodInfo.playIndex = seriesSize - 1;
        }

        if (vodInfo.seriesMap.get(vodInfo.playFlag) != null) {
            boolean canSelect = true;
            for (int j = 0; j < vodInfo.seriesMap.get(vodInfo.playFlag).size(); j++) {
                if (vodInfo.seriesMap.get(vodInfo.playFlag).get(j).selected) {
                    canSelect = false;
                    break;
                }
            }
            if (canSelect)
                vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).selected = true;
        }
        seriesAdapter.setNewData(vodInfo.seriesMap.get(vodInfo.playFlag));

    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.detailResult.observe(this, new Observer<AbsXml>() {
            @Override
            public void onChanged(AbsXml absXml) {
                if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
                    showSuccess();
                    mVideo = absXml.movie.videoList.get(0);
                    vodInfo = new VodInfo();
                    vodInfo.setVideo(mVideo);
                    vodInfo.sourceKey = mVideo.sourceKey;

                    // 设置标题和来源
                    mBinding.tvTitle.setText(mVideo.name);
                    mBinding.tvVideoTitle.setText(mVideo.name);
                    String srcName = ApiConfig.get().getSource(mVideo.sourceKey).getName();
                    mBinding.tvSite.setText(getString(R.string.detail_source_prefix) +
                        (TextUtils.isEmpty(srcName) ? getString(R.string.detail_source_unknown) : srcName));

                    if (vodInfo.seriesMap != null && vodInfo.seriesMap.size() > 0) {//线路
                        mBinding.mGridViewFlag.setVisibility(View.VISIBLE);
                        mBinding.mGridView.setVisibility(View.VISIBLE);
                        mBinding.mEmptyPlaylist.setVisibility(View.GONE);

                        VodInfo vodInfoRecord = RoomDataManger.getVodInfo(sourceKey, vodId);
                        // 读取历史记录
                        if (vodInfoRecord != null) {
                            vodInfo.playIndex = Math.max(vodInfoRecord.playIndex, 0);
                            vodInfo.playFlag = vodInfoRecord.playFlag;
                            vodInfo.playerCfg = vodInfoRecord.playerCfg;
                            vodInfo.reverseSort = vodInfoRecord.reverseSort;
                        } else {
                            vodInfo.playIndex = 0;
                            vodInfo.playFlag = null;
                            vodInfo.playerCfg = "";
                            vodInfo.reverseSort = false;
                        }

                        if (vodInfo.reverseSort) {
                            vodInfo.reverse();
                        }

                        if (vodInfo.playFlag == null || !vodInfo.seriesMap.containsKey(vodInfo.playFlag))
                            vodInfo.playFlag = (String) vodInfo.seriesMap.keySet().toArray()[0];

                        int flagScrollTo = 0;
                        for (int j = 0; j < vodInfo.seriesFlags.size(); j++) {
                            VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(j);
                            if (flag.name.equals(vodInfo.playFlag)) {
                                flagScrollTo = j;
                                flag.selected = true;
                            } else
                                flag.selected = false;
                        }
//                        setTextShow(tvPlayUrl, "播放地址：", vodInfo.seriesMap.get(vodInfo.playFlag).get(0).url);
                        //设置线路数据
                        seriesFlagAdapter.setNewData(vodInfo.seriesFlags);
                        mBinding.mGridViewFlag.scrollToPosition(flagScrollTo);

                        refreshList();
                        if (showPreview) {
                            jumpToPlay();
                            mBinding.previewPlayer.setVisibility(View.VISIBLE);
                            toggleSubtitleTextSize();
                        }
                        // startQuickSearch();
                    } else {//空布局
                        mBinding.mGridViewFlag.setVisibility(View.GONE);
                        mBinding.mGridView.setVisibility(View.GONE);
                        mBinding.mEmptyPlaylist.setVisibility(View.VISIBLE);
                    }
                } else {
                    showEmpty();
                    mBinding.previewPlayer.setVisibility(View.GONE);
                }
            }
        });
    }

    private String getHtml(String label, String content) {
        if (content == null) {
            content = "";
        }
        return label + "<font color=\"#FFFFFF\">" + content + "</font>";
    }

    private void initData() {
        try {
            Intent intent = getIntent();
            if (intent != null && intent.getExtras() != null) {
                Bundle bundle = intent.getExtras();
                loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""));
            }
        } catch (Exception e) {
            android.util.Log.e("DetailActivity", "initData发生异常: " + e.getMessage());
            e.printStackTrace();
            // 显示错误提示并关闭Activity
            MD3ToastUtils.showToast("加载详情失败，请重试");
            finish();
        }
    }

    private void loadDetail(String vid, String key) {
        if (vid != null) {
            try {
                vodId = vid;
                sourceKey = key;
                showLoading();

                // 添加spider初始化的安全检查
                android.util.Log.d("DetailActivity", "开始加载详情，sourceKey: " + sourceKey + ", vodId: " + vodId);

                // 使用SpiderExceptionHandler安全执行spider相关操作
                SpiderExceptionHandler.safeExecute(() -> {
                    sourceViewModel.getDetail(sourceKey, vodId);
                }, "加载视频详情失败");
                boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
                // 找到收藏图标
                TextView favoriteIcon = (TextView) ((ViewGroup) mBinding.tvCollect.getParent()).getChildAt(0);

                if (isVodCollect) {
                    mBinding.tvCollect.setText(getString(R.string.detail_remove_from_favorites));
                    // 设置为填充图标颜色
                    favoriteIcon.setTextColor(getResources().getColor(R.color.md3_primary));
                } else {
                    mBinding.tvCollect.setText(getString(R.string.detail_add_to_favorites));
                    // 设置为线框图标颜色
                    favoriteIcon.setTextColor(getResources().getColor(R.color.text_foreground));
                }
            } catch (Exception e) {
                android.util.Log.e("DetailActivity", "loadDetail发生异常: " + e.getMessage());
                e.printStackTrace();
                showEmpty();
                MD3ToastUtils.showToast("加载详情失败: " + e.getMessage());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_REFRESH) {
            if (event.obj != null) {
                if (event.obj instanceof Integer) {
                    int index = (int) event.obj;
                    for (int j = 0; j < vodInfo.seriesMap.get(vodInfo.playFlag).size(); j++) {
                        seriesAdapter.getData().get(j).selected = false;
                        seriesAdapter.notifyItemChanged(j);
                    }
                    seriesAdapter.getData().get(index).selected = true;
                    seriesAdapter.notifyItemChanged(index);
                    //mBinding.mGridView.setSelection(index);
                    vodInfo.playIndex = index;

                    // 更新当前播放标题
                    String videoTitle = mVideo.name;
                    try {
                        String episodeName = vodInfo.seriesMap.get(vodInfo.playFlag).get(index).name;
                        videoTitle = mVideo.name + " " + episodeName;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mBinding.tvVideoTitle.setText(videoTitle);

                    //保存历史
                    insertVod(sourceKey, vodInfo);
                } else if (event.obj instanceof JSONObject) {
                    vodInfo.playerCfg = ((JSONObject) event.obj).toString();
                    //保存历史
                    insertVod(sourceKey, vodInfo);
                }

            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
            if (event.obj != null) {
                Movie.Video video = (Movie.Video) event.obj;
                loadDetail(video.id, video.sourceKey);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD_CHANGE) {
            if (event.obj != null) {
                String word = (String) event.obj;
                switchSearchWord(word);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private String searchTitle = "";
    private boolean hadQuickStart = false;
    private final List<Movie.Video> quickSearchData = new ArrayList<>();
    private final List<String> quickSearchWord = new ArrayList<>();
    private ExecutorService searchExecutorService = null;

    private void switchSearchWord(String word) {
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchData.clear();
        searchTitle = word;
        searchResult();
    }

    private void startQuickSearch() {
        initCheckedSourcesForSearch();
        if (hadQuickStart)
            return;
        hadQuickStart = true;
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchWord.clear();
        searchTitle = mVideo.name;
        quickSearchData.clear();
        quickSearchWord.addAll(SearchHelper.splitWords(searchTitle));
        // 分词
        OkGo.<String>get(String.format(getString(R.string.detail_fenci_api_url), URLEncoder.encode(searchTitle)))
                .tag("fenci")
                .execute(new AbsCallback<String>() {
                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        if (response.body() != null) {
                            return response.body().string();
                        } else {
                            throw new IllegalStateException("网络请求错误");
                        }
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        String json = response.body();
                        try {
                            for (JsonElement je : new Gson().fromJson(json, JsonArray.class)) {
                                quickSearchWord.add(je.getAsJsonObject().get("t").getAsString());
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                        List<String> words = new ArrayList<>(new HashSet<>(quickSearchWord));
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, words));
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });

        searchResult();
    }

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable() || !bean.isQuickSearch()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
        }
        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    sourceViewModel.getQuickSearch(key, searchTitle);
                }
            });
        }
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                // 去除当前相同的影片
                if (video.sourceKey.equals(sourceKey) && video.id.equals(vodId))
                    continue;
                data.add(video);
            }
            quickSearchData.addAll(data);
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, data));
        }
    }

    private void insertVod(String sourceKey, VodInfo vodInfo) {
        if (Hawk.get(HawkConfig.PRIVATE_BROWSING, false)) {//无痕浏览
            return;
        }
        try {
            vodInfo.playNote = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).name;
        } catch (Throwable th) {
            vodInfo.playNote = "";
        }
        RoomDataManger.insertVodRecord(sourceKey, vodInfo);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
    }

    @Override
    protected void onDestroy() {
        // 记录销毁前的内存状态
        try {
            com.github.tvbox.osc.util.MemoryOptimizer.monitorMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 先停止所有异步操作
        registerActionReceiver(false);

        // 清理ViewModel观察者
        if (sourceViewModel != null) {
            try {
                sourceViewModel.detailResult.removeObservers(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 注销广播接收器
        try {
            if (mBatteryReceiver != null) {
                unregisterReceiver(mBatteryReceiver);
                mBatteryReceiver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 注销Home键广播接收器
        if (mHomeKeyReceiver != null) {
            try {
                unregisterReceiver(mHomeKeyReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mHomeKeyReceiver = null;
        }

        // 关闭线程池
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // 取消网络请求
        try {
            OkGo.getInstance().cancelTag("fenci");
            OkGo.getInstance().cancelTag("detail");
            OkGo.getInstance().cancelTag("quick_search");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 停止截屏监听
        toggleScreenShotListen(false);

        // 清理对话框引用
        if (mAllSeriesRightDialog != null) {
            try {
                if (mAllSeriesRightDialog.isShow()) {
                    mAllSeriesRightDialog.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mAllSeriesRightDialog = null;
        }

        if (mAllSeriesBottomDialog != null) {
            try {
                if (mAllSeriesBottomDialog.isShow()) {
                    mAllSeriesBottomDialog.dismiss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mAllSeriesBottomDialog = null;
        }

        // 清理RecyclerView和适配器引用
        try {
            if (mBinding != null) {
                if (mBinding.mGridView != null) {
                    mBinding.mGridView.setAdapter(null);
                }
                if (mBinding.mGridViewFlag != null) {
                    mBinding.mGridViewFlag.setAdapter(null);
                }
                if (mBinding.rvParse != null) {
                    mBinding.rvParse.setAdapter(null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 清理适配器引用
        if (seriesFlagAdapter != null) {
            try {
                seriesFlagAdapter.setNewData(null);
                seriesFlagAdapter.setOnItemClickListener(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            seriesFlagAdapter = null;
        }

        if (seriesAdapter != null) {
            try {
                seriesAdapter.setNewData(null);
                seriesAdapter.setOnItemClickListener(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            seriesAdapter = null;
        }

        // 清理Fragment引用
        if (playFragment != null) {
            try {
                // 如果Fragment还在，先移除
                if (getSupportFragmentManager().findFragmentById(R.id.previewPlayer) != null) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(playFragment)
                            .commitAllowingStateLoss();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            playFragment = null;
        }

        // 清理其他引用
        mVideo = null;
        vodInfo = null;
        previewVodInfo = null;
        mCheckSources = null;
        pauseRunnable = null;
        seriesFlagFocus = null;

        // 清理集合（final变量只能清空，不能设为null）
        try {
            quickSearchWord.clear();
            quickSearchData.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 清理App中的vodInfo引用
        try {
            App.getInstance().clearVodInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 清理ViewModel引用
        sourceViewModel = null;

        // 调用父类的onDestroy
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        android.util.Log.d("DetailActivity", "onBackPressed 被调用");
        if (mAllSeriesRightDialog != null && mAllSeriesRightDialog.isShow()) {
            mAllSeriesRightDialog.dismiss();
            return;
        }
        if (mAllSeriesBottomDialog != null && mAllSeriesBottomDialog.isShow()) {
            mAllSeriesBottomDialog.dismiss();
            return;
        }
        // 检查 playFragment 是否为 null
        if (playFragment != null && playFragment.hideAllDialogSuccess()) {//fragment有弹窗隐藏并拦截返回
            return;
        }
        if (fullWindows) {
            toggleFullPreview();
            mBinding.mGridView.requestFocus();
            return;
        }
        android.util.Log.d("DetailActivity", "调用 super.onBackPressed()");
        super.onBackPressed();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && playFragment != null && fullWindows) {
            if (playFragment.dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // preview
    VodInfo previewVodInfo = null;
    boolean showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true);
    ; // true 开启 false 关闭
    boolean fullWindows = false;
    ViewGroup.LayoutParams windowsPreview = null;
    ViewGroup.LayoutParams windowsFull = null;

    public void toggleFullPreview() {
        if (windowsPreview == null) {
            windowsPreview = mBinding.previewPlayer.getLayoutParams();
        }
        if (windowsFull == null) {//全屏尺寸
            windowsFull = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        fullWindows = !fullWindows;

        //交由fragment处理播放器全屏逻辑
        playFragment.changedLandscape(fullWindows);
        //activity处理预览尺寸(全屏/非全屏预览)
        mBinding.previewPlayer.setLayoutParams(fullWindows ? windowsFull : windowsPreview);
        mBinding.mGridView.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        mBinding.mGridViewFlag.setVisibility(fullWindows ? View.GONE : View.VISIBLE);

        //全屏下禁用详情页几个按键的焦点 防止上键跑过来
        mBinding.tvSort.setFocusable(!fullWindows);
        mBinding.tvCollect.setFocusable(!fullWindows);
        toggleSubtitleTextSize();
    }

    void toggleSubtitleTextSize() {
        int subtitleTextSize = SubtitleHelper.getTextSize(this);
        if (!fullWindows) {
            subtitleTextSize *= 0.6;
        }
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE, subtitleTextSize));
    }

    public void use1DMDownload() {
        if (vodInfo != null && vodInfo.seriesMap.get(vodInfo.playFlag).size() > 0) {
            VodInfo.VodSeries vod = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex);
            String url = TextUtils.isEmpty(playFragment.getFinalUrl()) ? vod.url : playFragment.getFinalUrl();
            // 创建Intent对象，启动1DM App
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setDataAndType(Uri.parse(url), "video/mp4");
            intent.putExtra("title", vodInfo.name + " " + vod.name); // 传入文件保存名
//            intent.setClassName("idm.internet.download.manager.plus", "idm.internet.download.manager.MainActivity");
            intent.setClassName(AppConstants.ExternalApps.PKG_1DM, AppConstants.ExternalApps.CLS_1DM_DOWNLOADER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 检查1DM App是否已安装
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                startActivity(intent); // 启动1DM App
            } else {
                // 如果1DM App未安装，提示用户安装1DM App
                MD3DialogUtils.showAlertDialog(
                    this,
                    getString(R.string.detail_install_1dm_title),
                    getString(R.string.detail_install_1dm_message),
                    getString(R.string.detail_install_1dm_positive),
                    getString(R.string.detail_install_1dm_negative),
                    (dialog, which) -> {
                        // 跳转到下载链接
                        Intent downloadIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.detail_1dm_download_url)));
                        startActivity(downloadIntent);
                    },
                    null
                );
            }
        } else {
            MD3ToastUtils.showToast(getString(R.string.detail_resource_error));
        }
    }

    /**
     * 画中画模式
     */
    public void enterPip() {
        if (Utils.supportsPiPMode()) {
            // 创建一个Intent对象，模拟按下Home键
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);

            // Calculate Video Resolution
            int vWidth = playFragment.getPlayer().getVideoSize()[0];
            int vHeight = playFragment.getPlayer().getVideoSize()[1];
            Rational ratio;
            if (vWidth != 0) {
                if ((((double) vWidth) / ((double) vHeight)) > 2.39) {
                    vHeight = (int) (((double) vWidth) / 2.35);
                }
                ratio = new Rational(vWidth, vHeight);
            } else {
                ratio = new Rational(16, 9);
            }
            List<RemoteAction> actions = new ArrayList<>();
            actions.add(generateRemoteAction(android.R.drawable.ic_media_previous, IntentKey.BROADCAST_ACTION_PREV, "Prev", "Play Previous"));
            actions.add(generateRemoteAction(android.R.drawable.ic_media_play, IntentKey.BROADCAST_ACTION_PLAYPAUSE, "Play", "Play/Pause"));
            actions.add(generateRemoteAction(android.R.drawable.ic_media_next, IntentKey.BROADCAST_ACTION_NEXT, "Next", "Play Next"));
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(ratio)
                    .setActions(actions).build();
            playFragment.getPlayer().postDelayed(() -> {//代码模拟home键时会立即执行,toggleFullPreview中竖屏有切换横屏操作,
                if (!fullWindows) {
                    toggleFullPreview();
                }
            }, 300);
            enterPictureInPictureMode(params);
            playFragment.getController().hideBottom();

            playFragment.getPlayer().postDelayed(() -> {
                if (!playFragment.getPlayer().isPlaying()) {
                    playFragment.getController().togglePlay();
                }
            }, 400);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private RemoteAction generateRemoteAction(int iconResId, int actionCode, String title, String desc) {
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 对于Android 12及以上版本，添加FLAG_IMMUTABLE标志
            flags = PendingIntent.FLAG_IMMUTABLE;
        }
        final PendingIntent intent =
                PendingIntent.getBroadcast(
                        DetailActivity.this,
                        actionCode,
                        new Intent(IntentKey.BROADCAST_ACTION).putExtra("action", actionCode),
                        flags);
        final Icon icon = Icon.createWithResource(DetailActivity.this, iconResId);
        return (new RemoteAction(icon, title, desc, intent));
    }

    /**
     * 事件接收广播(画中画/后台播放点击事件)
     * @param isRegister 注册/注销
     */
    private void registerActionReceiver(boolean isRegister) {
        if (isRegister) {
            mRemoteActionReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !intent.getAction().equals(IntentKey.BROADCAST_ACTION) ||
                        playFragment == null || playFragment.getController() == null) {
                        return;
                    }

                    int currentStatus = intent.getIntExtra("action", 1);
                    if (currentStatus == IntentKey.BROADCAST_ACTION_PREV) {
                        playFragment.playPrevious();
                    } else if (currentStatus == IntentKey.BROADCAST_ACTION_PLAYPAUSE) {
                        playFragment.getController().togglePlay();
                    } else if (currentStatus == IntentKey.BROADCAST_ACTION_NEXT) {
                        playFragment.playNext(false);
                    } else if (currentStatus == IntentKey.BROADCAST_ACTION_CLOSE) {
                        playServerSwitch(false);
                        finish();
                        NotificationUtils.cancelAll();
                    }
                }
            };
            try {
                registerReceiver(mRemoteActionReceiver, new IntentFilter(IntentKey.BROADCAST_ACTION));
            } catch (Exception e) {
                e.printStackTrace();
                mRemoteActionReceiver = null;
            }
        } else {
            if (mRemoteActionReceiver != null) {
                try {
                    unregisterReceiver(mRemoteActionReceiver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mRemoteActionReceiver = null;
            }
            if (playFragment != null && playFragment.getPlayer() != null && playFragment.getPlayer().isPlaying()) {
                // 退出画中画时,暂停播放(画中画的全屏也会触发,但全屏后会自动播放)
                playFragment.getController().togglePlay();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        registerActionReceiver(Utils.supportsPiPMode() && isInPictureInPictureMode);
    }

    /**
     * 后台播放服务开关,开启时注册操作广播,关闭时注销
     */
    private void playServerSwitch(boolean open) {
        if (open) {
            if (vodInfo != null && vodInfo.seriesMap != null && vodInfo.seriesMap.containsKey(vodInfo.playFlag) &&
                playFragment != null && playFragment.getPlayer() != null) {
                try {
                    VodInfo.VodSeries vod = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex);
                    PlayService.start(playFragment.getPlayer(), vodInfo.name + "&&" + vod.name);
                    registerActionReceiver(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (ServiceUtils.isServiceRunning(PlayService.class)) {
                PlayService.stop();
                registerActionReceiver(false);
            }
        }
    }

    public String getCurrentVodUrl() {
        return playFragment == null ? "" : playFragment.getFinalUrl();
    }

    public void quickLineChange() {
        List<VodInfo.VodSeriesFlag> flags = seriesFlagAdapter.getData();
        if (flags.size() > 1) {
            int currentIndex = 0;
            for (int i = 0; i < flags.size(); i++) {
                if (flags.get(i).selected) {
                    currentIndex = i;
                }
            }
            currentIndex += 1;
            if (currentIndex >= flags.size()) {
                currentIndex = 0;
            }
            mBinding.mGridViewFlag.smoothScrollToPosition(currentIndex);
            chooseFlag(currentIndex);
            mBinding.mGridView.postDelayed(() -> chooseSeries(vodInfo.playIndex, true), 300);
        }
    }

    public void showParseRoot(boolean show, ParseAdapter adapter) {
        mBinding.rvParse.setAdapter(adapter);
        int defaultIndex = 0;
        for (int i = 0; i < adapter.getData().size(); i++) {
            if (adapter.getData().get(i).isDefault()) {
                defaultIndex = i;
                break;
            }
        }
        if (defaultIndex != 0) {
            mBinding.rvParse.scrollToPosition(defaultIndex);
        }
        mBinding.parseRoot.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * 切换收藏状态
     */
    public void toggleCollect() {
        if (vodInfo != null) {
            boolean isVodCollect = RoomDataManger.isVodCollect(sourceKey, vodId);
            // 找到收藏图标
            TextView favoriteIcon = (TextView) ((ViewGroup) mBinding.tvCollect.getParent()).getChildAt(0);

            if (isVodCollect) {
                // 使用vodInfo对象而不是vodId
                RoomDataManger.deleteVodCollect(sourceKey, vodInfo);
                mBinding.tvCollect.setText(getString(R.string.detail_add_to_favorites));
                // 设置为线框图标
                favoriteIcon.setTextColor(getResources().getColor(R.color.text_foreground));
                MD3ToastUtils.showToast(getString(R.string.detail_removed_from_favorites));
            } else {
                RoomDataManger.insertVodCollect(sourceKey, vodInfo);
                mBinding.tvCollect.setText(getString(R.string.detail_remove_from_favorites));
                // 设置为填充图标
                favoriteIcon.setTextColor(getResources().getColor(R.color.md3_primary));
                MD3ToastUtils.showToast(getString(R.string.detail_added_to_favorites));
            }
        }
    }

    private void toggleScreenShotListen(boolean open) {
        if (open) {
            if (screenShotListenManager == null) {
                screenShotListenManager = ScreenShotListenManager.newInstance(this);
            }
            screenShotListenManager.setListener(imagePath -> {
                // 避免在播放状态下弹出对话框
                if (playFragment != null && playFragment.getPlayer() != null && playFragment.getPlayer().isInPlaybackState()) {
                    return;
                }

                new XPopup.Builder(this)
                        .isDarkTheme(Utils.isDarkTheme())
                        .asCenterList("", new String[]{"跳转阿狸", "跳转优汐", "跳转夸父", "关闭"}, null, (position, text) -> {
                            String pkg = "";
                            String cls = "";
                            switch (position) {
                                case 0:
                                    pkg = AppConstants.ExternalApps.PKG_ALI_CLOUD;
                                    cls = AppConstants.ExternalApps.CLS_ALI_CLOUD_SPLASH;
                                    break;
                                case 1:
                                    pkg = AppConstants.ExternalApps.PKG_UC_BROWSER;
                                    cls = AppConstants.ExternalApps.CLS_UC_BROWSER_MAIN;
                                    break;
                                case 2:
                                    pkg = AppConstants.ExternalApps.PKG_QUARK_BROWSER;
                                    cls = AppConstants.ExternalApps.CLS_QUARK_BROWSER_MAIN;
                                    break;
                                case 3:
                                    return;
                            }
                            try {
                                startActivity(new Intent().setComponent(new ComponentName(pkg, cls)));
                            } catch (Exception e) {
                                MD3ToastUtils.showToast(getString(R.string.detail_app_not_found));
                            }
                        })
                        .show();
            });
            screenShotListenManager.startListen();
        } else {
            if (screenShotListenManager != null) {
                screenShotListenManager.stopListen();
                screenShotListenManager = null; // 释放引用
            }
        }
    }
}
