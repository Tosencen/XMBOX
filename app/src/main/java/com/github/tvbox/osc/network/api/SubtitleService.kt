package com.github.tvbox.osc.network.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * 字幕相关API接口
 */
interface SubtitleService {
    /**
     * 搜索字幕
     * 
     * @param searchword 搜索关键词
     * @param sort 排序方式
     * @param page 页码
     * @param no_redir 是否不重定向
     * @return 搜索结果HTML
     */
    @GET("sub/")
    suspend fun searchSubtitles(
        @Query("searchword") searchword: String,
        @Query("sort") sort: String = "rank",
        @Query("page") page: Int = 1,
        @Query("no_redir") no_redir: Int = 1
    ): Response<String>
    
    /**
     * 获取字幕详情
     * 
     * @param url 完整的字幕详情URL
     * @return 字幕详情HTML
     */
    @GET
    suspend fun getSubtitleDetail(@Url url: String): Response<String>
    
    /**
     * 下载字幕
     * 
     * @param url 完整的字幕下载URL
     * @return 字幕下载响应
     */
    @GET
    suspend fun downloadSubtitle(@Url url: String): Response<String>
}
