package com.github.tvbox.osc.ui.dialog

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.blankj.utilcode.util.AppUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.databinding.DialogUpdateProgressBinding
import com.github.tvbox.osc.util.MD3ToastUtils
import com.lxj.xpopup.core.CenterPopupView

/**
 * 更新进度对话框
 */
class UpdateProgressDialog(
    context: Context,
    private val targetVersion: String? = null,
    private val onCancelListener: (() -> Unit)? = null
) : CenterPopupView(context) {

    private lateinit var binding: DialogUpdateProgressBinding
    private var isDownloading = true

    override fun getImplLayoutId(): Int {
        return R.layout.dialog_update_progress
    }

    override fun onCreate() {
        super.onCreate()
        binding = DialogUpdateProgressBinding.bind(popupImplView)

        // 应用Material Symbols字体到图标
        val iconView = popupImplView.findViewById<TextView>(R.id.tvUpdateIcon)
        if (iconView != null) {
            com.github.tvbox.osc.util.MaterialSymbolsLoader.apply(iconView)
        }

        // 设置版本信息
        val versionInfoView = popupImplView.findViewById<TextView>(R.id.tvVersionInfo)
        if (versionInfoView != null) {
            val version = targetVersion ?: AppUtils.getAppVersionName()
            versionInfoView.text = "正在更新至 v$version"
        }

        // 设置取消按钮
        val cancelButton = popupImplView.findViewById<Button>(R.id.btnCancel)
        if (cancelButton != null) {
            cancelButton.setOnClickListener {
                if (isDownloading) {
                    onCancelListener?.invoke()
                    dismiss()
                    MD3ToastUtils.showToast("已取消下载")
                }
            }
        }

        // 初始化进度为0
        updateProgress(0)
    }

    /**
     * 更新进度
     * @param progress 进度值(0-100)
     */
    fun updateProgress(progress: Int) {
        binding.progressBar.progress = progress
        binding.tvProgress.text = "$progress%"

        // 如果进度达到100%，标记下载完成
        if (progress >= 100) {
            isDownloading = false
            // 可以在这里隐藏取消按钮或改变其文本
            val cancelButton = popupImplView.findViewById<Button>(R.id.btnCancel)
            cancelButton?.visibility = View.GONE
        }
    }

    /**
     * 处理返回键事件
     */
    override fun onBackPressed(): Boolean {
        if (isDownloading) {
            onCancelListener?.invoke()
            MD3ToastUtils.showToast("已取消下载")
            return super.onBackPressed()
        }
        return super.onBackPressed()
    }
}
