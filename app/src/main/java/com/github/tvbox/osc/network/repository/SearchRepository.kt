package com.github.tvbox.osc.network.repository

import android.util.Log
import com.github.tvbox.osc.network.RetrofitClient
import com.github.tvbox.osc.network.api.SearchService
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 搜索仓库
 * 负责处理搜索相关的网络请求和数据处理
 */
class SearchRepository {
    private val TAG = "SearchRepository"
    private val service = RetrofitClient.createService(SearchService::class.java)
    
    // 搜索API基础URL
    private val HOT_SEARCH_BASE_URL = "https://node.video.qq.com/"
    private val SUGGEST_BASE_URL = "https://suggest.video.iqiyi.com/"
    
    /**
     * 获取热门搜索词
     * 
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun getHotSearch(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = service.getHotSearch()
            
            if (response.isSuccessful) {
                val jsonObject = response.body()
                if (jsonObject != null) {
                    val hots = ArrayList<String>()
                    val itemList = jsonObject
                        .getAsJsonObject("data")
                        .getAsJsonObject("mapResult")
                        .getAsJsonObject("0")
                        .getAsJsonArray("listInfo")
                    
                    for (ele in itemList) {
                        val obj = ele.asJsonObject
                        hots.add(obj.get("title").asString.trim()
                            .replace("<|>|《|》|-".toRegex(), "").split(" ".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0])
                    }
                    
                    return@withContext Result.success(hots)
                } else {
                    return@withContext Result.failure(Exception("响应内容为空"))
                }
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取热门搜索词失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 获取搜索建议
     * 
     * @param query 搜索关键词
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun getSearchSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = service.getSearchSuggestions("${SUGGEST_BASE_URL}?if=mobile&key=$query")
            
            if (response.isSuccessful) {
                val jsonObject = response.body()
                if (jsonObject != null) {
                    val titles = ArrayList<String>()
                    val datas = jsonObject.getAsJsonArray("data")
                    
                    for (data in datas) {
                        val item = data.asJsonObject
                        titles.add(item.get("name").asString.trim())
                    }
                    
                    return@withContext Result.success(titles)
                } else {
                    return@withContext Result.failure(Exception("响应内容为空"))
                }
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取搜索建议失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
}
