package com.github.tvbox.osc.network.repository

import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.network.RetrofitClient
import com.github.tvbox.osc.network.api.ConfigService
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.MD5
import com.github.tvbox.osc.util.UrlUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 配置仓库
 * 负责处理配置相关的网络请求和数据处理
 */
class ConfigRepository {
    private val service = RetrofitClient.createService(ConfigService::class.java)
    
    /**
     * 加载配置
     * 
     * @param apiUrl 配置地址
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun loadConfig(apiUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 处理URL
            val (configUrl, configKey) = processApiUrl(apiUrl)
            
            // 处理特殊URL
            if (isSpecialUrl(apiUrl)) {
                // 这里应该返回预定义的JSON数据
                // 由于这部分逻辑较复杂，暂时保留在ApiConfig中处理
                return@withContext Result.failure(Exception("特殊URL需要在ApiConfig中处理"))
            }
            
            // 处理URL，包括特殊URL映射和Punycode转换
            val encodedConfigUrl = UrlUtil.processUrl(configUrl)
            val response = service.getConfig(encodedConfigUrl)
            
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: ""
                
                // 保存到缓存
                saveToCache(apiUrl, json)
                
                return@withContext Result.success(json)
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            LOG.e("ConfigRepository", "加载配置失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 处理API URL
     * 
     * @param apiUrl 原始API URL
     * @return Pair<配置URL, 配置密钥>
     */
    private fun processApiUrl(apiUrl: String): Pair<String, String?> {
        var configUrl = apiUrl
        var configKey: String? = null
        val pk = ";pk;"
        
        if (apiUrl.contains(pk)) {
            val a = apiUrl.split(pk)
            configKey = a[1]
            configUrl = when {
                apiUrl.startsWith("clan") -> clanToAddress(a[0])
                apiUrl.startsWith("http") -> a[0]
                else -> "http://${a[0]}"
            }
        } else if (apiUrl.startsWith("clan")) {
            configUrl = clanToAddress(apiUrl)
        } else if (!apiUrl.startsWith("http")) {
            configUrl = "http://$configUrl"
        }
        
        return Pair(configUrl, configKey)
    }
    
    /**
     * 将clan://地址转换为http地址
     * 
     * @param url clan://格式的URL
     * @return 转换后的http URL
     */
    private fun clanToAddress(url: String): String {
        // 这里需要实现clan://地址转换逻辑
        // 暂时返回原始URL，实际实现需要根据ApiConfig中的逻辑
        return url
    }
    
    /**
     * 判断是否为特殊URL
     * 
     * @param url URL
     * @return 是否为特殊URL
     */
    private fun isSpecialUrl(url: String): Boolean {
        return url == "http://ok321.top/tv" || 
               url == "https://7213.kstore.vip/吃猫的鱼" || 
               url.startsWith("https://7213.kstore.vip/") || 
               url == "http://www.饭太硬.com/tv"
    }
    
    /**
     * 保存配置到缓存
     * 
     * @param apiUrl API URL
     * @param json JSON数据
     */
    private fun saveToCache(apiUrl: String, json: String) {
        try {
            val cache = File(App.getInstance().filesDir.absolutePath + "/" + MD5.encode(apiUrl))
            val cacheDir = cache.parentFile
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            if (cache.exists()) {
                cache.delete()
            }
            val fos = FileOutputStream(cache)
            fos.write(json.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            LOG.e("ConfigRepository", "保存缓存失败: ${e.message}")
        }
    }
}
