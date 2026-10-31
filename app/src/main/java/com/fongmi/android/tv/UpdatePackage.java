package com.fongmi.android.tv;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import com.github.catvod.utils.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 一次可用的更新（版本 + 候选下载链 + 元信息）。
 *
 * 下载完成后必须通过 verifyApk() 签名校验（比对已装应用签名），
 * 防止镜像/链路被篡改的安装包。
 */
public class UpdatePackage {

    private final String version;
    private final List<String> urls;
    private final long size;
    private final String digest;
    private String notes;

    public UpdatePackage(String version, List<String> urls, long size, String digest) {
        this.version = version;
        this.urls = urls == null ? new ArrayList<>() : urls;
        this.size = size;
        this.digest = digest == null ? "" : digest;
    }

    public String version() {
        return version;
    }

    public List<String> urls() {
        return urls;
    }

    /** 首选下载地址（官方直链） */
    public String url() {
        return urls.isEmpty() ? "" : urls.get(0);
    }

    /** 预期文件大小，未知返回 -1 */
    public long size() {
        return size;
    }

    /** GitHub 资产摘要（sha256:...），无则为空 */
    public String digest() {
        return digest;
    }

    public String notes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /** 版本专用缓存文件，便于同版本复用 */
    public File file() {
        return Path.cache("XMBOX-update-" + version + ".apk");
    }

    /**
     * 校验缓存/下载的 APK 签名是否与已装应用一致。
     * 无法解析或不一致返回 false。
     */
    public boolean verifyApk() {
        try {
            File file = file();
            if (!file.isFile() || file.length() == 0) return false;
            PackageManager pm = App.get().getPackageManager();
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
            PackageInfo archive = pm.getPackageArchiveInfo(file.getAbsolutePath(), flags);
            if (archive == null) return false;
            PackageInfo installed = pm.getPackageInfo(App.get().getPackageName(), flags);
            String[] a = signatures(archive);
            String[] b = signatures(installed);
            if (a == null || b == null || a.length != b.length) return false;
            Set<String> expected = new HashSet<>(Arrays.asList(b));
            for (String signature : a) if (!expected.contains(signature)) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String[] signatures(PackageInfo info) {
        List<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null && info.signingInfo.getApkContentsSigners() != null) {
            for (Signature signature : info.signingInfo.getApkContentsSigners()) list.add(signature.toCharsString());
        } else if (info.signatures != null) {
            for (Signature signature : info.signatures) list.add(signature.toCharsString());
        }
        return list.isEmpty() ? null : list.toArray(new String[0]);
    }
}
