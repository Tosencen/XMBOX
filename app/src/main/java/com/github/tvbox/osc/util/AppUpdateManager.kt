package com.github.tvbox.osc.util

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.github.tvbox.osc.ui.dialog.UpdateProgressDialog
import com.lxj.xpopup.XPopup
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern

/**
 * 应用更新管理器
 * 负责检查更新、下载和安装APK
 */
class AppUpdateManager(private val context: Context) {
    private val TAG = "AppUpdateManager"

    // GitHub API 地址
    private val GITHUB_API_URL = "https://api.github.com/repos/Tosencen/XMBOX/releases/latest"

    // 版本号正则表达式
    private val VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)")

    // 下载管理器
    private var downloadManager: DownloadManager? = null
    private var downloadId: Long = 0

    // 更新进度对话框
    private var progressDialog: UpdateProgressDialog? = null

    // 下载完成广播接收器
    private var downloadCompleteReceiver: BroadcastReceiver? = null

    // 目标版本号
    private var targetVersion: String? = null

    // 是否正在下载
    private var isDownloading = false

    // 更新检查回调接口
    interface UpdateCheckCallback {
        fun onUpdateAvailable(newVersion: String)
        fun onNoUpdateAvailable()
        fun onCheckFailed()
    }

    /**
     * 检查更新
     * @param showNoUpdate 是否显示无更新提示
     */
    fun checkUpdate(showNoUpdate: Boolean = false) {
        try {
            // 获取当前版本
            val currentVersion = com.blankj.utilcode.util.AppUtils.getAppVersionName()
            Log.d(TAG, "当前版本: $currentVersion")

            // 请求GitHub API获取最新版本
            com.lzy.okgo.OkGo.get<String>(GITHUB_API_URL)
                .headers("Accept", "application/vnd.github.v3+json")
                .execute(object : com.lzy.okgo.callback.StringCallback() {
                    override fun onSuccess(response: com.lzy.okgo.model.Response<String>) {
                        try {
                            val jsonObject = JSONObject(response.body())
                            val latestVersion = jsonObject.getString("tag_name").replace("v", "")
                            val releaseNotes = jsonObject.getString("body")

                            // 获取下载链接
                            val assets = jsonObject.getJSONArray("assets")
                            var downloadUrl = ""
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val name = asset.getString("name")
                                if (name.endsWith(".apk")) {
                                    downloadUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }

                            if (downloadUrl.isEmpty()) {
                                downloadUrl = jsonObject.getString("html_url")
                            }

                            Log.d(TAG, "最新版本: $latestVersion, 下载链接: $downloadUrl")

                            // 比较版本号
                            if (isNewerVersion(currentVersion, latestVersion)) {
                                showUpdateDialog(latestVersion, releaseNotes, downloadUrl)
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

                    override fun onError(response: com.lzy.okgo.model.Response<String>) {
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
    private fun showUpdateDialog(newVersion: String, releaseNotes: String, downloadUrl: String) {
        if (context is Activity && !context.isFinishing) {
            MD3DialogUtils.showAlertDialog(
                context,
                "发现新版本: v$newVersion",
                releaseNotes,
                "立即更新",
                "取消",
                { _, _ -> downloadApk(downloadUrl, newVersion) },
                null
            )
        }
    }

    /**
     * 下载APK
     */
    private fun downloadApk(downloadUrl: String, version: String) {
        try {
            // 设置目标版本号和下载状态
            targetVersion = version
            isDownloading = true

            // 创建下载目录
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir == null) {
                MD3ToastUtils.showToast("无法获取下载目录")
                isDownloading = false
                return
            }

            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // 创建APK文件
            val apkFile = File(downloadDir, "XMBOX_v${version}.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // 显示进度对话框
            showProgressDialog()

            // 注册下载完成广播接收器
            registerDownloadCompleteReceiver(apkFile)

            // 使用DownloadManager下载APK
            downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("XMBOX 更新")
                .setDescription("正在下载 v$version")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(apkFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadId = downloadManager!!.enqueue(request)

            // 启动进度更新
            updateProgress()
        } catch (e: Exception) {
            Log.e(TAG, "下载APK失败", e)
            MD3ToastUtils.showToast("下载失败: ${e.message}")
            dismissProgressDialog()
        }
    }

    /**
     * 显示进度对话框
     */
    private fun showProgressDialog() {
        if (context is Activity && !context.isFinishing) {
            progressDialog = UpdateProgressDialog(context, targetVersion) {
                // 取消下载的回调
                cancelDownload()
            }
            XPopup.Builder(context)
                .dismissOnBackPressed(true)  // 允许按返回键取消
                .dismissOnTouchOutside(false)
                .asCustom(progressDialog)
                .show()
        }
    }

    /**
     * 取消下载
     */
    private fun cancelDownload() {
        try {
            if (isDownloading && downloadId > 0) {
                // 取消下载任务
                downloadManager?.remove(downloadId)
                isDownloading = false

                // 清理资源
                unregisterDownloadCompleteReceiver()

                Log.d(TAG, "下载已取消")
            }
        } catch (e: Exception) {
            Log.e(TAG, "取消下载失败", e)
        }
    }

    /**
     * 关闭进度对话框
     */
    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    /**
     * 更新下载进度
     */
    private fun updateProgress() {
        Thread {
            try {
                var isDownloading = true
                while (isDownloading) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    downloadManager?.query(query)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                            if (statusIndex != -1 && bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                                val status = cursor.getInt(statusIndex)
                                val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                                val bytesTotal = cursor.getLong(bytesTotalIndex)

                                when (status) {
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        isDownloading = false
                                        Log.d(TAG, "下载完成")
                                    }
                                    DownloadManager.STATUS_FAILED -> {
                                        isDownloading = false
                                        Log.e(TAG, "下载失败")
                                        (context as Activity).runOnUiThread {
                                            MD3ToastUtils.showToast("下载失败")
                                            dismissProgressDialog()
                                        }
                                    }
                                    DownloadManager.STATUS_RUNNING -> {
                                        if (bytesTotal > 0) {
                                            val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                                            (context as Activity).runOnUiThread {
                                                progressDialog?.updateProgress(progress)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Thread.sleep(500)
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新进度失败", e)
            }
        }.start()
    }

    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadCompleteReceiver(apkFile: File) {
        try {
            // 注销之前的接收器
            unregisterDownloadCompleteReceiver()

            // 创建新的接收器
            downloadCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        dismissProgressDialog()
                        installApk(apkFile)
                    }
                }
            }

            // 注册接收器
            context.registerReceiver(
                downloadCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        } catch (e: Exception) {
            Log.e(TAG, "注册下载完成接收器失败", e)
        }
    }

    /**
     * 注销下载完成广播接收器
     */
    private fun unregisterDownloadCompleteReceiver() {
        try {
            if (downloadCompleteReceiver != null) {
                context.unregisterReceiver(downloadCompleteReceiver)
                downloadCompleteReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "注销下载完成接收器失败", e)
        }
    }

    /**
     * 安装APK
     */
    private fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                MD3ToastUtils.showToast("安装包不存在")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0及以上需要使用FileProvider
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.xmbox.app.fileprovider",
                    apkFile
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(
                    Uri.fromFile(apkFile),
                    "application/vnd.android.package-archive"
                )
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "安装APK失败", e)
            MD3ToastUtils.showToast("安装失败: ${e.message}")
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
     * 静默检查更新（不显示对话框）
     * @param callback 更新检查回调
     */
    fun checkUpdateSilently(callback: UpdateCheckCallback) {
        try {
            // 获取当前版本
            val currentVersion = com.blankj.utilcode.util.AppUtils.getAppVersionName()
            Log.d(TAG, "当前版本: $currentVersion")

            // 请求GitHub API获取最新版本
            com.lzy.okgo.OkGo.get<String>(GITHUB_API_URL)
                .headers("Accept", "application/vnd.github.v3+json")
                .execute(object : com.lzy.okgo.callback.StringCallback() {
                    override fun onSuccess(response: com.lzy.okgo.model.Response<String>) {
                        try {
                            val jsonObject = JSONObject(response.body())
                            val latestVersion = jsonObject.getString("tag_name").replace("v", "")

                            // 比较版本号
                            if (isNewerVersion(currentVersion, latestVersion)) {
                                callback.onUpdateAvailable(latestVersion)
                            } else {
                                callback.onNoUpdateAvailable()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "解析更新信息失败", e)
                            callback.onCheckFailed()
                        }
                    }

                    override fun onError(response: com.lzy.okgo.model.Response<String>) {
                        Log.e(TAG, "检查更新请求失败: ${response.message()}")
                        callback.onCheckFailed()
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "检查更新过程中发生错误", e)
            callback.onCheckFailed()
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        // 如果正在下载，取消下载
        if (isDownloading) {
            cancelDownload()
        }

        unregisterDownloadCompleteReceiver()
        dismissProgressDialog()
        isDownloading = false
    }
}
