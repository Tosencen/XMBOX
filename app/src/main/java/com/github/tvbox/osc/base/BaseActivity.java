package com.github.tvbox.osc.base;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD3LoadingUtils;
import com.github.tvbox.osc.util.Utils;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.bar.OnTitleBarListener;
import com.hjq.bar.TitleBar;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import me.jessyan.autosize.internal.CustomAdapt;

public abstract class BaseActivity extends AppCompatActivity implements CustomAdapt, OnTitleBarListener {
    protected Context mContext;
    private LoadService mLoadService;

    private ImmersionBar mImmersionBar;
    private TitleBar mTitleBar;
    private BasePopupView loadingPopup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 只有当前类重写了refresh方法才注册EventBus，避免不必要的注册
        if (shouldRegisterEventBus()) {
            EventBus.getDefault().register(this);
        }

        if (getLayoutResID()==-1){
            initVb();
        }else {
            setContentView(getLayoutResID());
        }
        mContext = this;
        AppManager.getInstance().addActivity(this);
        initStatusBar();
        initTitleBar();
        init();

        // 自动应用Material Symbols字体到所有使用该字体的TextView
        applyMaterialSymbolsToAllTextViews();

        if (!App.getInstance().isNormalStart){
            AppUtils.relaunchApp(true);
        }
    }

    /**
     * 判断是否需要注册EventBus
     * 通过反射检查当前类是否重写了refresh方法
     */
    private boolean shouldRegisterEventBus() {
        try {
            Class<?> clazz = this.getClass();
            return clazz.getMethod("refresh", RefreshEvent.class).getDeclaringClass() != BaseActivity.class;
        } catch (Exception e) {
            return false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {

    }


    private void initStatusBar(){
        ImmersionBar.with(this)
                .statusBarDarkFont(!Utils.isDarkTheme())
                .titleBar(findTitleBar(getWindow().getDecorView().findViewById(android.R.id.content)))
                .navigationBarColor(R.color.md3_surface)
                .init();
    }

    private void initTitleBar(){
        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
        }
    }

    /**
     * 递归获取 ViewGroup 中的 TitleBar 对象
     */
    private TitleBar findTitleBar(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View view = group.getChildAt(i);
            if ((view instanceof TitleBar)) {
                return (TitleBar) view;
            } else if (view instanceof ViewGroup) {
                TitleBar titleBar = findTitleBar((ViewGroup) view);
                if (titleBar != null) {
                    return titleBar;
                }
            }
        }
        return null;
    }

    private TitleBar getTitleBar() {
        if (mTitleBar == null) {
            mTitleBar = findTitleBar(getWindow().getDecorView().findViewById(android.R.id.content));
        }
        return mTitleBar;
    }


    public boolean hasPermission(String permission) {
        boolean has = true;
        try {
            has = PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return has;
    }

    protected abstract int getLayoutResID();

    protected abstract void init();

    protected void initVb() {

    }

    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                }
            });
        }
    }

    protected void setLoadSir(View view, Class<? extends Callback> emptyCallback) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, new Callback.OnReloadListener() {
                @Override
                public void onReload(View v) {
                }
            });
        }
    }

    protected void showLoading() {
        if (mLoadService != null) {
            mLoadService.showCallback(LoadingCallback.class);
        }
    }

    protected void showEmpty() {
        if (null != mLoadService) {
            mLoadService.showCallback(EmptyCallback.class);
        }
    }

    protected void showEmpty(Class<? extends Callback> emptyCallback) {
        if (null != mLoadService) {
            mLoadService.showCallback(emptyCallback);
        }
    }

    protected void showSuccess() {
        if (null != mLoadService) {
            mLoadService.showSuccess();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 只有注册了才需要取消注册
        if (shouldRegisterEventBus() && EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        // 清理加载对话框
        if (loadingPopup != null && loadingPopup.isShow()) {
            loadingPopup.dismiss();
        }
        loadingPopup = null;

        // 清理LoadService
        if (mLoadService != null) {
            mLoadService = null;
        }

        AppManager.getInstance().finishActivity(this);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz) {
        Intent intent = new Intent(mContext, clazz);
        startActivity(intent);
    }

    public void jumpActivity(Class<? extends BaseActivity> clazz, Bundle bundle) {
        if (DetailActivity.class.isAssignableFrom(clazz) && Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0) == 2) {
            //1.重新打开singleTask的页面(关闭小窗) 2.关闭画中画，重进detail再开启画中画会闪退
            ActivityUtils.finishActivity(DetailActivity.class);
        }
        Intent intent = new Intent(mContext, clazz);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    protected String getAssetText(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assets = getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assets.open(fileName)));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public float getSizeInDp() {
        return isBaseOnWidth() ? 360 : 720;
    }

    @Override
    public boolean isBaseOnWidth() {
        return true;
    }

    @Override
    public void onLeftClick(TitleBar titleBar) {
        finish();
    }


    /**
     * 显示加载框
     */
    public void showLoadingDialog() {
        if (loadingPopup == null) {
            loadingPopup = MD3LoadingUtils.createLoadingPopup(this);
        }
        loadingPopup.show();
    }

    /**
     * 隐藏加载框
     */
    public void dismissLoadingDialog() {
        if (loadingPopup != null && loadingPopup.isShow()) {
            loadingPopup.dismiss();
        }
    }

    /**
     * 自动应用Material Symbols字体到所有使用该字体的TextView
     * 递归查找视图层次结构中的所有TextView，检查是否使用了Material Symbols字体
     */
    protected void applyMaterialSymbolsToAllTextViews() {
        try {
            // 获取根视图
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            // 递归应用字体
            applyMaterialSymbolsToViewGroup((ViewGroup) rootView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 递归应用Material Symbols字体到ViewGroup中的所有TextView
     *
     * @param viewGroup 要处理的ViewGroup
     */
    private void applyMaterialSymbolsToViewGroup(ViewGroup viewGroup) {
        // 遍历ViewGroup中的所有子视图
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);

            // 如果是TextView，检查是否使用了Material Symbols字体
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                // 检查字体是否为Material Symbols
                if (isMaterialSymbolsFont(textView)) {
                    // 应用Material Symbols字体
                    com.github.tvbox.osc.util.MaterialSymbolsLoader.apply(textView);
                }
            }

            // 如果是ViewGroup，递归处理
            if (view instanceof ViewGroup) {
                applyMaterialSymbolsToViewGroup((ViewGroup) view);
            }
        }
    }

    /**
     * 检查TextView是否使用了Material Symbols字体
     *
     * @param textView 要检查的TextView
     * @return 是否使用了Material Symbols字体
     */
    private boolean isMaterialSymbolsFont(TextView textView) {
        try {
            // 检查字体名称
            String fontFamily = textView.getFontFeatureSettings();
            if (fontFamily != null && fontFamily.contains("material_symbols")) {
                return true;
            }

            // 检查XML中设置的fontFamily属性
            if (textView.getTypeface() != null) {
                String typefaceName = textView.getTypeface().toString().toLowerCase();
                if (typefaceName.contains("material") && typefaceName.contains("symbols")) {
                    return true;
                }
            }

            // 检查资源ID
            if (textView.getId() != View.NO_ID) {
                String resourceName = getResources().getResourceEntryName(textView.getId());
                // 常见的Material Symbols图标ID命名模式
                if (resourceName != null && (
                        resourceName.startsWith("ms_") ||
                        resourceName.startsWith("icon_") ||
                        resourceName.equals("tvSort") ||
                        resourceName.equals("tv_all_series"))) {
                    return true;
                }
            }

            // 检查文本内容是否是Unicode图标
            String text = textView.getText().toString();
            if (text.length() == 1) {
                char c = text.charAt(0);
                // Material Symbols图标的Unicode范围
                if (c >= '\uE000' && c <= '\uF000') {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}