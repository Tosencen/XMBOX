package com.github.tvbox.osc.network.repository

import android.util.Log
import com.github.tvbox.osc.bean.DoubanSuggestBean
import com.github.tvbox.osc.network.RetrofitClient
import com.github.tvbox.osc.network.api.DoubanService
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 豆瓣仓库
 * 负责处理豆瓣相关的网络请求和数据处理
 */
class DoubanRepository {
    private val TAG = "DoubanRepository"
    private val service = RetrofitClient.createService(DoubanService::class.java)
    
    // 豆瓣API基础URL
    private val DOUBAN_BASE_URL = "https://movie.douban.com/"
    private val WMDB_BASE_URL = "https://api.wmdb.tv/"
    
    /**
     * 获取豆瓣搜索建议
     * 
     * @param query 搜索关键词
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun getDoubanSuggest(query: String): Result<List<DoubanSuggestBean>> = withContext(Dispatchers.IO) {
        try {
            val response = service.getDoubanSuggest(query.trim())
            
            if (response.isSuccessful) {
                val suggestions = response.body()
                if (suggestions != null) {
                    return@withContext Result.success(suggestions)
                } else {
                    return@withContext Result.failure(Exception("响应内容为空"))
                }
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取豆瓣搜索建议失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 获取电影详情
     * 
     * @param id 豆瓣ID
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun getMovieDetail(id: String): Result<MovieDetail> = withContext(Dispatchers.IO) {
        try {
            val response = service.getMovieDetail("${WMDB_BASE_URL}movie/api?id=$id")
            
            if (response.isSuccessful) {
                val jsonObject = response.body()
                if (jsonObject != null) {
                    val imdbRating = jsonObject.get("imdbRating").asString
                    val doubanRating = jsonObject.get("doubanRating").asString
                    val rottenRating = jsonObject.get("rottenRating").asString
                    
                    return@withContext Result.success(
                        MovieDetail(
                            imdbRating = imdbRating,
                            doubanRating = doubanRating,
                            rottenRating = rottenRating
                        )
                    )
                } else {
                    return@withContext Result.failure(Exception("响应内容为空"))
                }
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取电影详情失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
}

/**
 * 电影详情数据类
 */
data class MovieDetail(
    val imdbRating: String,
    val doubanRating: String,
    val rottenRating: String
)
