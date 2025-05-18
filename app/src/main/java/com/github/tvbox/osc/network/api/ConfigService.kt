package com.github.tvbox.osc.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

/**
 * 配置相关API接口
 */
interface ConfigService {
    /**
     * 获取配置内容
     * 
     * @param url 配置地址
     * @return 配置内容
     */
    @GET
    @Headers(
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
        "User-Agent: okhttp/3.15"
    )
    suspend fun getConfig(@Url url: String): Response<ResponseBody>
}
