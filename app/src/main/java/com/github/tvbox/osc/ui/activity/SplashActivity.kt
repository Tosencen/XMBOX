package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.databinding.ActivitySplashBinding

class SplashActivity : BaseVbActivity<ActivitySplashBinding>() {
    override fun init() {
        // 记录应用启动日志
        Log.d("SplashActivity", "应用启动 - 开始初始化")

        try {
            // 设置正常启动标志
            App.getInstance().isNormalStart = true
            Log.d("SplashActivity", "设置正常启动标志成功")

            // 添加一个ImageView来显示启动页插图
            val imageView = ImageView(this)
            val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            imageView.layoutParams = layoutParams
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            try {
                // 直接加载启动页插图
                val drawable = resources.getDrawable(R.drawable.iv_splash, theme)
                imageView.setImageDrawable(drawable)
                // 设置缩放比例为原始大小的65%
                imageView.scaleX = 0.65f
                imageView.scaleY = 0.65f
                mBinding.root.addView(imageView)
                Log.d("SplashActivity", "成功加载启动页插图")
            } catch (e: Exception) {
                Log.e("SplashActivity", "加载启动页插图失败: ${e.message}")
            }

            // 预加载一些必要的组件，但不初始化可能导致崩溃的功能
            try {
                // 检查设备架构
                val arch = System.getProperty("os.arch")
                val isArmArchitecture = arch != null && (arch.contains("arm") || arch.contains("ARM"))
                Log.i("SplashActivity", "设备架构: $arch, 是否为ARM架构: $isArmArchitecture")

                // 检查应用版本
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val versionName = packageInfo.versionName
                val versionCode = packageInfo.versionCode
                Log.i("SplashActivity", "应用版本: $versionName ($versionCode)")

                // 检查存储空间
                val externalCacheDir = externalCacheDir
                val internalCacheDir = cacheDir
                Log.i("SplashActivity", "外部缓存目录: $externalCacheDir")
                Log.i("SplashActivity", "内部缓存目录: $internalCacheDir")

                Log.d("SplashActivity", "预加载组件成功")
            } catch (e: Exception) {
                Log.e("SplashActivity", "预加载组件失败: ${e.message}")
                e.printStackTrace()
            }

            // 延迟启动主界面
            mBinding.root.postDelayed({
                try {
                    Log.d("SplashActivity", "准备启动主界面")
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    Log.d("SplashActivity", "成功启动主界面")
                } catch (e: Exception) {
                    Log.e("SplashActivity", "启动主界面失败: ${e.message}")
                }
            }, 1000) // 延长显示时间以便观察
        } catch (e: Exception) {
            // 捕获所有可能的异常，确保应用不会在启动时崩溃
            Log.e("SplashActivity", "应用启动过程中发生严重错误: ${e.message}")
            e.printStackTrace()

            // 尝试恢复并继续启动
            try {
                mBinding.root.postDelayed({
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                }, 1000)
            } catch (e2: Exception) {
                Log.e("SplashActivity", "恢复启动失败，应用可能无法正常运行: ${e2.message}")
            }
        }
    }
}