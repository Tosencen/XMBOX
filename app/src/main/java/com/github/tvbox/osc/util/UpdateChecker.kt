package com.github.tvbox.osc.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.blankj.utilcode.util.AppUtils
import com.github.tvbox.osc.ui.dialog.UpdateDialog
import com.github.tvbox.osc.util.MD3DialogUtils
import com.lxj.xpopup.XPopup
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * 应用更新检查工具类
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    
    // GitHub API 地址
    private const val GITHUB_API_URL = "https://api.github.com/repos/Tosencen/XMBOX/releases/latest"
    
    // 版本号正则表达式
    private val VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)")
    
    /**
     * 检查更新
     * @param context 上下文
     * @param showNoUpdate 是否显示无更新提示
     */
    fun checkUpdate(context: Context, showNoUpdate: Boolean = false) {
        try {
            // 获取当前版本
            val currentVersion = AppUtils.getAppVersionName()
            Log.d(TAG, "当前版本: $currentVersion")
            
            // 请求GitHub API获取最新版本
            OkGo.get<String>(GITHUB_API_URL)
                .headers("Accept", "application/vnd.github.v3+json")
                .execute(object : StringCallback() {
                    override fun onSuccess(response: Response<String>) {
                        try {
                            val jsonObject = JSONObject(response.body())
                            val latestVersion = jsonObject.getString("tag_name").replace("v", "")
                            val releaseNotes = jsonObject.getString("body")
                            val downloadUrl = jsonObject.getString("html_url")
                            
                            Log.d(TAG, "最新版本: $latestVersion")
                            
                            // 比较版本号
                            if (isNewerVersion(currentVersion, latestVersion)) {
                                showUpdateDialog(context, latestVersion, releaseNotes, downloadUrl)
                            } else if (showNoUpdate) {
                                MD3ToastUtils.showToast("当前已是最新版本")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "解析更新信息失败", e)
                            if (showNoUpdate) {
                                MD3ToastUtils.showToast("检查更新失败")
                            }
                        }
                    }
                    
                    override fun onError(response: Response<String>) {
                        Log.e(TAG, "检查更新请求失败: ${response.message()}")
                        if (showNoUpdate) {
                            MD3ToastUtils.showToast("检查更新失败")
                        }
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "检查更新过程中发生错误", e)
            if (showNoUpdate) {
                MD3ToastUtils.showToast("检查更新失败")
            }
        }
    }
    
    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(context: Context, newVersion: String, releaseNotes: String, downloadUrl: String) {
        if (context is Activity && !context.isFinishing) {
            XPopup.Builder(context)
                .asCustom(UpdateDialog(context, newVersion, releaseNotes, downloadUrl))
                .show()
        }
    }
    
    /**
     * 比较版本号，检查是否有新版本
     * @param currentVersion 当前版本号
     * @param newVersion 新版本号
     * @return 如果新版本号大于当前版本号，返回true
     */
    private fun isNewerVersion(currentVersion: String, newVersion: String): Boolean {
        try {
            val currentMatcher = VERSION_PATTERN.matcher(currentVersion)
            val newMatcher = VERSION_PATTERN.matcher(newVersion)
            
            if (currentMatcher.find() && newMatcher.find()) {
                // 解析主版本号
                val currentMajor = currentMatcher.group(1)?.toInt() ?: 0
                val newMajor = newMatcher.group(1)?.toInt() ?: 0
                
                // 比较主版本号
                if (newMajor > currentMajor) return true
                if (newMajor < currentMajor) return false
                
                // 解析次版本号
                val currentMinor = currentMatcher.group(2)?.toInt() ?: 0
                val newMinor = newMatcher.group(2)?.toInt() ?: 0
                
                // 比较次版本号
                if (newMinor > currentMinor) return true
                if (newMinor < currentMinor) return false
                
                // 解析修订版本号
                val currentPatch = currentMatcher.group(3)?.toInt() ?: 0
                val newPatch = newMatcher.group(3)?.toInt() ?: 0
                
                // 比较修订版本号
                return newPatch > currentPatch
            }
            
            // 如果版本号格式不匹配，直接比较字符串
            return newVersion != currentVersion
        } catch (e: Exception) {
            Log.e(TAG, "版本比较失败", e)
            return false
        }
    }
    
    /**
     * 打开浏览器下载更新
     */
    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "打开下载链接失败", e)
            MD3ToastUtils.showToast("打开下载链接失败")
        }
    }
}
