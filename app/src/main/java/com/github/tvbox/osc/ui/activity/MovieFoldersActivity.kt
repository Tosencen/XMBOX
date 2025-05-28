package com.github.tvbox.osc.ui.activity

import android.os.Build
import android.os.Bundle
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.VideoFolder
import com.github.tvbox.osc.bean.VideoInfo
import com.github.tvbox.osc.databinding.ActivityMovieFoldersBinding
import com.github.tvbox.osc.ui.adapter.FolderAdapter
import com.github.tvbox.osc.util.PermissionHelper
import com.github.tvbox.osc.util.Utils
import com.hjq.permissions.OnPermissionCallback
import java.util.function.Function
import java.util.stream.Collectors

class MovieFoldersActivity : BaseVbActivity<ActivityMovieFoldersBinding>() {

    private var mFolderAdapter = FolderAdapter()
    override fun init() {
        mBinding.rv.setAdapter(mFolderAdapter)
        mFolderAdapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                val videoFolder = adapter.getItem(position) as VideoFolder?
                if (videoFolder != null) {
                    val bundle = Bundle()
                    bundle.putString("bucketDisplayName", videoFolder.name)
                    jumpActivity(VideoListActivity::class.java, bundle)
                }
            }

        // 检查并请求存储权限
        checkStoragePermission()
    }

    override fun onResume() {
        super.onResume()
        groupVideos()
    }

    /**
     * 检查存储权限
     */
    private fun checkStoragePermission() {
        PermissionHelper.requestBasicStoragePermission(this, object : OnPermissionCallback {
            override fun onGranted(permissions: List<String>, all: Boolean) {
                if (all) {
                    // 权限已授予，加载视频列表
                    groupVideos()
                }
            }

            override fun onDenied(permissions: List<String>, never: Boolean) {
                // 权限被拒绝，显示空列表或提示信息
                mFolderAdapter.setNewData(ArrayList())
                if (never) {
                    // 用户选择了"不再询问"，可以显示更详细的提示
                    com.github.tvbox.osc.util.MD3ToastUtils.showToast("需要存储权限才能访问本地视频文件")
                }
            }
        })
    }

    /**
     * 按文件夹名字分组视频
     */
    private fun groupVideos() {
        try {
            val videoList = Utils.getVideoList()
            val videoMap = videoList.stream()
                .collect(
                    Collectors.groupingBy { obj: VideoInfo -> obj.bucketDisplayName }
                )
            val videoFolders: MutableList<VideoFolder> = ArrayList()
            videoMap.forEach { (key: String?, value: List<VideoInfo>?) ->
                val videoFolder = VideoFolder(key, value)
                videoFolders.add(videoFolder)
            }
            mFolderAdapter.setNewData(videoFolders)
        } catch (e: Exception) {
            // 处理可能的权限或其他异常
            e.printStackTrace()
            mFolderAdapter.setNewData(ArrayList())
            com.github.tvbox.osc.util.MD3ToastUtils.showToast("加载本地视频失败，请检查存储权限")
        }
    }
}