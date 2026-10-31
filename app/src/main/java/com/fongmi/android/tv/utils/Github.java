package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.UpdatePackage;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * 检查更新（国内可用性优先）。
 *
 * 版本探测双通道：
 *  1. 官方 API（10s 短超时，能拿到更新日志正文/文件大小/digest）
 *  2. 失败自动降级 jsDelivr 读仓库 build.gradle 的 versionName（国内稳、无限流），
 *     下载地址按 CI 命名规则约定式构造，不依赖 API 返回的 assets 字段。
 *
 * 下载链路按"更新源"设置生成候选列表：内置 gh-proxy 镜像优先、官方直连兜底。
 * 镜像站淘汰很快，MIRRORS 为实测可用列表，失效时改这里即可。
 */
public class Github {

    public static final String REPO = "Tosencen/XMBOX";

    /** 内置镜像前缀列表（形如 https://gh-proxy.com/ ，镜像站淘汰快，失效时改这里即可） */
    private static final String[] MIRRORS = {
            "https://gh-proxy.com/",
            "https://gh-proxy.org/",
            "https://gh.ddlc.top/",
    };

    public static final int SOURCE_AUTO = 0;
    public static final int SOURCE_OFFICIAL = 1;
    public static final int SOURCE_MIRROR_FIRST = 2; // 2..2+MIRRORS.length-1 对应内置镜像
    public static final int SOURCE_CUSTOM = 9;

    public static final int[] SOURCE_VALUES = buildSourceValues();

    private static final long TIMEOUT = 10_000;
    private static final String API_LATEST = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String[] CDN_VERSION_URLS = {
            "https://cdn.jsdelivr.net/gh/" + REPO + "@latest/app/build.gradle",
            "https://cdn.jsdelivr.net/gh/" + REPO + "@main/app/build.gradle",
    };
    private static final Pattern VERSION_NAME = Pattern.compile("versionName\\s+\"([0-9]+(?:\\.[0-9]+){1,3})\"");

    private static int[] buildSourceValues() {
        int[] values = new int[MIRRORS.length + 3];
        values[0] = SOURCE_AUTO;
        values[1] = SOURCE_OFFICIAL;
        for (int i = 0; i < MIRRORS.length; i++) values[i + 2] = SOURCE_MIRROR_FIRST + i;
        values[values.length - 1] = SOURCE_CUSTOM;
        return values;
    }

    /** 设置页选项名（与 SOURCE_VALUES 一一对应） */
    public static String[] sourceNames() {
        String[] names = new String[SOURCE_VALUES.length];
        for (int i = 0; i < SOURCE_VALUES.length; i++) {
            int value = SOURCE_VALUES[i];
            if (value == SOURCE_AUTO) names[i] = ResUtil.getString(com.fongmi.android.tv.R.string.update_source_auto);
            else if (value == SOURCE_OFFICIAL) names[i] = ResUtil.getString(com.fongmi.android.tv.R.string.update_source_official);
            else if (value == SOURCE_CUSTOM) names[i] = ResUtil.getString(com.fongmi.android.tv.R.string.update_source_custom);
            else names[i] = MIRRORS[value - SOURCE_MIRROR_FIRST];
        }
        return names;
    }

    /** 当前更新源的显示文本（设置页用） */
    public static String sourceSummary() {
        int source = Setting.getUpdateSource();
        if (source == SOURCE_AUTO) return ResUtil.getString(com.fongmi.android.tv.R.string.update_source_auto);
        if (source == SOURCE_OFFICIAL) return ResUtil.getString(com.fongmi.android.tv.R.string.update_source_official);
        if (source == SOURCE_CUSTOM) return Setting.getUpdateMirror().isEmpty() ? ResUtil.getString(com.fongmi.android.tv.R.string.update_source_custom) : Setting.getUpdateMirror();
        if (source >= SOURCE_MIRROR_FIRST && source < SOURCE_MIRROR_FIRST + MIRRORS.length) return MIRRORS[source - SOURCE_MIRROR_FIRST];
        return ResUtil.getString(com.fongmi.android.tv.R.string.update_source_auto);
    }

    /** 资产命名与 CI 一致：{version}-{mode}-{abi}.apk */
    public static String assetName(String version) {
        return String.format(Locale.ROOT, "%s-%s-%s.apk", version, BuildConfig.FLAVOR_mode, BuildConfig.FLAVOR_abi);
    }

    /** 约定式下载地址（jsDelivr 通道使用） */
    public static String downloadUrl(String version) {
        return String.format(Locale.ROOT, "https://github.com/%s/releases/download/v%s/%s", REPO, version, assetName(version));
    }

    /** 按"更新源"设置生成候选下载链：镜像优先、官方兜底 */
    public static List<String> candidates(String url) {
        List<String> list = new ArrayList<>();
        int source = Setting.getUpdateSource();
        if (source == SOURCE_OFFICIAL) {
            list.add(url);
        } else if (source == SOURCE_CUSTOM) {
            String prefix = normalize(Setting.getUpdateMirror());
            if (!prefix.isEmpty()) list.add(prefix + url);
            list.add(url);
        } else if (source >= SOURCE_MIRROR_FIRST && source < SOURCE_MIRROR_FIRST + MIRRORS.length) {
            list.add(MIRRORS[source - SOURCE_MIRROR_FIRST] + url);
            list.add(url);
        } else {
            for (String mirror : MIRRORS) list.add(mirror + url);
            list.add(url);
        }
        return list;
    }

    private static String normalize(String prefix) {
        if (prefix == null) return "";
        String value = prefix.trim();
        if (value.isEmpty() || !value.startsWith("http")) return "";
        return value.endsWith("/") ? value : value + "/";
    }

    /**
     * 检查更新。
     *
     * @return 有新版时返回 UpdatePackage；确认已是最新返回 null
     * @throws Exception 所有通道均失败（携带失败原因）
     */
    public static UpdatePackage check() throws Exception {
        Exception apiError = null;
        try {
            UpdatePackage pkg = checkApi();
            if (pkg != null) return pkg;
            return null; // API 正常响应且确认已是最新
        } catch (Exception e) {
            apiError = e;
        }
        try {
            UpdatePackage pkg = checkCdn();
            if (pkg != null) return pkg;
            return null; // CDN 通道确认已是最新
        } catch (Exception e) {
            throw new IOException("API: " + apiError.getMessage() + "，CDN: " + e.getMessage());
        }
    }

    private static UpdatePackage checkApi() throws Exception {
        String response = fetch(API_LATEST);
        if (response.isEmpty()) throw new IOException("网络连接异常");
        JSONObject release = new JSONObject(response);
        String tagName = release.optString("tag_name");
        if (tagName.isEmpty()) {
            String message = release.optString("message");
            throw new IOException(message.contains("rate limit") ? "API请求次数已达上限" : "未找到发布版本");
        }
        String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        if (!isNewer(version, BuildConfig.VERSION_NAME)) return null;
        String name = assetName(version);
        JSONArray assets = release.optJSONArray("assets");
        String url = null;
        long size = -1;
        String digest = "";
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null || !name.equals(asset.optString("name"))) continue;
                if (!"uploaded".equals(asset.optString("state"))) continue;
                url = asset.optString("browser_download_url");
                size = asset.optLong("size", -1);
                digest = asset.optString("digest", "");
                break;
            }
        }
        if (url == null || url.isEmpty()) throw new IOException("未找到匹配的安装包");
        UpdatePackage pkg = new UpdatePackage(version, candidates(url), size, digest);
        pkg.setNotes(getNotes(release.optString("body"), BuildConfig.FLAVOR_mode));
        return pkg;
    }

    private static UpdatePackage checkCdn() throws Exception {
        Exception last = null;
        for (String url : CDN_VERSION_URLS) {
            try {
                String text = fetch(url);
                Matcher matcher = VERSION_NAME.matcher(text);
                if (!matcher.find()) throw new IOException("版本信息解析失败");
                String version = matcher.group(1);
                if (!isNewer(version, BuildConfig.VERSION_NAME)) return null;
                return new UpdatePackage(version, candidates(downloadUrl(version)), -1, "");
            } catch (Exception e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("版本探测失败");
    }

    private static String fetch(String url) throws IOException {
        OkHttpClient client = OkHttp.client(TIMEOUT);
        try (Response res = OkHttp.newCall(client, url, url).execute()) {
            if (!res.isSuccessful()) throw new IOException("HTTP " + res.code());
            return res.body() != null ? res.body().string() : "";
        }
    }

    /** 严格 x.y.z 版本解析（2-4 段纯数字），非法返回 null */
    public static int[] parseVersion(String tag) {
        if (tag == null) return null;
        String clean = tag.startsWith("v") ? tag.substring(1) : tag;
        String[] parts = clean.split("\\.", -1);
        if (parts.length < 2 || parts.length > 4) return null;
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("[0-9]+")) return null;
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    public static boolean isNewer(String candidate, String current) {
        int[] c = parseVersion(candidate);
        int[] u = parseVersion(current);
        if (c == null || u == null) return false;
        int n = Math.max(c.length, u.length);
        for (int i = 0; i < n; i++) {
            int a = i < c.length ? c[i] : 0;
            int b = i < u.length ? u[i] : 0;
            if (a != b) return a > b;
        }
        return false;
    }

    /** 按 flavor 分节提取更新日志（release body 中 "# mobile"/"# tablet"/"# leanback" 段落）；无分节则返回全文 */
    public static String getNotes(String body, String flavor) {
        if (body == null || body.trim().isEmpty()) return null;
        String heading = "# " + flavor;
        StringBuilder notes = new StringBuilder();
        boolean hasSections = false;
        boolean selected = false;
        for (String line : body.split("\r?\n")) {
            String text = line.trim();
            if (text.equalsIgnoreCase("# mobile") || text.equalsIgnoreCase("# tablet") || text.equalsIgnoreCase("# leanback")) {
                hasSections = true;
                selected = text.equalsIgnoreCase(heading);
            } else if (text.startsWith("# ")) {
                selected = false;
            } else if (selected && !text.isEmpty()) {
                if (notes.length() > 0) notes.append('\n');
                if (text.startsWith("* ") || text.startsWith("- ")) text = "• " + text.substring(2);
                notes.append(text);
            }
        }
        return hasSections ? (notes.length() == 0 ? null : notes.toString()) : body.trim();
    }
}
