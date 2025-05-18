package com.github.tvbox.osc.network.repository

import com.github.tvbox.osc.bean.Subscription
import com.github.tvbox.osc.network.RetrofitClient
import com.github.tvbox.osc.network.api.SubscriptionService
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.UrlUtil
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 订阅仓库
 * 负责处理订阅相关的网络请求和数据处理
 */
class SubscriptionRepository {
    private val service = RetrofitClient.createService(SubscriptionService::class.java)
    
    /**
     * 添加订阅
     * 
     * @param name 订阅名称
     * @param url 订阅地址
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun addSubscription(name: String, url: String): Result<SubscriptionResult> = withContext(Dispatchers.IO) {
        try {
            // 处理本地订阅
            if (url.startsWith("clan://")) {
                return@withContext Result.success(SubscriptionResult.SingleSubscription(
                    Subscription(name, url).apply { isChecked = false }
                ))
            }
            
            // 处理特定URL，直接添加不解析
            if (url == "http://ok321.top/tv" || 
                url == "https://7213.kstore.vip/吃猫的鱼" || 
                url.startsWith("https://7213.kstore.vip/") || 
                url == "http://www.饭太硬.com/tv") {
                
                return@withContext Result.success(SubscriptionResult.SingleSubscription(
                    Subscription(name, url).apply { isChecked = false }
                ))
            }
            
            // 处理HTTP请求
            if (url.startsWith("http")) {
                // 处理URL，包括特殊URL映射和Punycode转换
                val encodedUrl = UrlUtil.processUrl(url)
                val response = service.fetchSubscription(encodedUrl)
                
                if (response.isSuccessful) {
                    val jsonElement = response.body()
                    if (jsonElement == null) {
                        return@withContext Result.failure(Exception("响应内容为空"))
                    }
                    
                    val jsonObject = jsonElement.asJsonObject
                    
                    // 检查是否是多线路
                    if (jsonObject.has("urls") && jsonObject.get("urls").isJsonArray) {
                        val urlsArray = jsonObject.getAsJsonArray("urls")
                        if (urlsArray.size() > 0 && 
                            urlsArray[0].isJsonObject && 
                            urlsArray[0].asJsonObject.has("url") && 
                            urlsArray[0].asJsonObject.has("name")) {
                            
                            // 处理多线路格式
                            val subscriptions = mutableListOf<Subscription>()
                            for (i in 0 until urlsArray.size()) {
                                val obj = urlsArray[i].asJsonObject
                                val subName = obj.get("name").asString.trim()
                                    .replace("<|>|《|》|-".toRegex(), "")
                                val subUrl = obj.get("url").asString.trim()
                                subscriptions.add(Subscription(subName, subUrl))
                            }
                            
                            return@withContext Result.success(SubscriptionResult.MultipleSubscriptions(subscriptions))
                        }
                    }
                    
                    // 检查是否是多仓
                    if (jsonObject.has("storeHouse") && jsonObject.get("storeHouse").isJsonArray) {
                        val storeHouseArray = jsonObject.getAsJsonArray("storeHouse")
                        if (storeHouseArray.size() > 0 && 
                            storeHouseArray[0].isJsonObject && 
                            storeHouseArray[0].asJsonObject.has("sourceName") && 
                            storeHouseArray[0].asJsonObject.has("sourceUrl")) {
                            
                            // 处理多仓格式
                            val sources = mutableListOf<SourceItem>()
                            for (i in 0 until storeHouseArray.size()) {
                                val obj = storeHouseArray[i].asJsonObject
                                val sourceName = obj.get("sourceName").asString.trim()
                                    .replace("<|>|《|》|-".toRegex(), "")
                                val sourceUrl = obj.get("sourceUrl").asString.trim()
                                sources.add(SourceItem(sourceName, sourceUrl))
                            }
                            
                            return@withContext Result.success(SubscriptionResult.MultipleStoreHouses(sources))
                        }
                    }
                    
                    // 单线路
                    return@withContext Result.success(SubscriptionResult.SingleSubscription(
                        Subscription(name, url).apply { isChecked = false }
                    ))
                } else {
                    return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
                }
            } else {
                return@withContext Result.failure(Exception("订阅格式不正确"))
            }
        } catch (e: Exception) {
            LOG.e("SubscriptionRepository", "添加订阅失败: ${e.message}")
            // 异常情况下作为单线路处理
            return@withContext Result.success(SubscriptionResult.SingleSubscription(
                Subscription(name, url).apply { isChecked = false }
            ))
        }
    }
}

/**
 * 订阅结果密封类
 */
sealed class SubscriptionResult {
    /**
     * 单个订阅
     */
    data class SingleSubscription(val subscription: Subscription) : SubscriptionResult()
    
    /**
     * 多个订阅（多线路）
     */
    data class MultipleSubscriptions(val subscriptions: List<Subscription>) : SubscriptionResult()
    
    /**
     * 多个仓库
     */
    data class MultipleStoreHouses(val sources: List<SourceItem>) : SubscriptionResult()
}

/**
 * 源项目
 */
data class SourceItem(val sourceName: String, val sourceUrl: String)
