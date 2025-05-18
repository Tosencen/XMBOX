package com.github.tvbox.osc.network.api

import com.github.tvbox.osc.bean.DoubanSuggestBean
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 豆瓣相关API接口
 */
interface DoubanService {
    /**
     * 获取豆瓣搜索建议
     * 
     * @param query 搜索关键词
     * @return 豆瓣搜索建议列表
     */
    @GET("j/subject_suggest")
    suspend fun getDoubanSuggest(@Query("q") query: String): Response<List<DoubanSuggestBean>>
    
    /**
     * 获取豆瓣电影详情
     * 
     * @param url 完整的API URL
     * @return 豆瓣电影详情
     */
    @GET
    suspend fun getMovieDetail(@Url url: String): Response<JsonObject>
}
