package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
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
