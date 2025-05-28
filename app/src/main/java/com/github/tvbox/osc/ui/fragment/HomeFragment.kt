package com.github.tvbox.osc.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.tabs.TabLayoutMediator
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.MD3ToastUtils
import com.github.tvbox.osc.util.RecyclerViewOptimizer
import com.github.tvbox.osc.util.ThreadPoolManager
import com.xmbox.app.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.api.ApiConfig.LoadConfigCallback
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.base.BaseVbFragment
import com.github.tvbox.osc.bean.AbsSortXml
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.constant.IntentKey
import com.xmbox.app.databinding.FragmentHomeBinding
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.activity.CollectActivity
import com.github.tvbox.osc.ui.activity.FastSearchActivity
import com.github.tvbox.osc.ui.activity.HistoryActivity
import com.github.tvbox.osc.ui.activity.MainActivity
import com.github.tvbox.osc.ui.activity.SubscriptionActivity
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectDialogInterface
import com.github.tvbox.osc.ui.dialog.LastViewedDialog
import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.ui.dialog.TipDialog
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : BaseVbFragment<FragmentHomeBinding>() {

    /**
     * 提供给主页返回操作
     */
    val tabIndex: Int
        get() = mBinding.tabLayout.selectedTabPosition

    /**
     * 提供给主页返回操作
     */
    val allFragments: List<BaseLazyFragment>
        get() = fragments

    private var sourceViewModel: SourceViewModel? = null
    private val fragments: MutableList<BaseLazyFragment> = ArrayList()

    /**
     * 顶部tabs分类集合,用于渲染tab页,每个tab对应fragment内的数据
     */
    private var mSortDataList: List<SortData> = ArrayList()
    var dataInitOk = false
    var jarInitOk = false

    var errorTipDialog: TipDialog? = null

    /**
     * true: 配置变更重载
     * false: 全部重载(api变更、重启app等)
     */
    var onlyConfigChanged = false
    private var lastClickTime = 0L

    override fun init() {
        ControlManager.get().startServer()
        mBinding.tvName.setOnClickListener {
            // 防止重复点击
            if (System.currentTimeMillis() - lastClickTime < 500) {
                return@setOnClickListener
            }
            lastClickTime = System.currentTimeMillis()

            // 添加调试日志
            android.util.Log.d("HomeFragment", "点击视频源按钮 - dataInitOk: $dataInitOk, jarInitOk: $jarInitOk")
            val sources = ApiConfig.get().sourceBeanList
            android.util.Log.d("HomeFragment", "当前数据源数量: ${sources?.size ?: 0}")

            // 立即给用户反馈
            mBinding.tvName.alpha = 0.7f
            mBinding.tvName.postDelayed({ mBinding.tvName.alpha = 1.0f }, 150)

            // 优化用户体验：即使数据未完全加载，也尝试显示可用的数据源
            if (sources != null && sources.size > 0) {
                android.util.Log.d("HomeFragment", "调用 showSiteSwitch() - 有可用数据源")
                // 使用异步方式显示对话框，避免阻塞UI
                ThreadPoolManager.executeMain {
                    showSiteSwitch()
                }
            } else if (dataInitOk && jarInitOk) {
                // 数据已加载完成但没有数据源
                android.util.Log.d("HomeFragment", "数据加载完成但无可用数据源")
                MD3ToastUtils.showToast("暂无可用数据源，请检查订阅配置")
            } else {
                // 数据仍在加载中
                android.util.Log.d("HomeFragment", "数据源正在加载中...")
                MD3ToastUtils.showToast("数据源正在加载中，请稍候...")

                // 自动重试机制：延迟后再次检查
                ThreadPoolManager.executeMainDelayed({
                    if (isAdded && ApiConfig.get().sourceBeanList?.isNotEmpty() == true) {
                        MD3ToastUtils.showToast("数据源加载完成，请重新点击")
                    }
                }, 2000)
            }
        }
        mBinding.tvName.setOnLongClickListener {
            android.util.Log.d("HomeFragment", "长按视频源按钮 - 刷新数据源")
            refreshHomeSources()
            true
        }
        mBinding.search.setOnClickListener {
            jumpActivity(FastSearchActivity::class.java)
        }
        mBinding.ivHistory.setOnClickListener {
            jumpActivity(HistoryActivity::class.java)
        }
        mBinding.ivCollect.setOnClickListener {
            jumpActivity(CollectActivity::class.java)
        }
        setLoadSir(mBinding.contentLayout)
        initViewModel()
        initData()
    }

    private fun initViewModel() {
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        sourceViewModel?.sortResult?.observe(this) { absXml: AbsSortXml? ->
            showSuccess()
            mSortDataList =
                if (absXml?.classes != null && absXml.classes.sortList != null) {
                    DefaultConfig.adjustSort(
                        ApiConfig.get().homeSourceBean.key,
                        absXml.classes.sortList,
                        true
                    )
                } else {
                    DefaultConfig.adjustSort(ApiConfig.get().homeSourceBean.key, ArrayList(), true)
                }
            initViewPager(absXml)
        }
    }

    fun initData() {
        // 安全地获取MainActivity实例
        val mainActivity = mActivity as? MainActivity
        if (mainActivity == null) {
            android.util.Log.w("HomeFragment", "initData() - mActivity is null, Fragment may not be attached")
            return
        }

        // 只有在onlyConfigChanged还没有被设置时才使用MainActivity的值
        // 这样可以保持refreshHomeSources()中设置的true值不被覆盖
        if (!onlyConfigChanged) {
            onlyConfigChanged = mainActivity.useCacheConfig
        }

        // 确保订阅源区域始终可见
        mBinding.nameContainer.visibility = View.VISIBLE

        val home = ApiConfig.get().homeSourceBean
        android.util.Log.d("HomeFragment", "initData() - homeSourceBean: ${home?.name}, dataInitOk: $dataInitOk, jarInitOk: $jarInitOk")

        if (home != null && !home.name.isNullOrEmpty()) {
            mBinding.tvName.text = home.name
            mBinding.tvName.postDelayed({ mBinding.tvName.isSelected = true }, 2000)
        } else {
            // 如果没有订阅源，显示默认文本
            mBinding.tvName.text = "请选择订阅源"
        }

        showLoading()
        when{
            dataInitOk && jarInitOk -> {
                //正常初始化会先加载,最终到这,此时数据有以下几种情况
                // 1. api/jar/spider等均加载完,正常显示数据。2. 缺失spider(存疑?)/api配置有问题同样加载(最后空布局 或 只有豆瓣首页)
                android.util.Log.d("HomeFragment", "调用 sourceViewModel.getSort")
                sourceViewModel?.getSort(ApiConfig.get().homeSourceBean.key)
            }
            dataInitOk && !jarInitOk -> {
                android.util.Log.d("HomeFragment", "调用 loadJar")
                loadJar()
            }
            else -> {
                android.util.Log.d("HomeFragment", "调用 loadConfig")
                loadConfig()
            }
        }
    }

    private fun loadConfig(){
        // 添加超时处理
        val timeoutRunnable = Runnable {
            LOG.e("loadConfig", "加载配置超时")
            ThreadPoolManager.executeMain {
                dataInitOk = true
                jarInitOk = true
                initData()
                MD3ToastUtils.showToast("加载超时，使用缓存数据")
            }
        }

        // 减少超时时间到10秒，提升用户体验
        ThreadPoolManager.executeMainDelayed(timeoutRunnable, 10000)

        // 优先尝试使用缓存数据
        // 注释掉缓存数据加载逻辑，直接进行正常加载
        /*
        if (ApiConfig.get().getStoreHouse().isNotEmpty()) {
            LOG.i("loadConfig", "使用缓存数据快速加载")
            ThreadPoolManager.executeMainDelayed({
                dataInitOk = true
                // 后台异步加载最新数据
                ThreadPoolManager.executeIO {
                    ApiConfig.get().loadLastConfig()
                }
                // 立即进入页面
                initData()
            }, 50)
            return
        }
        */

        // 安全地获取activity引用
        val currentActivity = activity
        if (currentActivity == null) {
            android.util.Log.w("HomeFragment", "loadConfig() - activity is null, Fragment may not be attached")
            return
        }

        ApiConfig.get().loadConfig(onlyConfigChanged, object : LoadConfigCallback {

            override fun retry() {
                ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                ThreadPoolManager.executeMain { initData() }
            }

            override fun success() {
                ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                dataInitOk = true
                if (ApiConfig.get().spider.isEmpty()) {
                    jarInitOk = true
                }
                ThreadPoolManager.executeMainDelayed({ initData() }, 50)
            }

            override fun error(msg: String) {
                ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                if (msg.equals("-1", ignoreCase = true)) {
                    ThreadPoolManager.executeMain {
                        dataInitOk = true
                        jarInitOk = true
                        initData()
                    }
                } else {
                    // 确保在主线程中安全地调用showTipDialog
                    ThreadPoolManager.executeMain {
                        if (isAdded && activity != null) {
                            showTipDialog(msg)
                        } else {
                            // Fragment未附加到Activity，直接设置标志位
                            LOG.e("HomeFragment", "加载配置错误，但Fragment未附加到Activity")
                            dataInitOk = true
                            jarInitOk = true
                        }
                    }
                }
            }
        }, currentActivity)
    }

    private fun loadJar(){
        if (!ApiConfig.get().spider.isNullOrEmpty()) {
            // 添加超时处理
            val timeoutRunnable = Runnable {
                LOG.e("loadJar", "加载JAR超时")
                ThreadPoolManager.executeMain {
                    jarInitOk = true
                    initData()
                    MD3ToastUtils.showToast("JAR加载超时，使用缓存数据")
                }
            }

            // 减少超时时间到5秒，提升用户体验
            ThreadPoolManager.executeMainDelayed(timeoutRunnable, 5000)

            // 尝试预加载内置JAR以加快启动速度
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 预加载内置的JAR解析器 - 简化处理，直接跳过预加载
                    withContext(Dispatchers.Main) {
                        // 直接进入常规加载流程
                        /*
                        val defaultParser = ApiConfig.get().getDefaultParser()
                        if (defaultParser != null) {
                            // 取消超时处理
                            ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                            jarInitOk = true
                            initData()
                            // 后台继续加载完整JAR
                            ThreadPoolManager.executeIO {
                                ApiConfig.get().loadJarComplete()
                            }
                            return@withContext
                        }
                        */

                        // 默认解析器加载失败，使用常规加载
                        ApiConfig.get().loadJar(
                            onlyConfigChanged,
                            ApiConfig.get().spider,
                            object : LoadConfigCallback {
                                override fun success() {
                                    ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                                    jarInitOk = true
                                    ThreadPoolManager.executeMainDelayed({
                                        if (!onlyConfigChanged) {
                                            queryHistory()
                                        }
                                        initData()
                                    }, 50)
                                }

                                override fun retry() {
                                    ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                                }

                                override fun error(msg: String) {
                                    ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                                    jarInitOk = true
                                    ThreadPoolManager.executeMain {
                                        // 确保Fragment仍然附加到Activity
                                        if (isAdded && activity != null) {
                                            MD3ToastUtils.showToast("更新订阅失败")
                                        }
                                        initData()
                                    }
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    LOG.e("loadJar", "预加载失败: ${e.message}")
                    withContext(Dispatchers.Main) {
                        // 默认预加载失败，使用常规加载
                        ApiConfig.get().loadJar(
                            onlyConfigChanged,
                            ApiConfig.get().spider,
                            object : LoadConfigCallback {
                                override fun success() {
                                    ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                                    jarInitOk = true
                                    ThreadPoolManager.executeMainDelayed({
                                        if (!onlyConfigChanged) {
                                            queryHistory()
                                        }
                                        initData()
                                    }, 50)
                                }

                                override fun retry() {
                                    ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                                }

                                override fun error(msg: String) {
                                    ThreadPoolManager.getMainHandler().removeCallbacks(timeoutRunnable)
                                    jarInitOk = true
                                    ThreadPoolManager.executeMain {
                                        // 确保Fragment仍然附加到Activity
                                        if (isAdded && activity != null) {
                                            MD3ToastUtils.showToast("更新订阅失败")
                                        }
                                        initData()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showTipDialog(msg: String) {
        // 安全检查：确保Fragment已附加到Activity
        if (!isAdded || activity == null) {
            // Fragment未附加到Activity，记录错误并返回
            LOG.e("HomeFragment", "Fragment未附加到Activity，无法显示对话框")
            // 设置标志位，避免进一步的错误
            dataInitOk = true
            jarInitOk = true
            return
        }

        try {
            if (errorTipDialog == null) {
                errorTipDialog =
                    TipDialog(requireActivity(), msg, "重试", "取消", object : TipDialog.OnListener {
                        override fun left() {
                            ThreadPoolManager.executeMain {
                                initData()
                                errorTipDialog?.hide()
                            }
                        }

                        override fun right() {
                            dataInitOk = true
                            jarInitOk = true
                            ThreadPoolManager.executeMain {
                                initData()
                                errorTipDialog?.hide()
                            }
                        }

                        override fun cancel() {
                            dataInitOk = true
                            jarInitOk = true
                            ThreadPoolManager.executeMain {
                                initData()
                                errorTipDialog?.hide()
                            }
                        }

                        override fun onTitleClick() {
                            errorTipDialog?.hide()
                            if (isAdded && activity != null) {
                                jumpActivity(SubscriptionActivity::class.java)
                            }
                        }
                    })
            }
            if (errorTipDialog != null && !errorTipDialog!!.isShowing && isAdded && activity != null) {
                errorTipDialog!!.show()
            }
        } catch (e: Exception) {
            // 捕获任何可能的异常
            LOG.e("HomeFragment", "显示对话框时出错: ${e.message}")
            // 设置标志位，避免进一步的错误
            dataInitOk = true
            jarInitOk = true
        }
    }

    private fun getTabTextView(text: String): TextView {
        val textView = TextView(mContext)
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setPadding(
            ConvertUtils.dp2px(20f),
            ConvertUtils.dp2px(10f),
            ConvertUtils.dp2px(5f),
            ConvertUtils.dp2px(10f)
        )
        return textView
    }

    private fun initViewPager(absXml: AbsSortXml?) {
        // 在后台线程准备数据
        ThreadPoolManager.executeCompute {
            if (mSortDataList.isEmpty()) {
                ThreadPoolManager.executeMain { showEmpty() }
                return@executeCompute
            }

            val preparedFragments = ArrayList<BaseLazyFragment>()

            for (data in mSortDataList) {
                if (data.id == "my0") { //tab是主页,添加主页fragment 根据设置项显示豆瓣热门/站点推荐(每个源不一样)/历史记录
                    if (Hawk.get(
                            HawkConfig.HOME_REC,
                            0
                        ) == 1 && absXml != null && absXml.videoList != null && absXml.videoList.size > 0
                    ) { //站点推荐
                        preparedFragments.add(UserFragment.newInstance(absXml.videoList))
                    } else { //豆瓣热门/历史记录
                        preparedFragments.add(UserFragment.newInstance(null))
                    }
                } else { //来自源的分类
                    preparedFragments.add(GridFragment.newInstance(data))
                }
            }

            // 如果设置了关闭主页内容，移除主页Fragment和对应的SortData
            if (Hawk.get(HawkConfig.HOME_REC, 0) == 2 && preparedFragments.size > 0) { //关闭主页
                preparedFragments.removeAt(0)
                // 创建一个可变列表以便修改
                val mutableSortDataList = mSortDataList.toMutableList()
                // 移除第一个元素（主页标签）
                if (mutableSortDataList.isNotEmpty()) {
                    mutableSortDataList.removeAt(0)
                }
                // 更新mSortDataList
                mSortDataList = mutableSortDataList
            }

            // 在主线程更新UI
            ThreadPoolManager.executeMain {
                // 确保订阅源区域始终可见
                mBinding.nameContainer.visibility = View.VISIBLE

                mBinding.tabLayout.removeAllTabs()
                fragments.clear()
                fragments.addAll(preparedFragments)

                //使用ViewPager2的适配器
                mBinding.mViewPager.adapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
                    override fun getItemCount(): Int {
                        return fragments.size
                    }

                    override fun createFragment(position: Int): Fragment {
                        return fragments[position]
                    }
                }

                //设置ViewPager2和TabLayout的联动
                TabLayoutMediator(mBinding.tabLayout, mBinding.mViewPager) { tab, position ->
                    tab.text = mSortDataList[position].name
                }.attach()

                // 减少ViewPager2的预加载数量，默认是2
                mBinding.mViewPager.offscreenPageLimit = 1
            }
        }
    }

    /**
     * 提供给主页返回操作
     */
    fun scrollToFirstTab(): Boolean {
        return if (mBinding.tabLayout.selectedTabPosition != 0) {
            mBinding.mViewPager.setCurrentItem(0, false)
            true
        } else {
            false
        }
    }

    override fun onPause() {
        super.onPause()
        // 不再需要移除Handler的回调
    }

    private fun showSiteSwitch() {
        val sites = ApiConfig.get().sourceBeanList
        android.util.Log.d("HomeFragment", "showSiteSwitch() - 数据源列表: ${sites?.size ?: 0}")

        if (sites != null && sites.size > 0) {
            android.util.Log.d("HomeFragment", "创建选择对话框")

            // 预先计算当前选中的索引，避免在UI线程中进行耗时操作
            val currentHomeSource = ApiConfig.get().homeSourceBean
            var selectedIndex = 0
            if (currentHomeSource != null) {
                for (i in sites.indices) {
                    if (sites[i].key == currentHomeSource.key) {
                        selectedIndex = i
                        break
                    }
                }
            }

            try {
                val dialog = SelectDialog<SourceBean>(requireActivity())
                val tvRecyclerView = dialog.findViewById<TvRecyclerView>(R.id.list)
                tvRecyclerView.setLayoutManager(V7GridLayoutManager(dialog.context, 2))

                // 延迟执行RecyclerView优化，避免阻塞UI
                tvRecyclerView.post {
                    RecyclerViewOptimizer.optimizeTvRecyclerView(tvRecyclerView)
                }

                dialog.setTip("请选择首页数据源")

                // 使用简化的DiffUtil回调
                val diffCallback = object : DiffUtil.ItemCallback<SourceBean>() {
                    override fun areItemsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                        return oldItem.key == newItem.key
                    }

                    override fun areContentsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                        return oldItem.name == newItem.name
                    }
                }

                dialog.setAdapter(object : SelectDialogInterface<SourceBean?> {
                    override fun click(value: SourceBean?, pos: Int) {
                        android.util.Log.d("HomeFragment", "选择数据源: ${value?.name}")
                        ApiConfig.get().setSourceBean(value)
                        refreshHomeSources()
                    }

                    override fun getDisplay(source: SourceBean?): String {
                        return source?.name ?: ""
                    }
                }, diffCallback, sites, selectedIndex)

                android.util.Log.d("HomeFragment", "显示对话框")
                dialog.show()
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "创建对话框失败", e)
                MD3ToastUtils.showLongToast("创建对话框失败: ${e.message}")
            }
        } else {
            android.util.Log.d("HomeFragment", "没有可用数据源")
            MD3ToastUtils.showLongToast("暂无可用数据源")
        }
    }

    private fun refreshHomeSources() {
        // 使用EventBus发送源变更事件，而不是重启整个应用
        onlyConfigChanged = true
        dataInitOk = false
        jarInitOk = false

        // 确保订阅源区域可见并更新名称
        mBinding.nameContainer.visibility = View.VISIBLE
        val home = ApiConfig.get().homeSourceBean
        if (home != null && !home.name.isNullOrEmpty()) {
            mBinding.tvName.text = home.name
            mBinding.tvName.postDelayed({ mBinding.tvName.isSelected = true }, 2000)
        } else {
            mBinding.tvName.text = "请选择订阅源"
        }

        MD3ToastUtils.showToast("正在切换数据源...")
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()

        // 清理ViewPager2相关引用，防止内存泄漏
        try {
            // 清理适配器
            mBinding.mViewPager.adapter = null

            // 清理TabLayout和ViewPager2的联动
            // TabLayoutMediator会自动处理清理，但我们可以手动清理

            // 清理Fragment列表
            fragments.clear()

            // 清理数据列表
            mSortDataList = ArrayList()

            android.util.Log.d("HomeFragment", "ViewPager2相关资源清理完成")
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "清理ViewPager2资源时发生错误: ${e.message}")
        }

        ControlManager.get().stopServer()
    }

    private fun queryHistory() {
        // 使用ThreadPoolManager替代lifecycleScope
        ThreadPoolManager.executeIO {
            val allVodRecord = RoomDataManger.getAllVodRecord(100)
            val vodInfoList: MutableList<VodInfo?> = ArrayList()
            for (vodInfo in allVodRecord) {
                if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty()) vodInfo.note =
                    vodInfo.playNote
                vodInfoList.add(vodInfo)
            }

            // 查询完成后更新UI
            ThreadPoolManager.executeMain {
                if (vodInfoList.isNotEmpty() && vodInfoList[0] != null && isAdded) {
                    XPopup.Builder(context)
                        .hasShadowBg(true) // 添加阴影背景，增强视觉效果
                        .isDestroyOnDismiss(true)
                        .isCenterHorizontal(true)
                        .isTouchThrough(false)
                        // 使用系统主题
                        .offsetY(ScreenUtils.getAppScreenHeight() - 400)
                        .asCustom(LastViewedDialog(requireContext(), vodInfoList[0]))
                        .show()
                        .delayDismiss(5000)
                }
            }
        }
    }
}