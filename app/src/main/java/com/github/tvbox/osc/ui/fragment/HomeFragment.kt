package com.github.tvbox.osc.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
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
import com.github.tvbox.osc.R
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
import com.github.tvbox.osc.databinding.FragmentHomeBinding
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
    private var dataInitOk = false
    private var jarInitOk = false

    var errorTipDialog: TipDialog? = null

    /**
     * true: 配置变更重载
     * false: 全部重载(api变更、重启app等)
     */
    var onlyConfigChanged = false

    override fun init() {
        ControlManager.get().startServer()
        mBinding.nameContainer.setOnClickListener {
            if (dataInitOk && jarInitOk) {
                showSiteSwitch()
            } else {
                MD3ToastUtils.showToast("数据源未加载，长按刷新或切换订阅")
            }
        }
        mBinding.nameContainer.setOnLongClickListener {
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

    private fun initData() {
        val mainActivity = mActivity as MainActivity
        onlyConfigChanged = mainActivity.useCacheConfig

        val home = ApiConfig.get().homeSourceBean
        if (home != null && !home.name.isNullOrEmpty()) {
            mBinding.tvName.text = home.name
            mBinding.tvName.postDelayed({ mBinding.tvName.isSelected = true }, 2000)
        }

        showLoading()
        when{
            dataInitOk && jarInitOk -> {
                //正常初始化会先加载,最终到这,此时数据有以下几种情况
                // 1. api/jar/spider等均加载完,正常显示数据。2. 缺失spider(存疑?)/api配置有问题同样加载(最后空布局 或 只有豆瓣首页)
                sourceViewModel?.getSort(ApiConfig.get().homeSourceBean.key)
            }
            dataInitOk && !jarInitOk -> {
                loadJar()
            }
            else -> {
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

        // 设置15秒超时
        ThreadPoolManager.executeMainDelayed(timeoutRunnable, 15000)

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
                    showTipDialog(msg)
                }
            }
        }, activity)
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

            // 设置10秒超时
            ThreadPoolManager.executeMainDelayed(timeoutRunnable, 10000)

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
                            MD3ToastUtils.showToast("更新订阅失败")
                            initData()
                        }
                    }
                })
        }
    }

    private fun showTipDialog(msg: String) {
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
                        jumpActivity(SubscriptionActivity::class.java)
                    }
                })
        }
        if (!errorTipDialog!!.isShowing) errorTipDialog!!.show()
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
        if (sites.size > 0) {
            val dialog = SelectDialog<SourceBean>(requireActivity())
            val tvRecyclerView = dialog.findViewById<TvRecyclerView>(R.id.list)
            tvRecyclerView.setLayoutManager(V7GridLayoutManager(dialog.context, 2))
            // 优化RecyclerView性能
            RecyclerViewOptimizer.optimizeTvRecyclerView(tvRecyclerView)
            dialog.setTip("请选择首页数据源")
            dialog.setAdapter(object : SelectDialogInterface<SourceBean?> {
                override fun click(value: SourceBean?, pos: Int) {
                    ApiConfig.get().setSourceBean(value)
                    refreshHomeSources()
                }

                override fun getDisplay(source: SourceBean?): String {
                    return if (source == null) "" else source.name
                }
            }, object : DiffUtil.ItemCallback<SourceBean>() {
                override fun areItemsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                    return oldItem === newItem
                }

                override fun areContentsTheSame(oldItem: SourceBean, newItem: SourceBean): Boolean {
                    return oldItem.key.contentEquals(newItem.key)
                }
            }, sites, sites.indexOf(ApiConfig.get().homeSourceBean))
            dialog.show()
        } else {
            MD3ToastUtils.showLongToast("暂无可用数据源")
        }
    }

    private fun refreshHomeSources() {
        val intent = Intent(App.getInstance(), MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val bundle = Bundle()
        bundle.putBoolean(IntentKey.CACHE_CONFIG_CHANGED, true)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
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
                        .isDarkTheme(true) // 强制使用深色主题
                        .offsetY(ScreenUtils.getAppScreenHeight() - 400)
                        .asCustom(LastViewedDialog(requireContext(), vodInfoList[0]))
                        .show()
                        .delayDismiss(5000)
                }
            }
        }
    }
}