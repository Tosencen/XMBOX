package com.github.tvbox.osc.network.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 搜索相关API接口
 */
interface SearchService {
    /**
     * 获取热门搜索词
     * 
     * @param channelId 频道ID
     * @param timestamp 时间戳
     * @return 热门搜索词
     */
    @GET("x/api/hot_search")
    suspend fun getHotSearch(
        @Query("channdlId") channelId: String = "0",
        @Query("_") timestamp: Long = System.currentTimeMillis()
    ): Response<JsonObject>
    
    /**
     * 获取搜索建议
     * 
     * @param url 完整的API URL
     * @return 搜索建议
     */
    @GET
    suspend fun getSearchSuggestions(@Url url: String): Response<JsonObject>
}
