package com.github.tvbox.osc.network.repository

import android.util.Log
import com.github.tvbox.osc.network.RetrofitClient
import com.github.tvbox.osc.network.api.UpdateService
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 更新仓库
 * 负责处理应用更新相关的网络请求和数据处理
 */
class UpdateRepository {
    private val TAG = "UpdateRepository"
    private val service = RetrofitClient.createService(UpdateService::class.java)
    
    // GitHub API 地址
    private val GITHUB_API_URL = "https://api.github.com/repos/Tosencen/XMBOX/releases/latest"
    
    /**
     * 获取最新版本信息
     * 
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun getLatestVersion(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val response = service.getGithubReleaseInfo(GITHUB_API_URL)
            
            if (response.isSuccessful) {
                val jsonObject = response.body()
                if (jsonObject == null) {
                    return@withContext Result.failure(Exception("响应内容为空"))
                }
                
                // 解析版本号
                val latestVersion = jsonObject.get("tag_name").asString.replace("v", "")
                val releaseNotes = jsonObject.get("body").asString
                val htmlUrl = jsonObject.get("html_url").asString
                
                // 获取下载链接
                var downloadUrl = htmlUrl
                if (jsonObject.has("assets") && jsonObject.getAsJsonArray("assets").size() > 0) {
                    val assets = jsonObject.getAsJsonArray("assets")
                    for (i in 0 until assets.size()) {
                        val asset = assets.get(i).asJsonObject
                        val name = asset.get("name").asString
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.get("browser_download_url").asString
                            break
                        }
                    }
                }
                
                return@withContext Result.success(
                    ReleaseInfo(
                        version = latestVersion,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl
                    )
                )
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取最新版本失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
}

/**
 * 发布信息数据类
 */
data class ReleaseInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String
)
