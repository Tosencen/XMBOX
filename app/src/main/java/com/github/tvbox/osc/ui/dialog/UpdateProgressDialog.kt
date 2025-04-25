package com.github.tvbox.osc.ui.dialog

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.databinding.DialogUpdateProgressBinding
import com.lxj.xpopup.core.CenterPopupView

/**
 * 更新进度对话框
 */
class UpdateProgressDialog(context: Context) : CenterPopupView(context) {

    private lateinit var binding: DialogUpdateProgressBinding
    
    override fun getImplLayoutId(): Int {
        return R.layout.dialog_update_progress
    }
    
    override fun onCreate() {
        super.onCreate()
        binding = DialogUpdateProgressBinding.bind(popupImplView)
        
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
    }
}
