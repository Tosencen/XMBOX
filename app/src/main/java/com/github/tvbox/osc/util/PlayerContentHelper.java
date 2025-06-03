package com.github.tvbox.osc.util;

import android.text.TextUtils;
import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 播放内容获取助手
 * 借鉴TVBoxOS-Mobile的优秀实现，增强播放信息获取的稳定性
 */
public class PlayerContentHelper {
    private static final String TAG = "PlayerContentHelper";

    /**
     * 增强的播放内容获取方法
     * @param sourceBean 数据源
     * @param playFlag 播放标识
     * @param url 播放URL
     * @param vipFlags VIP解析标识列表
     * @return 播放内容JSON字符串
     */
    public static String getPlayerContentWithRetry(SourceBean sourceBean, String playFlag, String url, List<String> vipFlags) {
        if (sourceBean == null || TextUtils.isEmpty(url)) {
            return null;
        }

        // 第一次尝试
        String result = getPlayerContentInternal(sourceBean, playFlag, url, vipFlags);
        if (!TextUtils.isEmpty(result)) {
            return result;
        }

        // 重试机制 - 使用异步方式避免阻塞线程
        LOG.w(TAG, "首次获取播放内容失败，开始重试");
        try {
            // 使用ThreadPoolManager的延迟执行，避免直接Thread.sleep
            final String[] retryResult = new String[1];
            final Object lock = new Object();

            ThreadPoolManager.executeIODelayed(() -> {
                synchronized (lock) {
                    retryResult[0] = getPlayerContentInternal(sourceBean, playFlag, url, vipFlags);
                    lock.notify();
                }
            }, 500);

            // 等待重试结果，最多等待2秒
            synchronized (lock) {
                lock.wait(2000);
            }

            if (!TextUtils.isEmpty(retryResult[0])) {
                LOG.i(TAG, "重试获取播放内容成功");
                return retryResult[0];
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.e(TAG, "重试机制异常: " + e.getMessage());
        }

        LOG.e(TAG, "获取播放内容失败，所有重试均失败");
        return null;
    }

    /**
     * 内部播放内容获取方法
     */
    private static String getPlayerContentInternal(SourceBean sourceBean, String playFlag, String url, List<String> vipFlags) {
        try {
            Spider spider = ApiConfig.get().getCSPWithRetry(sourceBean);
            if (spider == null) {
                LOG.e(TAG, "无法获取Spider实例");
                return null;
            }

            // 设置超时时间
            long startTime = System.currentTimeMillis();
            String json = spider.playerContent(playFlag, url, vipFlags);
            long endTime = System.currentTimeMillis();

            LOG.i(TAG, "获取播放内容耗时: " + (endTime - startTime) + "ms");

            if (TextUtils.isEmpty(json)) {
                LOG.w(TAG, "Spider返回空内容");
                return null;
            }

            // 验证返回的JSON格式
            if (isValidPlayerContent(json)) {
                return json;
            } else {
                LOG.w(TAG, "返回的播放内容格式无效");
                return null;
            }

        } catch (Exception e) {
            LOG.e(TAG, "获取播放内容异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 验证播放内容JSON是否有效
     */
    private static boolean isValidPlayerContent(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            // 检查必要字段
            return jsonObject.has("url") || jsonObject.has("parse");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 增强的播放URL处理
     * 借鉴TVBoxOS-Mobile的URL处理逻辑
     */
    public static String processPlayUrl(String url, String sourceKey) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        // 处理代理URL
        url = ApiConfig.checkReplaceProxy(url);

        // 处理相对路径
        if (url.startsWith("./") || url.startsWith("../")) {
            SourceBean sourceBean = ApiConfig.get().getSource(sourceKey);
            if (sourceBean != null && !TextUtils.isEmpty(sourceBean.getApi())) {
                String baseUrl = getBaseUrl(sourceBean.getApi());
                if (!TextUtils.isEmpty(baseUrl)) {
                    if (url.startsWith("./")) {
                        url = baseUrl + url.substring(2);
                    } else if (url.startsWith("../")) {
                        // 处理上级目录
                        String parentUrl = getParentUrl(baseUrl);
                        url = parentUrl + url.substring(3);
                    }
                }
            }
        }

        return url;
    }

    /**
     * 获取基础URL
     */
    private static String getBaseUrl(String fullUrl) {
        try {
            if (fullUrl.contains("/")) {
                return fullUrl.substring(0, fullUrl.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            LOG.e(TAG, "获取基础URL失败: " + e.getMessage());
        }
        return fullUrl;
    }

    /**
     * 获取父级URL
     */
    private static String getParentUrl(String baseUrl) {
        try {
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (baseUrl.contains("/")) {
                return baseUrl.substring(0, baseUrl.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            LOG.e(TAG, "获取父级URL失败: " + e.getMessage());
        }
        return baseUrl;
    }

    /**
     * 检查播放URL是否为视频格式
     * 借鉴TVBoxOS-Mobile的视频格式检测
     */
    public static boolean isVideoFormat(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        // 使用DefaultConfig的检测方法
        return DefaultConfig.isVideoFormat(url);
    }

    /**
     * 获取播放内容的超时时间（毫秒）
     */
    public static long getPlayerContentTimeout() {
        return TimeUnit.SECONDS.toMillis(10); // 10秒超时
    }

    /**
     * 清理播放内容中的无效字符
     */
    public static String cleanPlayerContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return content;
        }

        try {
            // 移除可能的BOM字符
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }

            // 移除前后空白字符
            content = content.trim();

            return content;
        } catch (Exception e) {
            LOG.e(TAG, "清理播放内容失败: " + e.getMessage());
            return content;
        }
    }
}
