package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.server.ControlManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class DefaultConfig {

    public static List<MovieSort.SortData> adjustSort(String sourceKey, List<MovieSort.SortData> list, boolean withMy) {
        List<MovieSort.SortData> data = new ArrayList<>();
        if (sourceKey != null) {
            SourceBean sb = ApiConfig.get().getSource(sourceKey);
            ArrayList<String> categories = sb.getCategories();
            if (!categories.isEmpty()) {
                for (String cate : categories) {
                    for (MovieSort.SortData sortData : list) {
                        if (sortData.name.equals(cate)) {
                            if (sortData.filters == null)
                                sortData.filters = new ArrayList<>();
                            data.add(sortData);
                        }
                    }
                }
            } else {
                for (MovieSort.SortData sortData : list) {
                    if (sortData.filters == null)
                        sortData.filters = new ArrayList<>();
                    data.add(sortData);
                }
            }
        }
        if (withMy)
            data.add(0, new MovieSort.SortData("my0", "主页"));
        Collections.sort(data);
        return data;
    }

    public static int getAppVersionCode(Context mContext) {
        //包管理操作管理类
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String getAppVersionName(Context mContext) {
        //包管理操作管理类
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 后缀
     *
     * @param name
     * @return
     */
    public static String getFileSuffix(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        int endP = name.lastIndexOf(".");
        return endP > -1 ? name.substring(endP) : "";
    }

    /**
     * 获取文件的前缀
     *
     * @param fileName
     * @return
     */
    public static String getFilePrefixName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        int start = fileName.lastIndexOf(".");
        return start > -1 ? fileName.substring(0, start) : fileName;
    }

    private static final Pattern snifferMatch = Pattern.compile(
            "http((?!http).){12,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a)\\?.*|" +
            "http((?!http).){12,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a)|" +
            "http((?!http).)*?video/tos*|" +
            "http((?!http).){20,}?/m3u8\\?pt=m3u8.*|" +
            "http((?!http).)*?default\\.ixigua\\.com/.*|" +
            "http((?!http).)*?dycdn-tos\\.pstatp[^\\?]*|" +
            "http.*?/player/m3u8play\\.php\\?url=.*|" +
            "http.*?/player/.*?[pP]lay\\.php\\?url=.*|" +
            "http.*?/playlist/m3u8/\\?vid=.*|" +
            "http.*?\\.php\\?type=m3u8&.*|" +
            "http.*?/download.aspx\\?.*|" +
            "http.*?/api/up_api.php\\?.*|" +
            "https.*?\\.66yk\\.cn.*|" +
            "http((?!http).)*?netease\\.com/file/.*"
    );
    public static boolean isVideoFormat(String url) {
        Uri uri = Uri.parse(url);
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        if (snifferMatch.matcher(url).find()) return true;
        return false;
    }


    public static String safeJsonString(JsonObject obj, String key, String defaultVal) {
        try {
            if (obj.has(key))
                return obj.getAsJsonPrimitive(key).getAsString().trim();
            else
                return defaultVal;
        } catch (Throwable th) {
        }
        return defaultVal;
    }

    public static int safeJsonInt(JsonObject obj, String key, int defaultVal) {
        try {
            if (obj.has(key))
                return obj.getAsJsonPrimitive(key).getAsInt();
            else
                return defaultVal;
        } catch (Throwable th) {
        }
        return defaultVal;
    }

    public static ArrayList<String> safeJsonStringList(JsonObject obj, String key) {
        ArrayList<String> result = new ArrayList<>();
        try {
            if (obj.has(key)) {
                if (obj.get(key).isJsonObject()) {
                    result.add(obj.get(key).getAsString());
                } else {
                    for (JsonElement opt : obj.getAsJsonArray(key)) {
                        result.add(opt.getAsString());
                    }
                }
            }
        } catch (Throwable th) {
        }
        return result;
    }

    public static String checkReplaceProxy(String urlOri) {
        // 添加调试日志
        android.util.Log.d("DefaultConfig", "checkReplaceProxy输入: " + urlOri);

        if (TextUtils.isEmpty(urlOri)) {
            return urlOri;
        }

        // 参考TVBoxOS-Mobile的实现：对于非proxy://协议的URL，直接返回
        if (!urlOri.startsWith("proxy://")) {
            android.util.Log.d("DefaultConfig", "非proxy协议，直接返回: " + urlOri);
            return urlOri;
        }

        // 处理proxy://协议
        try {
            String serverAddress = ControlManager.get().getAddress(true);
            android.util.Log.d("DefaultConfig", "服务器地址: " + serverAddress);

            // 检查服务器地址是否有效
            if (serverAddress.contains("0.0.0.0")) {
                android.util.Log.w("DefaultConfig", "服务器地址无效，跳过proxy处理: " + serverAddress);
                // 如果服务器地址无效，尝试直接使用原始URL（去掉proxy://前缀）
                String directUrl = urlOri.substring(8); // 去掉"proxy://"
                android.util.Log.d("DefaultConfig", "使用直接URL: " + directUrl);
                return directUrl;
            }

            String result = urlOri.replace("proxy://", serverAddress + "proxy?");
            android.util.Log.d("DefaultConfig", "proxy://处理结果: " + result);
            return result;
        } catch (Exception e) {
            android.util.Log.e("DefaultConfig", "处理proxy://时发生错误: " + e.getMessage());
            // 发生错误时，尝试直接使用原始URL（去掉proxy://前缀）
            if (urlOri.startsWith("proxy://")) {
                String directUrl = urlOri.substring(8);
                android.util.Log.d("DefaultConfig", "错误fallback，使用直接URL: " + directUrl);
                return directUrl;
            }
            return urlOri;
        }
    }

    /**
     * 简化的图片URL处理，参考TVBoxOS-Mobile实现
     * @param url 原始图片URL
     * @return 处理后的图片URL
     */
    public static String processImageUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        android.util.Log.d("DefaultConfig", "processImageUrl输入: " + url);

        try {
            // 处理特殊的图片URL格式，如: "url@Referer=xxx@User-Agent=xxx"
            if (url.contains("@Referer=") || url.contains("@User-Agent=")) {
                // 提取@符号前的实际图片URL
                int atIndex = url.indexOf("@");
                if (atIndex > 0) {
                    String cleanUrl = url.substring(0, atIndex);
                    android.util.Log.d("DefaultConfig", "清理后的图片URL: " + cleanUrl);
                    url = cleanUrl;
                }
            }

            // 处理相对协议的URL
            if (url.startsWith("//")) {
                url = "https:" + url;
            }

            // 处理中文域名（仅在必要时）
            if (url.contains("饭太硬")) {
                url = UrlUtil.convertToPunycode(url);
            }

            android.util.Log.d("DefaultConfig", "processImageUrl输出: " + url);
            return url;
        } catch (Exception e) {
            android.util.Log.e("DefaultConfig", "处理图片URL时发生错误: " + e.getMessage());
            return url; // 发生错误时返回原始URL
        }
    }
}