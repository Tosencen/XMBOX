package com.github.tvbox.osc.network

import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.OkHttpSafetyUtil
import com.github.tvbox.osc.util.urlhttp.BrotliInterceptor
import com.google.gson.GsonBuilder
import com.orhanobut.hawk.Hawk
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端
 * 负责创建和管理Retrofit实例
 */
object RetrofitClient {
    private const val DEFAULT_TIMEOUT = 10000L // 默认超时时间，与OkGo保持一致
    
    // 共享OkHttpClient实例
    private val okHttpClient by lazy { createOkHttpClient() }
    
    // 创建基本的Retrofit实例
    private val retrofit by lazy { createRetrofit() }
    
    /**
     * 创建API服务接口
     */
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
    
    /**
     * 创建OkHttpClient
     * 尽可能复用OkGoHelper中的配置，确保行为一致性
     */
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        
        // 复用OkGoHelper中的连接规范
        builder.connectionSpecs(OkGoHelper.getConnectionSpec())
        
        // 添加Brotli压缩支持
        builder.addInterceptor(BrotliInterceptor())
        
        // 启用连接失败重试
        builder.retryOnConnectionFailure(true)
        
        // 允许重定向
        builder.followRedirects(true)
        builder.followSslRedirects(true)
        
        // 设置超时
        builder.connectTimeout(DEFAULT_TIMEOUT / 2, TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
        
        // 设置SSL
        try {
            OkGoHelper.setOkHttpSsl(builder)
        } catch (th: Throwable) {
            LOG.e("RetrofitClient", "SSL设置失败: ${th.message}")
        }
        
        // 使用安全工具类设置DNS
        OkHttpSafetyUtil.ensureSafeDns(builder, OkGoHelper.dnsOverHttps)
        
        // 增加并发连接数
        val dispatcher = Dispatcher()
        dispatcher.maxRequestsPerHost = 10
        dispatcher.maxRequests = 64
        builder.dispatcher(dispatcher)
        
        // 启用连接池
        builder.connectionPool(okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES))
        
        // 设置缓存
        val cacheDir = File(App.getInstance().cacheDir, "retrofit-cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        builder.cache(Cache(cacheDir, 50 * 1024 * 1024)) // 50MB缓存
        
        return builder.build()
    }
    
    /**
     * 创建Retrofit实例
     */
    private fun createRetrofit(): Retrofit {
        // 创建Gson实例，配置与OkGo使用的Gson保持一致
        val gson = GsonBuilder()
            .setLenient() // 宽松解析
            .create()
        
        return Retrofit.Builder()
            .baseUrl("https://placeholder-base-url.com/") // 实际URL会在请求时通过@Url参数提供
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
