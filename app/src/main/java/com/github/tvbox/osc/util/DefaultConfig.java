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
        if (!TextUtils.isEmpty(urlOri) && urlOri.startsWith("proxy://")) {
            return urlOri.replace("proxy://", ControlManager.get().getAddress(true) + "proxy?");
        } else {
            // 处理图片URL的特殊情况
            return processImageUrl(urlOri);
        }
    }

    /**
     * 处理图片URL的特殊情况
     * @param url 原始图片URL
     * @return 处理后的图片URL
     */
    public static String processImageUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        try {
            // 去除URL前后的空白字符
            url = url.trim();

            // 处理中文域名的图片URL
            if (url.contains("饭太硬")) {
                android.util.Log.d("DefaultConfig", "检测到饭太硬域名图片URL: " + url);
                // 使用UrlUtil处理中文域名
                url = UrlUtil.convertToPunycode(url);
                android.util.Log.d("DefaultConfig", "转换后的图片URL: " + url);
            }

            // 处理相对路径的图片URL
            if (url.startsWith("//")) {
                url = "https:" + url;
                android.util.Log.d("DefaultConfig", "补充协议后的图片URL: " + url);
            } else if (url.startsWith("/") && !url.startsWith("//")) {
                // 相对路径，需要根据当前数据源的域名补充
                // 这里可以根据需要添加更多逻辑
                android.util.Log.d("DefaultConfig", "检测到相对路径图片URL: " + url);
            }

            // 检查URL是否有效
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                android.util.Log.w("DefaultConfig", "无效的图片URL协议: " + url);
                return url; // 返回原始URL，让Glide处理
            }

            return url;
        } catch (Exception e) {
            android.util.Log.e("DefaultConfig", "处理图片URL时发生错误: " + e.getMessage());
            return url; // 发生错误时返回原始URL
        }
    }
}