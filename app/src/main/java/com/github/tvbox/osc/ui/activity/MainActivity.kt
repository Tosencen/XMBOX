package com.github.tvbox.osc.ui.activity

import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.constant.IntentKey
import com.xmbox.app.databinding.ActivityMainBinding
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.fragment.GridFragment
import com.github.tvbox.osc.ui.fragment.HomeFragment
import com.github.tvbox.osc.ui.fragment.MyFragment
import com.github.tvbox.osc.ui.widget.BlurView
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.AppUpdateManager
import com.github.tvbox.osc.util.MD3ToastUtils
import com.orhanobut.hawk.Hawk
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.system.exitProcess

class MainActivity : BaseVbActivity<ActivityMainBinding>() {

    var fragments = listOf(HomeFragment(),MyFragment())
    var useCacheConfig = false
    private var exitTime = 0L
    private var updateManager: AppUpdateManager? = null

    override fun init() {
        // 注册EventBus
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        useCacheConfig = intent.extras?.getBoolean(IntentKey.CACHE_CONFIG_CHANGED, false)?:false

        // 检查是否是应用重启
        val isAppRestarted = intent.extras?.getBoolean("app_restarted", false) ?: false
        if (isAppRestarted) {
            MD3ToastUtils.showToast("应用已重新启动，性能已优化")
            android.util.Log.i("MainActivity", "应用已从长时间后台状态重启")
        }

        mBinding.vp.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }
        }

        mBinding.bottomNav.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            mBinding.vp.setCurrentItem(menuItem.order, false)
            true
        }
        mBinding.vp.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mBinding.bottomNav.menu.getItem(position).setChecked(true)
            }
        })

        // 初始化更新管理器并自动检查更新
        initAutoUpdateCheck()

        // 初始化磨砂模糊效果
        initBlurEffect()
    }

    /**
     * 初始化自动更新检查
     */
    private fun initAutoUpdateCheck() {
        try {
            updateManager = AppUpdateManager(this)

            // 延迟3秒后自动检查更新，避免影响应用启动速度
            Handler(Looper.getMainLooper()).postDelayed({
                updateManager?.checkUpdateSilently(object : AppUpdateManager.UpdateCheckCallback {
                    override fun onUpdateAvailable(newVersion: String) {
                        // 有新版本可用，自动弹出更新弹窗
                        android.util.Log.d("MainActivity", "检测到新版本: $newVersion，自动弹出更新弹窗")
                        updateManager?.checkUpdate(false) // 显示更新弹窗
                    }

                    override fun onNoUpdateAvailable() {
                        // 没有新版本，不做任何处理
                        android.util.Log.d("MainActivity", "当前已是最新版本")
                    }

                    override fun onCheckFailed() {
                        // 检查失败，不做任何处理
                        android.util.Log.d("MainActivity", "自动检查更新失败")
                    }
                })
            }, 3000) // 延迟3秒
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "初始化自动更新检查失败: ${e.message}")
        }
    }

    /**
     * 初始化磨砂毛玻璃效果 - 类似logo的高级质感
     */
    private fun initBlurEffect() {
        try {
            val blurView = findViewById<BlurView>(com.xmbox.app.R.id.blur_container)
            blurView?.let {
                // 设置磨砂毛玻璃参数 - 参考logo效果
                it.setBlurRadius(20f) // 增加模糊半径，更强的磨砂效果
                it.setScaleFactor(0.3f) // 稍微提高缩放因子，保持性能
                it.setBlurEnabled(true) // 启用模糊效果

                android.util.Log.d("MainActivity", "磨砂毛玻璃效果已初始化 - logo风格")
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "磨砂毛玻璃效果初始化失败", e)
        }
    }

    override fun onBackPressed() {
        if (mBinding.vp.currentItem == 1) {
            mBinding.vp.currentItem = 0
            return
        }
        val homeFragment = fragments[0] as HomeFragment
        if (!homeFragment.isAdded) { // 资源不足销毁重建时未挂载到activity时getChildFragmentManager会崩溃
            confirmExit()
            return
        }
        val childFragments = homeFragment.allFragments
        if (childFragments.isEmpty()) { //加载中(没有tab)
            confirmExit()
            return
        }
        val fragment: Fragment = childFragments[homeFragment.tabIndex]
        if (fragment is GridFragment) { // 首页数据源动态加载的tab
            if (!fragment.restoreView()) { // 有回退的view,先回退(AList等文件夹列表),没有可回退的,返到主页tab
                if (!homeFragment.scrollToFirstTab()) {
                    confirmExit()
                }
            }
        } else {
            confirmExit()
        }
    }

    private fun confirmExit() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            ToastUtils.showShort("再按一次退出程序")
            exitTime = System.currentTimeMillis()
        } else {
            ActivityUtils.finishAllActivities(true)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_API_URL_CHANGE) {
            // 显示加载提示
            MD3ToastUtils.showToast("正在切换订阅源...")

            // 清理旧的数据源选择，让新订阅源使用默认的第一个数据源
            Hawk.delete(HawkConfig.HOME_API)

            // 获取HomeFragment并刷新数据
            val homeFragment = fragments[0] as HomeFragment
            if (homeFragment.isAdded) {
                // 重置初始化状态
                homeFragment.dataInitOk = false
                homeFragment.jarInitOk = false
                homeFragment.onlyConfigChanged = false // 强制完全重新加载
                // 重新加载数据
                homeFragment.initData()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销EventBus
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        // 清理更新管理器
        updateManager = null
    }
}