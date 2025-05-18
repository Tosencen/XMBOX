package com.github.tvbox.osc.network.api

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * 订阅相关API接口
 */
interface SubscriptionService {
    /**
     * 获取订阅内容
     * 
     * @param url 订阅地址
     * @return 订阅内容的JSON响应
     */
    @GET
    suspend fun fetchSubscription(@Url url: String): Response<JsonElement>
}
