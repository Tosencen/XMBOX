package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.xmbox.app.R;
import com.github.tvbox.osc.util.Utils;
import com.lxj.xpopup.core.BottomPopupView;

import java.util.ArrayList;
import java.util.List;

/**
 * 版本历史弹窗
 */
public class VersionHistoryDialog extends BottomPopupView {

    public VersionHistoryDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.dialog_version_history;
    }

    @Override
    protected void beforeShow() {
        super.beforeShow();
        // 在beforeShow中设置背景，确保在显示前应用
        View rootView = getPopupImplView();
        if (Utils.isDarkTheme()) {
            rootView.setBackgroundResource(R.drawable.bg_dialog_dark);
        } else {
            rootView.setBackgroundResource(R.drawable.bg_dialog_md3);
        }
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        View rootView = getPopupImplView();

        // 设置标题
        TextView tvTitle = rootView.findViewById(R.id.tv_title);
        tvTitle.setText("版本更新历史");

        // 设置RecyclerView
        RecyclerView rvVersions = rootView.findViewById(R.id.rv_versions);
        rvVersions.setLayoutManager(new LinearLayoutManager(getContext()));

        // 创建数据
        List<VersionInfo> versionInfoList = getVersionHistory();

        // 设置适配器
        VersionHistoryAdapter adapter = new VersionHistoryAdapter();
        rvVersions.setAdapter(adapter);
        adapter.setNewData(versionInfoList);
    }

    /**
     * 获取版本历史数据
     */
    private List<VersionInfo> getVersionHistory() {
        List<VersionInfo> versionInfoList = new ArrayList<>();

        // v2.0.8
        VersionInfo v208 = new VersionInfo();
        v208.setVersion("v2.0.8");
        v208.setDate("2025-01-20");
        List<String> features208 = new ArrayList<>();
        features208.add("🎯 修复了切换数据源后仍显示\"上次看到\"弹窗的问题");
        features208.add("🚀 借鉴TVBoxOS-Mobile策略，优化数据源切换流程");
        features208.add("✨ 禁用数据源切换时的页面动画，直接显示新内容");
        features208.add("🧠 全面重构内存管理，添加简化版内存管理器");
        features208.add("🔧 修复多个内存泄漏问题，提升应用稳定性");
        features208.add("📱 优化Activity生命周期管理，减少内存占用");
        features208.add("⚡ 改进应用启动和切换性能");
        features208.add("🎨 优化UI响应速度和流畅度");
        features208.add("🔒 增强应用安全性和稳定性");
        features208.add("📺 改进视频播放体验和稳定性");
        v208.setFeatures(features208);
        versionInfoList.add(v208);

        // v2.0.7
        VersionInfo v207 = new VersionInfo();
        v207.setVersion("v2.0.7");
        v207.setDate("2025-01-15");
        List<String> features207 = new ArrayList<>();
        features207.add("🛠️ 修复了LiveActivity中的多个内存泄漏问题");
        features207.add("🔄 优化了订阅源切换后首页数据更新逻辑");
        features207.add("💾 添加了综合内存管理器，自动检测和修复内存泄漏");
        features207.add("🎮 改进了播放器相关的内存管理");
        features207.add("📊 添加了内存使用监控和报告功能");
        features207.add("🧹 增强了WebView、Handler、BroadcastReceiver的清理机制");
        features207.add("⚙️ 优化了应用生命周期管理");
        features207.add("🚀 提升应用整体性能和响应速度");
        features207.add("🔧 修复了多个潜在的崩溃问题");
        features207.add("🎯 优化数据源管理和加载机制");
        v207.setFeatures(features207);
        versionInfoList.add(v207);

        // v2.0.6
        VersionInfo v206 = new VersionInfo();
        v206.setVersion("v2.0.6");
        v206.setDate("2025-05-28");
        List<String> features206 = new ArrayList<>();
        features206.add("修复了DetailActivity中的NullPointerException崩溃问题");
        features206.add("修复了视频详情页返回按钮点击无效的问题");
        features206.add("在我的页面右上角添加了GitHub图标，点击可跳转到GitHub主页");
        features206.add("优化了spider异常处理，提升应用稳定性");
        features206.add("改进了Context管理，避免Activity销毁导致的空指针异常");
        features206.add("添加了详细的调试日志，便于问题定位");
        v206.setFeatures(features206);
        versionInfoList.add(v206);

        // v2.0.5
        VersionInfo v205 = new VersionInfo();
        v205.setVersion("v2.0.5");
        v205.setDate("2025-04-26");
        List<String> features205 = new ArrayList<>();
        features205.add("优化了更新弹窗UI，移除了背景描边");
        features205.add("调整了更新弹窗中版本号文本的位置");
        features205.add("点击我的页面的XMBOX标题或版本号可查看版本历史");
        v205.setFeatures(features205);
        versionInfoList.add(v205);

        // v2.0.4
        VersionInfo v204 = new VersionInfo();
        v204.setVersion("v2.0.4");
        v204.setDate("2025-04-25");
        List<String> features204 = new ArrayList<>();
        features204.add("在我的页面添加了版本号显示，位于XMBOX标题下方");
        features204.add("添加了应用更新过程中的取消功能，用户可以在下载过程中取消更新");
        features204.add("在首页添加了上次看到提示功能，显示用户最近观看的内容");
        features204.add("优化了白天模式下上次看到提示弹窗的背景色，提高可读性");
        features204.add("改进了更新弹窗UI，使其符合Material Design 3规范");
        features204.add("优化了应用更新流程，动态显示目标版本号");
        features204.add("修复了部分设备上订阅源无法正确加载的问题");
        v204.setFeatures(features204);
        versionInfoList.add(v204);

        // v2.0.3
        VersionInfo v203 = new VersionInfo();
        v203.setVersion("v2.0.3");
        v203.setDate("2025-03-15");
        List<String> features203 = new ArrayList<>();
        features203.add("添加了对中文域名订阅源的支持");
        features203.add("新增了直播源处理功能，支持更多直播源格式");
        features203.add("添加了检查更新功能的新版本提醒标识");
        features203.add("优化了视频播放器导航栏，现在会在进入页面后3秒自动隐藏");
        features203.add("改进了订阅管理页面的UI设计，悬浮按钮移至右侧");
        features203.add("修复了切换订阅源后返回键行为异常的问题");
        v203.setFeatures(features203);
        versionInfoList.add(v203);

        // v2.0.2
        VersionInfo v202 = new VersionInfo();
        v202.setVersion("v2.0.2");
        v202.setDate("2025-02-20");
        List<String> features202 = new ArrayList<>();
        features202.add("添加了Material Design 3风格的UI设计");
        features202.add("新增了广告过滤功能");
        features202.add("添加了IJK播放器缓存功能");
        features202.add("优化了应用启动速度");
        features202.add("修复了某些情况下应用崩溃的问题");
        v202.setFeatures(features202);
        versionInfoList.add(v202);

        // v2.0.1
        VersionInfo v201 = new VersionInfo();
        v201.setVersion("v2.0.1");
        v201.setDate("2025-01-10");
        List<String> features201 = new ArrayList<>();
        features201.add("首次发布Material Design风格的全新版本");
        features201.add("添加了多种订阅源支持");
        features201.add("新增了收藏功能");
        features201.add("全新的用户界面设计");
        v201.setFeatures(features201);
        versionInfoList.add(v201);

        return versionInfoList;
    }

    /**
     * 版本信息类
     */
    public static class VersionInfo {
        private String version;
        private String date;
        private List<String> features;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public List<String> getFeatures() {
            return features;
        }

        public void setFeatures(List<String> features) {
            this.features = features;
        }
    }

    /**
     * 版本历史适配器
     */
    private static class VersionHistoryAdapter extends BaseQuickAdapter<VersionInfo, BaseViewHolder> {
        public VersionHistoryAdapter() {
            super(R.layout.item_version_history);
        }

        @Override
        protected void convert(BaseViewHolder helper, VersionInfo item) {
            helper.setText(R.id.tv_version, item.getVersion() + " (" + item.getDate() + ")");

            // 设置特性列表
            TextView tvFeatures = helper.getView(R.id.tv_features);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < item.getFeatures().size(); i++) {
                sb.append("• ").append(item.getFeatures().get(i));
                if (i < item.getFeatures().size() - 1) {
                    sb.append("\n");
                }
            }
            tvFeatures.setText(sb.toString());
        }
    }
}
