package com.github.tvbox.osc.util;

import static okhttp3.ConnectionSpec.CLEARTEXT;
import static okhttp3.ConnectionSpec.COMPATIBLE_TLS;
import static okhttp3.ConnectionSpec.MODERN_TLS;
import static okhttp3.ConnectionSpec.RESTRICTED_TLS;

import okhttp3.Dispatcher;

import com.github.catvod.net.SSLCompat;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.urlhttp.BrotliInterceptor;
import com.github.tvbox.osc.util.LOG;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.Cache;
import okhttp3.ConnectionSpec;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;
// import okhttp3.internal.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// import okhttp3.internal.Version;
import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper;
import com.github.tvbox.osc.util.OkHttpSafetyUtil;

public class OkGoHelper {
    public static final long DEFAULT_MILLISECONDS = 10000;      //默认的超时时间

    static void initExoOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkExoPlayer");

        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }

        // 设置连接规范
        builder.connectionSpecs(getConnectionSpec());

        // 添加Brotli压缩支持
        builder.addInterceptor(new BrotliInterceptor());

        // 启用连接失败重试
        builder.retryOnConnectionFailure(true);

        // 允许重定向
        builder.followRedirects(true);
        builder.followSslRedirects(true);

        // 优化超时设置，视频播放需要更长的超时时间
        builder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
               .writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
               .connectTimeout(DEFAULT_MILLISECONDS / 2, TimeUnit.MILLISECONDS);

        // 设置SSL
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // 使用安全工具类设置DNS，确保不会传入null值
        // 在Android 15及以上版本，OkHttp不再接受null作为DNS参数
        OkHttpSafetyUtil.ensureSafeDns(builder, dnsOverHttps);

        // 增加并发连接数
        Dispatcher dispatcher = new Dispatcher(ThreadPoolManager.getIOThreadPool());
        dispatcher.setMaxRequestsPerHost(8);
        dispatcher.setMaxRequests(32);
        builder.dispatcher(dispatcher);

        // 启用连接池
        builder.connectionPool(new okhttp3.ConnectionPool(16, 5, TimeUnit.MINUTES));

        // 设置缓存
        File cacheDir = new File(App.getInstance().getCacheDir(), "exoplayer-http-cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        builder.cache(new Cache(cacheDir, 50 * 1024 * 1024)); // 50MB缓存

        // 设置客户端
        OkHttpClient exoClient = builder.build();
        ExoMediaSourceHelper.getInstance(App.getInstance()).setOkClient(exoClient);

        // 保存ExoPlayer客户端引用以便清理
        exoPlayerClient = exoClient;
    }

    public static DnsOverHttps dnsOverHttps = null;

    public static ArrayList<String> dnsHttpsList = new ArrayList<>();

    public static List<ConnectionSpec> getConnectionSpec() {
        List<ConnectionSpec> specs = new ArrayList<>();
        specs.add(RESTRICTED_TLS);
        specs.add(MODERN_TLS);
        specs.add(COMPATIBLE_TLS);
        specs.add(CLEARTEXT);
        return Collections.unmodifiableList(specs);
    }

    public static String getDohUrl(int type) {
        // 确保type在有效范围内
        int safeType = Math.min(Math.max(type, 0), 3);
        switch (safeType) {
            case 1: {
                return "https://dns.alidns.com/dns-query"; // 阿里DNS
            }
            case 2: {
                return "https://doh.pub/dns-query"; // 腾讯DNS
            }
            case 3: {
                return "https://doh.360.cn/dns-query"; // 360DNS
            }
        }
        return ""; // 关闭DNS
    }

    static void initDnsOverHttps() {
        // 初始化DNS列表
        dnsHttpsList.clear();
        dnsHttpsList.add("关闭");
        dnsHttpsList.add("阿里DNS");
        dnsHttpsList.add("腾讯DNS");
        dnsHttpsList.add("360DNS");

        // 创建DoH客户端
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // 配置日志
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("DNS-Client");
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }

        // 添加拦截器
        builder.addInterceptor(loggingInterceptor);
        builder.addInterceptor(new BrotliInterceptor());

        // 设置SSL
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // 设置连接规范
        builder.connectionSpecs(getConnectionSpec());

        // 设置超时
        builder.connectTimeout(3, TimeUnit.SECONDS)
               .readTimeout(3, TimeUnit.SECONDS)
               .writeTimeout(3, TimeUnit.SECONDS);

        // 设置缓存
        File cacheDir = new File(App.getInstance().getCacheDir(), "dohcache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        builder.cache(new Cache(cacheDir, 10 * 1024 * 1024));

        // 构建DoH客户端
        OkHttpClient dohClient = builder.build();

        // 确保使用有效的DOH索引，默认使用阿里DNS
        int dohIndex = Hawk.get(HawkConfig.DOH_URL, 1); // 默认值为1（阿里DNS）
        if (dohIndex < 0 || dohIndex >= dnsHttpsList.size()) {
            dohIndex = 1; // 默认使用阿里DNS
            Hawk.put(HawkConfig.DOH_URL, 1);
        }

        // 配置DNS
        if (dohIndex == 0) {
            // 默认关闭DoH，使用系统DNS以提高速度
            dnsOverHttps = null;
            LOG.i("DNS", "使用系统DNS");
        } else {
            // 使用DoH
            String dohUrl = getDohUrl(dohIndex);
            if (!dohUrl.isEmpty()) {
                try {
                    dnsOverHttps = new DnsOverHttps.Builder()
                            .client(dohClient)
                            .url(HttpUrl.get(dohUrl))
                            .bootstrapDnsHosts(getBootstrapDnsHosts()) // 添加引导DNS服务器
                            .includeIPv6(false) // 禁用IPv6以提高速度
                            .build();
                    LOG.i("DNS", "使用DoH: " + dohUrl);
                } catch (Exception e) {
                    LOG.e("DNS", "DoH初始化失败: " + e.getMessage());
                    dnsOverHttps = null;
                }
            } else {
                dnsOverHttps = null;
                LOG.e("DNS", "无效的DoH URL");
            }
        }
    }

    /**
     * 获取引导DNS服务器IP地址
     * 用于在DoH初始化时解析DoH服务器域名
     */
    private static List<InetAddress> getBootstrapDnsHosts() {
        List<InetAddress> hosts = new ArrayList<>();
        try {
            // 添加一些公共DNS服务器IP
            hosts.add(InetAddress.getByName("223.5.5.5")); // 阿里DNS
            hosts.add(InetAddress.getByName("119.29.29.29")); // 腾讯DNS
            hosts.add(InetAddress.getByName("8.8.8.8")); // 谷歌DNS
        } catch (Exception e) {
            LOG.e("DNS", "获取引导DNS失败: " + e.getMessage());
        }
        return hosts;
    }
    private static volatile OkHttpClient defaultClient = null;
    private static volatile OkHttpClient noRedirectClient = null;
    private static volatile OkHttpClient exoPlayerClient = null;

    // 添加清理标志
    private static volatile boolean isCleaningUp = false;

    public static OkHttpClient getDefaultClient() {
        return defaultClient;
    }

    public static OkHttpClient getNoRedirectClient() {
        return noRedirectClient;
    }

    public static void init() {
        initDnsOverHttps();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");

        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }

        // 启用连接失败重试
        builder.retryOnConnectionFailure(true);

        // 设置连接规范
        builder.connectionSpecs(getConnectionSpec());

        // 添加Brotli压缩支持
        builder.addInterceptor(new BrotliInterceptor());

        // 优化超时设置，减少等待时间
        builder.readTimeout(DEFAULT_MILLISECONDS / 2, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_MILLISECONDS / 2, TimeUnit.MILLISECONDS)
                .connectTimeout(DEFAULT_MILLISECONDS / 2, TimeUnit.MILLISECONDS);

        // 使用安全工具类设置DNS，确保不会传入null值
        // 在Android 15及以上版本，OkHttp不再接受null作为DNS参数
        OkHttpSafetyUtil.ensureSafeDns(builder, dnsOverHttps);

        // 增加并发连接数
        Dispatcher dispatcher = new Dispatcher(ThreadPoolManager.getIOThreadPool());
        dispatcher.setMaxRequestsPerHost(10);
        dispatcher.setMaxRequests(64); // 增加最大并发请求数
        builder.dispatcher(dispatcher);

        // 启用连接池
        builder.connectionPool(new okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES));

        // 设置SSL
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // 设置User-Agent
        HttpHeaders.setUserAgent("okhttp/4.9.0");

        // 构建客户端
        OkHttpClient okHttpClient = builder.build();
        OkGo.getInstance().setOkHttpClient(okHttpClient);

        // 配置OkGo
        OkGo.getInstance()
            .setRetryCount(3) // 请求失败重试次数
            .setCacheMode(com.lzy.okgo.cache.CacheMode.NO_CACHE); // 默认不使用缓存

        // 保存默认客户端
        defaultClient = okHttpClient;

        // 创建不跟随重定向的客户端
        builder.followRedirects(false);
        builder.followSslRedirects(false);
        noRedirectClient = builder.build();

        // 初始化ExoPlayer的OkHttp客户端
        initExoOkHttpClient();
    }

    /**
     * 清理静态引用，防止内存泄漏
     * 建议在Application的onTerminate()中调用
     */
    public static synchronized void cleanup() {
        if (isCleaningUp) {
            LOG.w("OkGoHelper", "清理操作已在进行中，跳过重复清理");
            return;
        }

        isCleaningUp = true;

        try {
            LOG.i("OkGoHelper", "开始清理OkHttp客户端资源");

            // 清理默认客户端
            if (defaultClient != null) {
                try {
                    // 关闭连接池
                    defaultClient.connectionPool().evictAll();
                    // 关闭缓存
                    if (defaultClient.cache() != null) {
                        defaultClient.cache().close();
                    }
                    // 关闭调度器
                    defaultClient.dispatcher().executorService().shutdown();
                    defaultClient = null;
                    LOG.i("OkGoHelper", "默认客户端已清理");
                } catch (Exception e) {
                    LOG.w("OkGoHelper", "清理默认客户端失败: " + e.getMessage());
                }
            }

            // 清理无重定向客户端
            if (noRedirectClient != null) {
                try {
                    noRedirectClient.connectionPool().evictAll();
                    if (noRedirectClient.cache() != null) {
                        noRedirectClient.cache().close();
                    }
                    noRedirectClient.dispatcher().executorService().shutdown();
                    noRedirectClient = null;
                    LOG.i("OkGoHelper", "无重定向客户端已清理");
                } catch (Exception e) {
                    LOG.w("OkGoHelper", "清理无重定向客户端失败: " + e.getMessage());
                }
            }

            LOG.i("OkGoHelper", "OkHttp客户端资源清理完成");
        } catch (Exception e) {
            LOG.e("OkGoHelper", "清理OkHttp客户端资源失败: " + e.getMessage());
        }

        try {
            LOG.i("OkGoHelper", "开始清理OkGoHelper资源");

            // 取消所有OkGo请求
            try {
                if (OkGo.getInstance() != null) {
                    OkGo.getInstance().cancelAll();
                    LOG.i("OkGoHelper", "已取消所有OkGo请求");
                }
            } catch (Exception e) {
                LOG.w("OkGoHelper", "取消OkGo请求时发生错误: " + e.getMessage());
            }

            // 清理默认客户端
            if (defaultClient != null) {
                try {
                    // 关闭连接池
                    defaultClient.connectionPool().evictAll();

                    // 强制关闭调度器
                    if (defaultClient.dispatcher() != null && defaultClient.dispatcher().executorService() != null) {
                        defaultClient.dispatcher().executorService().shutdownNow();
                        try {
                            if (!defaultClient.dispatcher().executorService().awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                                LOG.w("OkGoHelper", "默认客户端调度器未能在1秒内关闭");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOG.w("OkGoHelper", "等待默认客户端调度器关闭时被中断");
                        }
                    }

                    // 关闭缓存
                    if (defaultClient.cache() != null) {
                        try {
                            defaultClient.cache().close();
                        } catch (Exception e) {
                            LOG.w("OkGoHelper", "关闭默认客户端缓存时发生错误: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOG.w("OkGoHelper", "清理默认客户端时发生错误: " + e.getMessage());
                }
                defaultClient = null;
                LOG.i("OkGoHelper", "默认客户端已清理");
            }

            // 清理无重定向客户端
            if (noRedirectClient != null) {
                try {
                    noRedirectClient.connectionPool().evictAll();

                    if (noRedirectClient.dispatcher() != null && noRedirectClient.dispatcher().executorService() != null) {
                        noRedirectClient.dispatcher().executorService().shutdownNow();
                        try {
                            if (!noRedirectClient.dispatcher().executorService().awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                                LOG.w("OkGoHelper", "无重定向客户端调度器未能在1秒内关闭");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOG.w("OkGoHelper", "等待无重定向客户端调度器关闭时被中断");
                        }
                    }

                    // 关闭缓存
                    if (noRedirectClient.cache() != null) {
                        try {
                            noRedirectClient.cache().close();
                        } catch (Exception e) {
                            LOG.w("OkGoHelper", "关闭无重定向客户端缓存时发生错误: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOG.w("OkGoHelper", "清理无重定向客户端时发生错误: " + e.getMessage());
                }
                noRedirectClient = null;
                LOG.i("OkGoHelper", "无重定向客户端已清理");
            }

            // 清理ExoPlayer客户端
            if (exoPlayerClient != null) {
                try {
                    exoPlayerClient.connectionPool().evictAll();

                    if (exoPlayerClient.dispatcher() != null && exoPlayerClient.dispatcher().executorService() != null) {
                        exoPlayerClient.dispatcher().executorService().shutdownNow();
                        try {
                            if (!exoPlayerClient.dispatcher().executorService().awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                                LOG.w("OkGoHelper", "ExoPlayer客户端调度器未能在1秒内关闭");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOG.w("OkGoHelper", "等待ExoPlayer客户端调度器关闭时被中断");
                        }
                    }

                    // 关闭缓存
                    if (exoPlayerClient.cache() != null) {
                        try {
                            exoPlayerClient.cache().close();
                        } catch (Exception e) {
                            LOG.w("OkGoHelper", "关闭ExoPlayer客户端缓存时发生错误: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    LOG.w("OkGoHelper", "清理ExoPlayer客户端时发生错误: " + e.getMessage());
                }
                exoPlayerClient = null;
                LOG.i("OkGoHelper", "ExoPlayer客户端已清理");
            }

            // 清理DNS相关资源
            if (dnsOverHttps != null) {
                dnsOverHttps = null;
                LOG.i("OkGoHelper", "DNS over HTTPS已清理");
            }

            // 清理DNS列表
            if (dnsHttpsList != null) {
                dnsHttpsList.clear();
                LOG.i("OkGoHelper", "DNS列表已清理");
            }

            LOG.i("OkGoHelper", "OkGoHelper资源清理完成");
        } catch (Exception e) {
            LOG.e("OkGoHelper", "清理OkGoHelper资源时发生错误: " + e.getMessage());
        } finally {
            isCleaningUp = false;
        }
    }

    /**
     * 彻底清理OkGo，仅在应用退出时调用
     * 警告：调用此方法后OkGo将无法继续使用
     */
    public static void completeCleanup() {
        try {
            LOG.i("OkGoHelper", "开始彻底清理OkGo资源");

            // 先执行常规清理
            cleanup();

            // 清理OkGo的内部引用
            try {
                java.lang.reflect.Field clientField = OkGo.class.getDeclaredField("okHttpClient");
                clientField.setAccessible(true);
                clientField.set(OkGo.getInstance(), null);
                LOG.i("OkGoHelper", "OkGo内部引用已清理");
            } catch (Exception e) {
                LOG.w("OkGoHelper", "清理OkGo内部引用时发生错误: " + e.getMessage());
            }

            LOG.i("OkGoHelper", "彻底清理完成");
        } catch (Exception e) {
            LOG.e("OkGoHelper", "彻底清理时发生错误: " + e.getMessage());
        }
    }

    // Picasso初始化已移除，改为使用GlideHelper

    public static synchronized void setOkHttpSsl(OkHttpClient.Builder builder) {
        try {

            final SSLSocketFactory sslSocketFactory = new SSLCompat();
            builder.sslSocketFactory(sslSocketFactory, SSLCompat.TM);
            builder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
