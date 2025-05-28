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
        ExoMediaSourceHelper.getInstance(App.getInstance()).setOkClient(builder.build());
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
    private static OkHttpClient defaultClient = null;
    private static OkHttpClient noRedirectClient = null;

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
    public static void cleanup() {
        try {
            // 清理OkGo实例
            if (defaultClient != null) {
                // 关闭连接池
                defaultClient.connectionPool().evictAll();
                defaultClient.dispatcher().executorService().shutdown();
            }
            if (noRedirectClient != null) {
                noRedirectClient.connectionPool().evictAll();
                noRedirectClient.dispatcher().executorService().shutdown();
            }

            // 清空静态引用
            defaultClient = null;
            noRedirectClient = null;
            dnsOverHttps = null;

            // 清理DNS列表
            if (dnsHttpsList != null) {
                dnsHttpsList.clear();
            }

            LOG.i("OkGoHelper", "清理完成");
        } catch (Exception e) {
            LOG.e("OkGoHelper", "清理时发生错误: " + e.getMessage());
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
