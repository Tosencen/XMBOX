package com.github.tvbox.osc.ui.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.databinding.DialogUpdateBinding
import com.github.tvbox.osc.util.UpdateChecker
import com.lxj.xpopup.core.CenterPopupView

/**
 * 应用更新对话框
 */
class UpdateDialog(
    context: Context,
    private val newVersion: String,
    private val releaseNotes: String,
    private val downloadUrl: String
) : CenterPopupView(context) {

    private lateinit var binding: DialogUpdateBinding

    override fun getImplLayoutId(): Int {
        return R.layout.dialog_update
    }

    override fun onCreate() {
        super.onCreate()
        binding = DialogUpdateBinding.bind(popupImplView)

        // 设置版本信息
        binding.tvVersion.text = "发现新版本：v$newVersion"
        
        // 设置更新内容
        binding.tvReleaseNotes.text = releaseNotes
        
        // 设置按钮点击事件
        binding.btnUpdate.setOnClickListener {
            UpdateChecker.openDownloadUrl(context, downloadUrl)
            dismiss()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
