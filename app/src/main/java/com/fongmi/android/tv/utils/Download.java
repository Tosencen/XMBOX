package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.App;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Logger;
import com.github.catvod.utils.Path;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 通用文件下载。
 *
 * - 支持多候选 URL：当前候选失败/无进度超时后自动切换下一个
 * - 支持 Range 断点续传与暂停/继续
 * - 连接 10s / 读 30s 短超时 + 30s 无数据看门狗，快速失败不卡死
 * - APK 下载完成后校验 ZIP 文件头
 *
 * 旧接口（create(url, file[, fallbackUrl][, callback]) + start()）保持兼容：
 * 无回调时同步执行，有回调时进共享线程池；更新服务请用 create(List,...) + run()。
 */
public class Download {

    public static final int STALL_TIMEOUT = 30_000;

    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT = 30_000;
    private static final int MAX_ATTEMPT = 2;
    private static final OkHttpClient CLIENT = OkHttp.client().newBuilder()
            .connectTimeout(CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(READ_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build();

    private final List<String> urls;
    private final File file;
    private final Callback callback;
    private volatile boolean cancelled;
    private volatile boolean paused;
    private volatile Call call;

    public static Download create(String url, File file) {
        return create(url, file, null);
    }

    public static Download create(String url, File file, Callback callback) {
        return create(url, file, null, callback);
    }

    public static Download create(String url, File file, String fallbackUrl, Callback callback) {
        List<String> urls = new ArrayList<>();
        urls.add(url);
        if (fallbackUrl != null && !fallbackUrl.isEmpty() && !fallbackUrl.equals(url)) urls.add(fallbackUrl);
        return new Download(urls, file, callback);
    }

    public static Download create(List<String> urls, File file, Callback callback) {
        return new Download(urls, file, callback);
    }

    public Download(List<String> urls, File file, Callback callback) {
        this.urls = urls == null ? Collections.emptyList() : urls;
        this.file = file;
        this.callback = callback;
    }

    /** 旧入口：无回调同步执行，有回调进共享线程池 */
    public void start() {
        if (urls.isEmpty() || urls.get(0) == null || urls.get(0).isEmpty()) {
            if (callback != null) App.post(() -> callback.error("下载URL为空"));
            return;
        }
        if (urls.get(0).startsWith("file")) return;
        if (file == null) {
            if (callback != null) App.post(() -> callback.error("保存文件路径为空"));
            return;
        }
        if (callback == null) {
            run();
        } else {
            App.execute(this::run);
        }
    }

    /** 在调用方线程上执行下载（更新服务用专用线程调用） */
    public void run() {
        if (cancelled || file == null) return;
        Logger.d("Download: run urls=" + urls + " file=" + file + " resume=" + file.length());
        paused = false;
        String lastError = null;
        for (String url : urls) {
            if (cancelled || paused || url == null || url.isEmpty() || url.startsWith("file")) continue;
            for (int attempt = 1; attempt <= MAX_ATTEMPT; attempt++) {
                if (cancelled || paused) return;
                try {
                    if (download(url)) return;
                } catch (Exception e) {
                    if (cancelled || paused) return;
                    lastError = e.getMessage();
                    Logger.w("Download: 失败 (" + url + ", " + attempt + "/" + MAX_ATTEMPT + "): " + lastError);
                    try {
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        if (cancelled || paused) return;
        if (callback != null) {
            String msg = lastError != null ? lastError : "下载失败";
            App.post(() -> callback.error(msg));
        }
    }

    /** @return true 表示下载结束（成功或已暂停），false 不应出现 */
    private boolean download(String url) throws Exception {
        long offset = file.length();
        Request.Builder builder = new Request.Builder().url(url).tag(url);
        if (offset > 0) builder.header("Range", "bytes=" + offset + "-");
        Call newCall = CLIENT.newCall(builder.build());
        this.call = newCall;
        try (Response res = newCall.execute()) {
            if (!res.isSuccessful() && res.code() != 206) throw new Exception("HTTP " + res.code());
            boolean append = res.code() == 206 && offset > 0;
            long total;
            if (append) {
                total = parseContentRangeTotal(res.header("Content-Range"));
                if (total < 0) {
                    long part = contentLength(res);
                    total = part > 0 ? offset + part : -1;
                }
            } else {
                offset = 0;
                total = contentLength(res);
            }
            if (res.body() == null) throw new Exception("响应体为空");
            Path.create(file);
            write(res.body().byteStream(), append ? offset : 0, total);
            return true;
        } finally {
            this.call = null;
        }
    }

    private void write(InputStream is, long start, long total) throws Exception {
        long done = start;
        int percent = start > 0 && total > 0 ? (int) Math.min(done * 100 / total, 100) : -1;
        long lastActive = System.currentTimeMillis();
        long windowStart = done;
        long windowTime = lastActive;
        long lastReport = 0;
        long speed = 0;
        try (BufferedInputStream input = new BufferedInputStream(is); RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            if (start > 0) raf.seek(start);
            else raf.setLength(0);
            byte[] buffer = new byte[8192];
            int read;
            while (!cancelled && !paused && (read = input.read(buffer)) != -1) {
                raf.write(buffer, 0, read);
                done += read;
                long now = System.currentTimeMillis();
                if (now - lastActive > STALL_TIMEOUT) throw new Exception("下载超时（长时间无数据）");
                lastActive = now;
                if (now - windowTime >= 1000) {
                    speed = (done - windowStart) * 1000 / (now - windowTime);
                    windowStart = done;
                    windowTime = now;
                }
                int p = total > 0 ? (int) Math.min(done * 100 / total, 100) : -1;
                if (p != percent || now - lastReport >= 1000) {
                    percent = p;
                    lastReport = now;
                    notifyProgress(p, speed, eta(total, done, speed));
                }
            }
            if (cancelled) throw new Exception("下载已取消");
            if (paused) {
                if (callback != null) App.post(() -> callback.paused());
                return;
            }
            if (total > 0 && done != total) throw new Exception("下载不完整（" + done + "/" + total + "）");
            if (callback != null && total <= 0) App.post(() -> callback.progress(100));
        }
        if (file.getName().toLowerCase().endsWith(".apk") && !verifyApkHeader()) throw new Exception("下载的文件已损坏");
        if (callback != null) App.post(() -> callback.success(file));
    }

    private void notifyProgress(int percent, long speed, long eta) {
        Callback cb = callback;
        if (cb == null || cancelled || paused) return;
        App.post(() -> cb.progress(percent, speed, eta));
    }

    private static long eta(long total, long done, long speed) {
        if (total <= 0 || speed <= 0 || done >= total) return -1;
        return (total - done) / speed;
    }

    private static long contentLength(Response res) {
        String value = res.header("Content-Length");
        if (value == null || value.isEmpty()) return -1;
        try {
            long length = Long.parseLong(value);
            return length > 0 ? length : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 解析 Content-Range: bytes start-end/total 中的 total，失败返回 -1 */
    private static long parseContentRangeTotal(String contentRange) {
        if (contentRange == null) return -1;
        int slash = contentRange.lastIndexOf('/');
        if (slash < 0 || slash == contentRange.length() - 1) return -1;
        try {
            long total = Long.parseLong(contentRange.substring(slash + 1).trim());
            return total > 0 ? total : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean verifyApkHeader() {
        try {
            if (file == null || !file.isFile() || file.length() < 4) return false;
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] header = new byte[4];
                if (fis.read(header) < 4) return false;
                return header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /** 暂停：保留已下载部分，可通过 run() 续传 */
    public void pause() {
        paused = true;
        Call active = call;
        if (active != null) active.cancel();
    }

    /** 取消：中断并删除已下载文件 */
    public void cancel() {
        cancelled = true;
        Call active = call;
        if (active != null) active.cancel();
        for (String url : urls) if (url != null && url.startsWith("http")) OkHttp.cancel(url);
        Path.clear(file);
    }

    public interface Callback {

        void progress(int progress);

        void error(String msg);

        void success(File file);

        /** 带速度/剩余时间的进度（秒，-1 未知） */
        default void progress(int progress, long speed, long eta) {
            progress(progress);
        }

        /** 已暂停（保留断点） */
        default void paused() {
        }
    }
}
