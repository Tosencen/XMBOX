package com.github.tvbox.osc.network.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

/**
 * 更新相关API接口
 */
interface UpdateService {
    /**
     * 获取GitHub发布信息
     * 
     * @param url GitHub API URL
     * @param accept 接受的内容类型
     * @return GitHub发布信息的JSON响应
     */
    @GET
    suspend fun getGithubReleaseInfo(
        @Url url: String,
        @Header("Accept") accept: String = "application/vnd.github.v3+json"
    ): Response<JsonObject>
}
