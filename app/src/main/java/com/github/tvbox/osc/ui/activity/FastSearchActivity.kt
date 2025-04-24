package com.github.tvbox.osc.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.tvbox.osc.ui.adapter.SearchWordAdapter
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.catvod.crawler.JsLoader
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.DoubanSuggestBean
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.databinding.ActivityFastSearchBinding
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.event.ServerEvent
import com.github.tvbox.osc.ui.adapter.FastSearchAdapter
import com.github.tvbox.osc.ui.dialog.DoubanSuggestDialog
import com.github.tvbox.osc.ui.dialog.SearchSourceDialog
import com.github.tvbox.osc.ui.dialog.SearchSuggestionsDialog
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.SearchHelper
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.callback.StringCallback
import com.orhanobut.hawk.Hawk
import com.zhy.view.flowlayout.FlowLayout
import com.zhy.view.flowlayout.TagAdapter
import okhttp3.Response
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.github.tvbox.osc.util.ThreadPoolManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class FastSearchActivity : BaseVbActivity<ActivityFastSearchBinding>(), TextWatcher {

    companion object {
        private var mCheckSources: HashMap<String, String>? = null
        fun setCheckedSourcesForSearch(checkedSources: HashMap<String, String>?) {
            mCheckSources = checkedSources
        }
    }

    private lateinit var sourceViewModel : SourceViewModel
    private var searchAdapter = FastSearchAdapter()
    private var searchAdapterFilter = FastSearchAdapter()
    private var searchWordAdapter = SearchWordAdapter()
    private var searchTitle: String? = ""
    private var spNames = HashMap<String, String>()
    private var isFilterMode = false
    private var searchFilterKey: String? = "" // 过滤的key
    private var resultVods = HashMap<String, MutableList<Movie.Video>>()
    private var pauseRunnable: MutableList<Runnable>? = null
    private var mSearchSuggestionsDialog: SearchSuggestionsDialog? = null
    private var lastClickTime: Long = 0 // 用于防止快速重复点击
    override fun init() {
        sourceViewModel = ViewModelProvider(this).get(SourceViewModel::class.java)
        // 注册LoadSir
        setLoadSir(mBinding.llLayout)
        initView()
        initData()
        //历史搜索
        initHistorySearch()
        // 热门搜索
        hotWords
    }

    override fun onResume() {
        super.onResume()
        if (pauseRunnable != null && pauseRunnable!!.size > 0) {
            searchExecutorService = ThreadPoolManager.getIOThreadPool()
            allRunCount.set(pauseRunnable!!.size)
            for (runnable: Runnable? in pauseRunnable!!) {
                searchExecutorService!!.execute(runnable)
            }
            pauseRunnable!!.clear()
            pauseRunnable = null
        }
    }

    private fun initView() {
        mBinding.etSearch.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search(mBinding.etSearch.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }
        mBinding.etSearch.addTextChangedListener(this)
        mBinding.ivFilter.setOnClickListener { filterSearchSource() }
        mBinding.ivBack.setOnClickListener { finish() }
        mBinding.ivSearch.setOnClickListener {
            search(mBinding.etSearch.text.toString())
        }
        // 初始化垂直标签列表 - 优化布局加载
        optimizeSearchWordList()

        // 添加更多调试日志
        LogUtils.d("FastSearchActivity: 初始化搜索导航菜单点击监听器")

        // 简化点击监听器实现，完全避免使用FastClickCheckUtil
        searchWordAdapter.setOnItemClickListener { adapter, view, position ->
            // 记录上次点击时间，防止快速重复点击
            val currentTime = System.currentTimeMillis()
            LogUtils.d("FastSearchActivity: 点击监听器触发 - position=$position, 时间间隔=${currentTime - lastClickTime}ms")

            if (currentTime - lastClickTime > 300) { // 300ms防抖
                lastClickTime = currentTime

                try {
                    LogUtils.d("FastSearchActivity: 处理点击 - 位置=$position, 数据大小=${searchWordAdapter.data.size}")
                    if (position >= 0 && position < searchWordAdapter.data.size) {
                        val word = searchWordAdapter.data[position]
                        LogUtils.d("FastSearchActivity: 点击了位置=$position, 文字=$word")
                        // 注意：不需要在这里调用setSelectedPosition，因为在适配器的点击监听器中已经调用了
                        // searchWordAdapter.setSelectedPosition(position)
                        filterResult(word)
                    } else {
                        LogUtils.e("FastSearchActivity: 位置越界 - position=$position, 数据大小=${searchWordAdapter.data.size}")
                    }
                } catch (e: Exception) {
                    LogUtils.e("FastSearchActivity: 处理点击异常 - " + e.message)
                    e.printStackTrace()
                }
            }
        }
        mBinding.mGridView.setHasFixedSize(true)
        mBinding.mGridView.setLayoutManager(LinearLayoutManager(this))
        mBinding.mGridView.adapter = searchAdapter
        searchAdapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val video = searchAdapter.data[position]
            try {
                if (searchExecutorService != null) {
                    pauseRunnable = searchExecutorService!!.shutdownNow()
                    searchExecutorService = null
                    JsLoader.stopAll()
                }
            } catch (th: Throwable) {
                th.printStackTrace()
            }
            // 直接跳转到详情页面
            val bundle = Bundle()
            bundle.putString("id", video.id)
            bundle.putString("sourceKey", video.sourceKey)
            jumpActivity(DetailActivity::class.java, bundle)
        }
        mBinding.mGridViewFilter.setLayoutManager(LinearLayoutManager(this))

        mBinding.mGridViewFilter.adapter = searchAdapterFilter
        searchAdapterFilter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            val video = searchAdapterFilter.data[position]
            if (video != null) {
                try {
                    if (searchExecutorService != null) {
                        pauseRunnable = searchExecutorService!!.shutdownNow()
                        searchExecutorService = null
                        JsLoader.stopAll()
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
                // 直接跳转到详情页面
                val bundle = Bundle()
                bundle.putString("id", video.id)
                bundle.putString("sourceKey", video.sourceKey)
                jumpActivity(DetailActivity::class.java, bundle)
            }
        }

        searchAdapter.setOnItemLongClickListener { _, _, position ->
            val video = searchAdapter.data[position]
            getDoubanSuggest(video.name)
            true
        }
        searchAdapterFilter.setOnItemLongClickListener { _, _, position ->
            val video = searchAdapterFilter.data[position]
            getDoubanSuggest(video.name)
            true
        }

        setLoadSir(mBinding.llLayout)
    }

    /**
     * 指定搜索源(过滤)
     */
    private fun filterSearchSource() {
        val allSourceBean = ApiConfig.get().sourceBeanList
        if (allSourceBean.isNotEmpty()) {
            val searchAbleSource: MutableList<SourceBean> = ArrayList()
            for (sourceBean: SourceBean in allSourceBean) {
                if (sourceBean.isSearchable) {
                    searchAbleSource.add(sourceBean)
                }
            }
            val mSearchSourceDialog = SearchSourceDialog(this@FastSearchActivity, searchAbleSource, mCheckSources)
            mSearchSourceDialog.show()
        }

    }

    private fun filterResult(spName: String) {
        LogUtils.d("filterResult: 开始过滤结果 - " + spName)
        LogUtils.d("filterResult: 当前线程 = " + Thread.currentThread().name)
        LogUtils.d("filterResult: UI状态 - llSearchResult可见性 = " + mBinding.llSearchResult.visibility + ", mGridView可见性 = " + mBinding.mGridView.visibility + ", mGridViewFilter可见性 = " + mBinding.mGridViewFilter.visibility)

        try {
            // 首先确保搜索结果容器可见
            mBinding.llSearchResult.visibility = View.VISIBLE

            if (spName === "全部显示") {
                LogUtils.d("filterResult: 显示全部结果")
                isFilterMode = false

                // 先设置可见性，再设置其他属性
                LogUtils.d("filterResult: 设置全部显示视图可见性")
                mBinding.mGridView.visibility = View.VISIBLE
                mBinding.mGridViewFilter.visibility = View.GONE

                // 确保加载状态正确
                LogUtils.d("filterResult: 调用showSuccess()")
                showSuccess()

                LogUtils.d("filterResult: 全部显示处理完成")
                return
            }

            LogUtils.d("filterResult: 显示特定源的结果 - " + spName)
            isFilterMode = true

            val key = spNames[spName]
            LogUtils.d("filterResult: 源的key - " + key)
            LogUtils.d("filterResult: spNames大小 = " + spNames.size + ", 内容 = " + spNames.toString())

            if (key.isNullOrEmpty()) {
                LogUtils.d("filterResult: key为空，返回")
                // 如果key为空，保持当前视图状态
                return
            }

            if (searchFilterKey === key) {
                LogUtils.d("filterResult: 已经是当前选中的key，返回")
                // 如果已经是当前选中的key，保持当前视图状态
                return
            }

            searchFilterKey = key
            LogUtils.d("filterResult: 检查resultVods - 大小 = " + resultVods.size + ", 包含当前key = " + resultVods.containsKey(key))

            val list: List<Movie.Video> = (resultVods[key]) ?: run {
                LogUtils.d("filterResult: 没有找到对应的视频列表，返回")
                // 如果没有找到对应的视频列表，保持当前视图状态
                return
            }

            LogUtils.d("filterResult: 设置新数据到过滤器适配器，数量: " + list.size)

            // 先设置数据，再设置可见性
            LogUtils.d("filterResult: 设置数据到过滤器适配器")
            searchAdapterFilter.setNewData(list)

            // 设置过滤视图可见性
            LogUtils.d("filterResult: 设置过滤视图可见性")
            mBinding.mGridView.visibility = View.GONE
            mBinding.mGridViewFilter.visibility = View.VISIBLE

            // 确保加载状态正确
            LogUtils.d("filterResult: 调用showSuccess()")
            showSuccess()

            LogUtils.d("filterResult: 过滤处理完成")
        } catch (e: Exception) {
            // 捕获并记录任何异常，避免崩溃
            LogUtils.e("filterResult 异常: " + e.message)
            e.printStackTrace()

            // 发生异常时，确保至少有一个视图可见
            if (mBinding.mGridView.visibility != View.VISIBLE && mBinding.mGridViewFilter.visibility != View.VISIBLE) {
                mBinding.mGridView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 优化左侧目录的布局和加载
     */
    private fun optimizeSearchWordList() {
        // 使用更高效的布局管理器
        val layoutManager = LinearLayoutManager(this)
        layoutManager.initialPrefetchItemCount = 20 // 预取更多项目

        // 设置固定大小提高性能
        mBinding.rvSearchWord.setHasFixedSize(true)

        // 减少重绘
        mBinding.rvSearchWord.itemAnimator = null

        // 设置布局管理器
        mBinding.rvSearchWord.layoutManager = layoutManager

        // 设置适配器
        mBinding.rvSearchWord.adapter = searchWordAdapter
    }

    private fun initData() {
        mCheckSources = SearchHelper.getSourcesForSearch()
        if (intent != null && intent.hasExtra("title")) {
            val title = intent.getStringExtra("title")
            if (!TextUtils.isEmpty(title)) {
                showLoading()
                search(title)
            }
        }
    }

    private fun hideHotAndHistorySearch(isHide: Boolean) {
        if (isHide) {
            mBinding.llSearchSuggest.visibility = View.GONE
            mBinding.llSearchResult.visibility = View.VISIBLE
        } else {
            mBinding.llSearchSuggest.visibility = View.VISIBLE
            mBinding.llSearchResult.visibility = View.GONE
        }
    }

    private fun initHistorySearch() {
        val mSearchHistory: List<String> = Hawk.get(HawkConfig.HISTORY_SEARCH, ArrayList())
        mBinding.llHistory.visibility = if (mSearchHistory.isNotEmpty()) View.VISIBLE else View.GONE
        mBinding.flHistory.adapter = object : TagAdapter<String?>(mSearchHistory) {
            override fun getView(parent: FlowLayout, position: Int, s: String?): View {
                val tv: TextView = LayoutInflater.from(this@FastSearchActivity).inflate(
                    R.layout.item_search_word_hot,
                    mBinding.flHistory, false
                ) as TextView
                tv.text = s
                return tv
            }
        }
        mBinding.flHistory.setOnTagClickListener { _: View?, position: Int, _: FlowLayout? ->
            val keyword = mSearchHistory[position]
            LogUtils.d("History search clicked: " + keyword)
            // 直接执行搜索，不等待文本变化事件
            search(keyword)
            // 确保搜索结果显示
            ThreadPoolManager.executeMainDelayed({
                // 确保搜索结果容器可见
                mBinding.llSearchResult.visibility = View.VISIBLE
                if (mBinding.mGridView.visibility != View.VISIBLE && !isFilterMode) {
                    showSuccess()
                    mBinding.mGridView.visibility = View.VISIBLE
                }
            }, 100) // 缩短延迟时间
            true
        }
        findViewById<View>(R.id.iv_clear_history).setOnClickListener { view: View ->
            Hawk.put(HawkConfig.HISTORY_SEARCH, ArrayList<Any>())
            //FlowLayout及其adapter貌似没有清空数据的api,简单粗暴重置
            view.postDelayed({ initHistorySearch() }, 300)
        }
    }

    /**
     * 热门搜索
     */
    private val hotWords: Unit
        get() {
            // 加载热词
            OkGo.get<String>("https://node.video.qq.com/x/api/hot_search")
                .params("channdlId", "0")
                .params("_", System.currentTimeMillis())
                .execute(object : AbsCallback<String?>() {
                    override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
                        try {
                            val hots = ArrayList<String>()
                            val itemList =
                                JsonParser.parseString(response.body()).asJsonObject["data"].asJsonObject["mapResult"].asJsonObject["0"].asJsonObject["listInfo"].asJsonArray
                            //                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonArray();
                            for (ele: JsonElement in itemList) {
                                val obj = ele as JsonObject
                                hots.add(obj["title"].asString.trim { it <= ' ' }
                                    .replace("<|>|《|》|-".toRegex(), "").split(" ".toRegex())
                                    .dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[0])
                            }
                            mBinding.flHot.adapter = object : TagAdapter<String?>(hots as List<String?>?) {
                                override fun getView(
                                    parent: FlowLayout,
                                    position: Int,
                                    s: String?
                                ): View {
                                    val tv: TextView =
                                        LayoutInflater.from(this@FastSearchActivity).inflate(
                                            R.layout.item_search_word_hot,
                                            mBinding.flHot, false
                                        ) as TextView
                                    tv.text = s
                                    return tv
                                }
                            }
                            mBinding.flHot.setOnTagClickListener { _: View?, position: Int, _: FlowLayout? ->
                                val keyword = hots.get(position)
                                LogUtils.d("Hot search clicked: " + keyword)
                                // 先确保搜索结果容器可见
                                mBinding.llSearchResult.visibility = View.VISIBLE
                                mBinding.mGridView.visibility = View.VISIBLE
                                // 直接执行搜索，不等待文本变化事件
                                search(keyword)
                                true
                            }
                        } catch (th: Throwable) {
                            th.printStackTrace()
                        }
                    }

                    @Throws(Throwable::class)
                    override fun convertResponse(response: Response): String {
                        return response.body()!!.string()
                    }
                })
        }

    /**
     * 联想搜索
     */
    private fun getSuggest(text: String) {
        // 加载热词
        OkGo.get<String>("https://suggest.video.iqiyi.com/?if=mobile&key=$text")
            .execute(object : AbsCallback<String?>() {
                override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
                    val titles: MutableList<String> = ArrayList()
                    try {
                        val json = JsonParser.parseString(response.body()).asJsonObject
                        val datas = json["data"].asJsonArray
                        for (data: JsonElement in datas) {
                            val item = data as JsonObject
                            titles.add(item["name"].asString.trim { it <= ' ' })
                        }
                    } catch (th: Throwable) {
                        LogUtils.d(th.toString())
                    }
                    if (titles.isNotEmpty()) {
                        showSuggestDialog(titles)
                    }
                }

                @Throws(Throwable::class)
                override fun convertResponse(response: Response): String {
                    return response.body()!!.string()
                }
            })
    }

    private fun showSuggestDialog(list: List<String>) {
        if (mSearchSuggestionsDialog == null) {
            mSearchSuggestionsDialog =
                SearchSuggestionsDialog(this@FastSearchActivity, list
                ) { _, text ->
                    LogUtils.d("搜索:$text")
                    // 先确保搜索结果容器可见
                    mBinding.llSearchResult.visibility = View.VISIBLE
                    mBinding.mGridView.visibility = View.VISIBLE
                    mSearchSuggestionsDialog!!.dismissWith { search(text) }
                }
            XPopup.Builder(this@FastSearchActivity)
                .atView(mBinding.etSearch)
                .notDismissWhenTouchInView(mBinding.etSearch)
                .isViewMode(true) //开启View实现
                .isRequestFocus(false) //不强制焦点
                .setPopupCallback(object : SimpleCallback() {
                    override fun onDismiss(popupView: BasePopupView) { // 弹窗关闭了就置空对象,下次重新new
                        super.onDismiss(popupView)
                        mSearchSuggestionsDialog = null
                    }
                })
                .asCustom(mSearchSuggestionsDialog)
                .show()
        } else { // 不为空说明弹窗为打开状态(关闭就置空了).直接刷新数据
            mSearchSuggestionsDialog!!.updateSuggestions(list)
        }
    }

    private fun saveSearchHistory(searchWord: String?) {
        if (!searchWord.isNullOrEmpty()) {
            val history = Hawk.get(HawkConfig.HISTORY_SEARCH, ArrayList<String?>())
            if (!history.contains(searchWord)) {
                history.add(0, searchWord)
            } else {
                history.remove(searchWord)
                history.add(0, searchWord)
            }
            if (history.size > 30) {
                history.removeAt(30)
            }
            Hawk.put(HawkConfig.HISTORY_SEARCH, history)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun server(event: ServerEvent) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            val title = event.obj as String
            showLoading()
            search(title)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun refresh(event: RefreshEvent) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(if (event.obj == null) null else event.obj as AbsXml)
            } catch (e: Exception) {
                searchData(null)
            }
        }
    }

    private fun search(title: String?) {
        if (title.isNullOrEmpty()) {
            ToastUtils.showShort("请输入搜索内容")
            return
        }

        LogUtils.d("search: 开始搜索 - " + title)

        // Cancel any pending search suggestion requests
        searchDebounceRunnable?.let { searchHandler.removeSearchCallback(it) }
        searchDebounceRunnable = null

        //先移除监听,避免重新设置要搜索的文字触发搜索建议并弹窗
        mBinding.etSearch.removeTextChangedListener(this)
        mBinding.etSearch.setText(title)
        mBinding.etSearch.setSelection(title.length)
        mBinding.etSearch.addTextChangedListener(this)
        if (mSearchSuggestionsDialog != null && mSearchSuggestionsDialog!!.isShow) {
            mSearchSuggestionsDialog!!.dismiss()
        }
        if (!Hawk.get(HawkConfig.PRIVATE_BROWSING, false)) { //无痕浏览不存搜索历史
            saveSearchHistory(title)
        }
        hideHotAndHistorySearch(true)
        KeyboardUtils.hideSoftInput(this)
        cancel()
        showLoading()
        searchTitle = title

        // Reset UI state
        LogUtils.d("search: 重置界面状态")
        mBinding.llSearchResult.visibility = View.VISIBLE  // 显示搜索结果容器
        mBinding.mGridView.visibility = View.VISIBLE  // 显示搜索结果列表
        mBinding.mGridViewFilter.visibility = View.GONE  // 隐藏过滤后的搜索结果列表

        // Clear data on background thread to avoid UI jank
        ThreadPoolManager.executeCompute {
            val emptyList = ArrayList<Movie.Video>()
            resultVods.clear()
            searchFilterKey = ""
            isFilterMode = false
            spNames.clear()

            // Update adapters on main thread
            ThreadPoolManager.executeMain {
                searchAdapter.setNewData(emptyList)
                searchAdapterFilter.setNewData(emptyList)
                searchWordAdapter.setNewData(ArrayList())

                // Start search after UI is updated
                searchResult()
            }
        }
    }

    private var searchExecutorService: ExecutorService? = null
    private val allRunCount = AtomicInteger(0)
    private val searchHandler = SearchHandler(this)
    private var searchDebounceRunnable: Runnable? = null
    private val SEARCH_DEBOUNCE_DELAY = 300L // 300ms debounce delay

    // 使用静态内部类处理Handler消息，避免内存泄漏
    private class SearchHandler(activity: FastSearchActivity) : Handler(Looper.getMainLooper()) {
        private val weakReference = WeakReference(activity)

        fun postSearchSuggestion(runnable: Runnable, delayMillis: Long) {
            weakReference.get()?.let {
                postDelayed(runnable, delayMillis)
            }
        }

        fun removeSearchCallback(runnable: Runnable) {
            removeCallbacks(runnable)
        }
    }

    private fun addWordForText(text: String) {
        val words = searchWordAdapter.data
        words.add(text)
        searchWordAdapter.setNewData(words)
    }

    private fun searchResult() {
        LogUtils.d("searchResult: 开始执行搜索 - " + searchTitle)
        try {
            if (searchExecutorService != null) {
                searchExecutorService!!.shutdownNow()
                searchExecutorService = null
                JsLoader.stopAll()
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        } finally {
            searchAdapter.setNewData(ArrayList())
            searchAdapterFilter.setNewData(ArrayList())
            allRunCount.set(0)
        }

        // Use optimized thread pool instead of creating a new one each time
        searchExecutorService = ThreadPoolManager.getIOThreadPool()

        // Process search sources in background thread to avoid UI blocking
        ThreadPoolManager.executeCompute {
            val searchRequestList: MutableList<SourceBean> = ArrayList()
            searchRequestList.addAll(ApiConfig.get().sourceBeanList)
            val home = ApiConfig.get().homeSourceBean
            searchRequestList.remove(home)
            searchRequestList.add(0, home)
            val siteKey = ArrayList<String>()
            val wordList = ArrayList<String>()
            wordList.add("全部显示")

            // Update UI on main thread
            ThreadPoolManager.executeMain {
                searchWordAdapter.setNewData(wordList)
            }

            // Filter searchable sources
            for (bean: SourceBean in searchRequestList) {
                if (!bean.isSearchable) {
                    continue
                }
                if (mCheckSources != null && !mCheckSources!!.containsKey(bean.key)) {
                    continue
                }
                siteKey.add(bean.key)
                spNames[bean.name] = bean.key
                allRunCount.incrementAndGet()
            }

            // Execute search requests in batches to avoid overwhelming the system
            val batchSize = 2 // 减少到每批只处理2个源，减轻系统负担
            var currentIndex = 0

            while (currentIndex < siteKey.size) {
                val endIndex = minOf(currentIndex + batchSize, siteKey.size)
                val batch = siteKey.subList(currentIndex, endIndex)

                // Execute batch
                for (key in batch) {
                    searchExecutorService?.execute {
                        try {
                            LogUtils.d("Searching source: " + key)
                            sourceViewModel.getSearch(key, searchTitle)
                        } catch (e: Exception) {
                            // Decrement counter on error to avoid hanging
                            LogUtils.e("Search error for source " + key + ": " + e.message)
                            val count = allRunCount.decrementAndGet()
                            if (count <= 0) {
                                ThreadPoolManager.executeMain {
                                    if (searchAdapter.data.size <= 0) {
                                        showEmpty()
                                    }
                                    cancel()
                                }
                            }
                        }
                    }
                }

                currentIndex = endIndex

                // Increase delay between batches to improve UI responsiveness
                if (currentIndex < siteKey.size) {
                    try {
                        Thread.sleep(300) // 增加到300ms的延迟
                    } catch (_: InterruptedException) {}
                }
            }
        }
    }

    /**
     * 添加到最后面并返回最后一个key
     * 优化版本：减少UI更新频率
     * @param key 源的key
     * @return 返回源的key
     */
    private fun addWordAdapterIfNeed(key: String): String {
        try {
            // 获取源名称
            var name = ""
            for (n: String in spNames.keys) {
                if ((spNames[n] == key)) {
                    name = n
                }
            }
            if ((name == "")) return key

            // 检查是否已存在
            for (word in searchWordAdapter.data) {
                if (word == name) {
                    return key
                }
            }

            // 使用临时列表收集所有要添加的词，然后一次性更新
            val currentWords = ArrayList(searchWordAdapter.data)
            currentWords.add(name)

            // 一次性更新适配器数据
            ThreadPoolManager.executeMain {
                searchWordAdapter.setNewData(currentWords)
            }

            return key
        } catch (e: Exception) {
            return key
        }
    }

    private fun matchSearchResult(name: String, searchTitle: String?): Boolean {
        var searchTitle = searchTitle
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false
        searchTitle = searchTitle!!.trim { it <= ' ' }
        val arr = searchTitle.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var matchNum = 0
        for (one: String in arr) {
            if (name.contains(one)) matchNum++
        }
        return if (matchNum == arr.size) true else false
    }

    /**
     * 处理搜索数据结果
     * 优化版本：批量处理数据，减少UI更新频率
     */
    private fun searchData(absXml: AbsXml?) {
        // 在后台线程中处理数据
        LogUtils.d("searchData: 开始处理搜索结果")
        ThreadPoolManager.executeCompute {
            val data: MutableList<Movie.Video> = ArrayList()
            val newSourceKeys = HashSet<String>() // 使用HashSet避免重复

            if ((absXml != null) && (absXml.movie != null) && (absXml.movie.videoList != null) && (absXml.movie.videoList.size > 0)) {
                // 处理视频列表
                for (video: Movie.Video in absXml.movie.videoList) {
                    if (!matchSearchResult(video.name, searchTitle)) continue

                    // 预加载缩略图 - 使用低优先级
                    if (!video.pic.isNullOrEmpty()) {
                        ThreadPoolManager.executeIO {
                            try {
                                com.github.tvbox.osc.util.GlideHelper.preloadImage(this@FastSearchActivity, video.pic)
                            } catch (_: Exception) {}
                        }
                    }

                    data.add(video)

                    // 收集源信息
                    if (!resultVods.containsKey(video.sourceKey)) {
                        resultVods[video.sourceKey] = ArrayList()
                        newSourceKeys.add(video.sourceKey)
                    }
                    resultVods[video.sourceKey]!!.add(video)
                }
            }

            // 批量更新左侧目录
            if (newSourceKeys.isNotEmpty()) {
                updateSearchWordList(newSourceKeys)
            }

            // 更新搜索结果
            updateSearchResults(data)
        }
    }

    /**
     * 批量更新左侧目录
     */
    private fun updateSearchWordList(sourceKeys: Set<String>) {
        // 收集所有新的搜索词
        val newWords = ArrayList<String>()

        for (sourceKey in sourceKeys) {
            val source = ApiConfig.get().getSource(sourceKey)
            if (source != null) {
                val name = source.getName()
                if (!searchWordAdapter.data.contains(name)) {
                    newWords.add(name)
                    spNames[name] = sourceKey
                }
            }
        }

        // 一次性更新UI
        if (newWords.isNotEmpty()) {
            ThreadPoolManager.executeMain {
                // 添加到现有列表
                val currentWords = ArrayList(searchWordAdapter.data)
                currentWords.addAll(newWords)

                // 更新适配器
                searchWordAdapter.setNewData(currentWords)
            }
        }
    }

    /**
     * 更新搜索结果
     */
    private fun updateSearchResults(data: List<Movie.Video>) {
        ThreadPoolManager.executeMain {
            LogUtils.d("searchData: 在UI线程更新搜索结果，数量: " + data.size)
            if (data.isNotEmpty()) {
                if (searchAdapter.data.size > 0) {
                    LogUtils.d("searchData: 添加新数据到现有结果")
                    searchAdapter.addData(data)
                } else {
                    LogUtils.d("searchData: 设置新数据到空结果")
                    showSuccess()

                    // 始终显示搜索结果区域
                    mBinding.llSearchResult.visibility = View.VISIBLE

                    if (!isFilterMode) {
                        LogUtils.d("searchData: 设置搜索结果区域可见")
                        mBinding.mGridView.visibility = View.VISIBLE
                        mBinding.mGridViewFilter.visibility = View.GONE
                    } else {
                        LogUtils.d("searchData: 设置过滤搜索结果区域可见")
                        mBinding.mGridView.visibility = View.GONE
                        mBinding.mGridViewFilter.visibility = View.VISIBLE
                    }

                    searchAdapter.setNewData(data)
                }
            }

            val count = allRunCount.decrementAndGet()
            LogUtils.d("searchData: 剩余搜索请求数量: " + count)
            if (count <= 0) {
                LogUtils.d("searchData: 所有搜索请求完成")
                if (searchAdapter.data.size <= 0) {
                    LogUtils.d("searchData: 无搜索结果，显示空状态")
                    showEmpty()
                } else {
                    LogUtils.d("searchData: 有搜索结果，显示成功状态")
                    // 确保搜索结果显示成功状态
                    showSuccess()

                    // 确保搜索结果区域可见
                    mBinding.llSearchResult.visibility = View.VISIBLE

                    if (!isFilterMode) {
                        LogUtils.d("searchData: 设置搜索结果区域可见")
                        mBinding.mGridView.visibility = View.VISIBLE
                        mBinding.mGridViewFilter.visibility = View.GONE
                    } else {
                        LogUtils.d("searchData: 设置过滤搜索结果区域可见")
                        mBinding.mGridView.visibility = View.GONE
                        mBinding.mGridViewFilter.visibility = View.VISIBLE
                    }
                }
                cancel()
            }
        }
    }

    private fun cancel() {
        OkGo.getInstance().cancelTag("search")
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        try {
            // Remove any pending search callbacks
            searchDebounceRunnable?.let { searchHandler.removeSearchCallback(it) }
            searchDebounceRunnable = null

            if (searchExecutorService != null) {
                searchExecutorService!!.shutdownNow()
                searchExecutorService = null
                JsLoader.load()
            }

            // 清理适配器引用，防止内存泄漏
            if (mBinding.mGridView != null) {
                mBinding.mGridView.adapter = null
            }
            if (mBinding.mGridViewFilter != null) {
                mBinding.mGridViewFilter.adapter = null
            }
            if (mBinding.rvSearchWord != null) {
                mBinding.rvSearchWord.adapter = null
            }

            // 清理集合数据
            resultVods.clear()
            spNames.clear()

            // 确保对话框已关闭
            if (mSearchSuggestionsDialog != null && mSearchSuggestionsDialog!!.isShow) {
                mSearchSuggestionsDialog!!.dismiss()
            }
            mSearchSuggestionsDialog = null
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
    override fun afterTextChanged(editable: Editable) {
        val text = editable.toString()
        if (TextUtils.isEmpty(text)) {
            mSearchSuggestionsDialog?.dismiss()
            hideHotAndHistorySearch(false)
        } else {
            // 取消之前的搜索建议请求
            searchDebounceRunnable?.let { searchHandler.removeSearchCallback(it) }

            // 如果文本长度大于等于2个字符，立即执行搜索
            if (text.length >= 2) {
                // 立即执行搜索，不设置延迟
                search(text)
            }

            // 创建新的延迟任务，仅用于获取搜索建议
            searchDebounceRunnable = Runnable {
                // 获取搜索建议
                getSuggest(text)
            }

            // 设置较短的延迟时间用于搜索建议
            searchHandler.postSearchSuggestion(searchDebounceRunnable!!, 300) // 300ms延迟
        }
    }

    private fun getDoubanSuggest(text: String) {
        OkGo.get<String>("https://movie.douban.com/j/subject_suggest?q="+text.trim())
            .execute(object : StringCallback(){
                override fun onSuccess(response: com.lzy.okgo.model.Response<String>?) {
                    val list = GsonUtils.fromJson<List<DoubanSuggestBean>>(
                        response?.body(),
                        object : TypeToken<List<DoubanSuggestBean>>() {}.type
                    )

                    //暂时只保留第一个,分数查询接口有限制
                    val filterList = list.filter {
                        it.title == text
                    }
                    if (filterList.isEmpty()){
                        ToastUtils.showShort("暂无评分信息")
                        return
                    }

                    XPopup.Builder(this@FastSearchActivity)
                        .maxHeight(ScreenUtils.getScreenHeight() - (ScreenUtils.getScreenHeight() / 4))
                        .asCustom(DoubanSuggestDialog(this@FastSearchActivity,filterList.subList(0,1)))
                        .show()
                }
            })
    }
}