package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.util.MD3ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.xmbox.app.R
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.Source
import com.github.tvbox.osc.bean.Subscription
import com.github.tvbox.osc.callback.EmptySubscriptionCallback
import com.xmbox.app.databinding.ActivitySubscriptionBinding
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.dialog.ConfirmDialog
import com.github.tvbox.osc.ui.adapter.MenuAdapter
import com.github.tvbox.osc.ui.adapter.SubscriptionAdapter
import com.github.tvbox.osc.ui.dialog.ChooseSourceDialog
import com.github.tvbox.osc.ui.dialog.MenuDialog
import com.github.tvbox.osc.ui.dialog.RenameDialog
import com.github.tvbox.osc.ui.dialog.SubsTipDialog
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog
import com.github.tvbox.osc.ui.dialog.SubsciptionDialog.OnSubsciptionListener
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.UrlUtil
import com.github.tvbox.osc.util.Utils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import com.obsez.android.lib.filechooser.ChooserDialog
import com.orhanobut.hawk.Hawk
import org.greenrobot.eventbus.EventBus
import java.util.function.Consumer

class SubscriptionActivity : BaseVbActivity<ActivitySubscriptionBinding>() {

    private var mBeforeUrl = Hawk.get(HawkConfig.API_URL, "")
    private var mSelectedUrl = ""
    private var mSubscriptions: MutableList<Subscription> = Hawk.get(HawkConfig.SUBSCRIPTIONS, ArrayList())
    private var mSubscriptionAdapter = SubscriptionAdapter()
    private val mSources: MutableList<Source> = ArrayList()

    override fun init() {
        // 自定义返回图标，使用Material Symbols字体
        val leftView = mBinding.titleBar.getLeftView()
        if (leftView is TextView) {
            leftView.text = getString(R.string.ms_arrow_back)
            // 应用Material Symbols字体
            com.github.tvbox.osc.util.MaterialSymbolsLoader.apply(leftView)
            // 设置正确的大小和样式
            leftView.textSize = 24f  // 24sp是M3规范的标准图标大小
            leftView.setTextColor(getColor(R.color.md3_on_surface))
        }

        setLoadSir(mBinding.rv, EmptySubscriptionCallback::class.java)

        mBinding.rv.setAdapter(mSubscriptionAdapter)
        mSubscriptions.forEach(Consumer { item: Subscription ->
            if (item.isChecked) {
                mSelectedUrl = item.url
            }
        })

        mSubscriptionAdapter.setNewData(mSubscriptions)

        if (mSubscriptions.isEmpty()) {
            showEmpty(EmptySubscriptionCallback::class.java)
        } else {
            showSuccess()
        }
        mBinding.ivUseTip.setOnClickListener {
            XPopup.Builder(this)
                .asCustom(SubsTipDialog(this))
                .show()
        }

        mBinding.fabAdd.setOnClickListener {//添加订阅
            XPopup.Builder(this)
                .autoFocusEditText(false)
                .asCustom(
                    SubsciptionDialog(
                        this,
                        "订阅: " + (mSubscriptions.size + 1),
                        object : OnSubsciptionListener {
                            override fun onConfirm(
                                name: String,
                                url: String,
                                checked: Boolean
                            ) { //只有addSub2List用到,看注释,单线路才生效,其余方法仅作为参数继续传递
                                for (item in mSubscriptions) {
                                    if (item.url == url) {
                                        ToastUtils.showLong("订阅地址与" + item.name + "相同")
                                        return
                                    }
                                }
                                addSubscription(name, url, checked)
                            }


                        })
                ).show()
        }

        mSubscriptionAdapter.setOnItemChildClickListener { _: BaseQuickAdapter<*, *>?, view: View, position: Int ->
            LogUtils.d("删除订阅")
            if (view.id == R.id.iv_del) {
                if (mSubscriptions.get(position).isChecked) {
                    // 使用Material Design 3风格的对话框替代Toast提示
                    XPopup.Builder(this@SubscriptionActivity)
                        .isDarkTheme(Utils.isDarkTheme())
                        .asCustom(ConfirmDialog(
                            this@SubscriptionActivity,
                            "提示",
                            "不能删除当前使用的订阅",
                            "",
                            "确定",
                            object : ConfirmDialog.OnDialogActionListener {
                                override fun onConfirm() {
                                    // 点击确定按钮关闭对话框
                                }
                            }
                        )).show()
                    return@setOnItemChildClickListener
                }
                XPopup.Builder(this@SubscriptionActivity)
                    .isDarkTheme(Utils.isDarkTheme())
                    .asCustom(ConfirmDialog(
                        this@SubscriptionActivity,
                        "删除订阅",
                        "确定删除订阅吗？",
                        "取消",
                        "确定",
                        object : ConfirmDialog.OnDialogActionListener {
                            override fun onConfirm() {
                                mSubscriptions.removeAt(position)
                                //删除后立即保存数据
                                saveSubscriptions()
                                //删除/选择只刷新,不触发重新排序
                                mSubscriptionAdapter.notifyDataSetChanged()
                            }
                        }
                    )).show()
            }
        }

        mSubscriptionAdapter.setOnItemClickListener { _: BaseQuickAdapter<*, *>?, _: View?, position: Int ->  //选择订阅
            for (i in mSubscriptions.indices) {
                val subscription = mSubscriptions[i]
                if (i == position) {
                    subscription.setChecked(true)
                    mSelectedUrl = subscription.url
                } else {
                    subscription.setChecked(false)
                }
            }
            //保存选择状态
            saveSubscriptions()
            //删除/选择只刷新,不触发重新排序
            mSubscriptionAdapter.notifyDataSetChanged()
        }

        mSubscriptionAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View, position: Int ->
                val item = mSubscriptions[position]

                // 创建菜单项列表
                val menuItems = ArrayList<MenuAdapter.MenuItem>()
                menuItems.add(
                    MenuAdapter.MenuItem(
                        if (item.isTop) "取消置顶" else "置顶",
                        if (item.isTop) R.drawable.ic_unpin_m3 else R.drawable.ic_pin_m3
                    )
                )
                menuItems.add(MenuAdapter.MenuItem("重命名", R.drawable.ic_edit_m3))
                menuItems.add(MenuAdapter.MenuItem("复制地址", R.drawable.ic_content_copy_m3))

                // 创建菜单对话框
                val menuDialog = MenuDialog(this, menuItems)
                menuDialog.setOnItemClickListener { index ->
                    when (index) {
                        0 -> {
                            item.isTop = !item.isTop
                            mSubscriptions[position] = item
                            saveSubscriptions()
                            mSubscriptionAdapter.setNewData(mSubscriptions)
                        }
                        1 -> {
                            XPopup.Builder(this)
                                .asCustom(
                                    RenameDialog(
                                        this,
                                        "更改为",
                                        "新的订阅名",
                                        item.name,
                                        object : RenameDialog.OnRenameListener {
                                            override fun onRename(text: String) {
                                                item.name = text.trim()
                                                saveSubscriptions()
                                                mSubscriptionAdapter.notifyItemChanged(position)
                                            }
                                        }
                                    )
                                ).show()
                        }
                        2 -> {
                            ClipboardUtils.copyText(mSubscriptions.get(position).url)
                            MD3ToastUtils.showToast("已复制")
                        }
                    }
                }

                // 显示菜单
                XPopup.Builder(this)
                    .atView(view.findViewById(R.id.tv_name))
                    .hasShadowBg(false)
                    .asCustom(menuDialog)
                    .show()

                true
            }
    }



    private fun addSubscription(name: String, url: String, checked: Boolean) {
        if (url.startsWith("clan://")) {
            // 本地订阅直接添加
            addSub2List(name, url, checked)
        } else if (url.startsWith("http")) {
            showLoadingDialog()

            // 对于特定的URL直接添加，不尝试解析内容
            if (url == "http://ok321.top/tv" || url == "https://7213.kstore.vip/吃猫的鱼" || url.startsWith("https://7213.kstore.vip/") || url == "http://www.饭太硬.com/tv") {
                dismissLoadingDialog()
                addSub2List(name, url, checked)
                MD3ToastUtils.showToast("添加订阅成功")
                return
            }

            // 处理URL，包括特殊URL映射和Punycode转换
            val encodedUrl = UrlUtil.processUrl(url)

            // 添加超时处理
            val timeoutRunnable = Runnable {
                try {
                    dismissLoadingDialog()
                    // 超时时作为单线路处理
                    addSub2List(name, url, checked)
                    MD3ToastUtils.showToast("网络超时，已作为单线路订阅添加")
                } catch (e: Exception) {
                    MD3ToastUtils.showToast("网络超时，添加失败")
                }
            }

            // 15秒后自动超时
            mBinding.root.postDelayed(timeoutRunnable, 15000)

            OkGo.get<String>(encodedUrl)
                .tag("get_subscription")
                .execute(object : AbsCallback<String?>() {
                    override fun onSuccess(response: Response<String?>) {
                        try {
                            // 取消超时处理
                            mBinding.root.removeCallbacks(timeoutRunnable)
                            dismissLoadingDialog()
                            val responseBody = response.body()
                            if (responseBody.isNullOrBlank()) {
                                // 响应为空，作为单线路处理
                                addSub2List(name, url, checked)
                                MD3ToastUtils.showToast("添加订阅成功")
                                return
                            }
                            val json = JsonParser.parseString(responseBody).asJsonObject
                            // 多线路?
                            val urls = json["urls"]
                            // 多仓?
                            val storeHouse = json["storeHouse"]
                            if (urls != null && urls.isJsonArray) { // 多线路
                                if (checked) {
                                    ToastUtils.showLong("多条线路请主动选择")
                                }
                                val urlList = urls.asJsonArray
                                if (urlList != null && urlList.size() > 0 && urlList[0].isJsonObject
                                    && urlList[0].asJsonObject.has("url")
                                    && urlList[0].asJsonObject.has("name")
                                ) { //多线路格式
                                    for (i in 0 until urlList.size()) {
                                        val obj = urlList[i] as JsonObject
                                        val name = obj["name"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val url = obj["url"].asString.trim { it <= ' ' }
                                        mSubscriptions.add(Subscription(name, url))
                                    }
                                    // 立即保存数据并刷新UI
                                    saveSubscriptions()
                                    mSubscriptionAdapter.setNewData(mSubscriptions)
                                    // 确保显示内容
                                    if (mSubscriptions.isNotEmpty()) {
                                        showSuccess()
                                    }
                                    // 提示用户
                                    MD3ToastUtils.showToast("添加多线路订阅成功")
                                }
                            } else if (storeHouse != null && storeHouse.isJsonArray) { // 多仓
                                val storeHouseList = storeHouse.asJsonArray
                                if (storeHouseList != null && storeHouseList.size() > 0 && storeHouseList[0].isJsonObject
                                    && storeHouseList[0].asJsonObject.has("sourceName")
                                    && storeHouseList[0].asJsonObject.has("sourceUrl")
                                ) { //多仓格式
                                    mSources.clear()
                                    for (i in 0 until storeHouseList.size()) {
                                        val obj = storeHouseList[i] as JsonObject
                                        val name = obj["sourceName"].asString.trim { it <= ' ' }
                                            .replace("<|>|《|》|-".toRegex(), "")
                                        val url = obj["sourceUrl"].asString.trim { it <= ' ' }
                                        mSources.add(Source(name, url))
                                    }
                                    XPopup.Builder(this@SubscriptionActivity)
                                        .asCustom(
                                            ChooseSourceDialog(
                                                this@SubscriptionActivity,
                                                mSources
                                            ) { position: Int, _: String? ->
                                                // 再根据多线路格式获取配置,如果仓内是正常多线路模式,name没用,直接使用线路的命名
                                                addSubscription(
                                                    mSources[position].sourceName,
                                                    mSources[position].sourceUrl,
                                                    checked
                                                )
                                            })
                                        .show()
                                }
                            } else { // 单线路/其余
                                addSub2List(name, url, checked)
                                // 提示用户
                                MD3ToastUtils.showToast("添加订阅成功")
                            }
                        } catch (th: Throwable) {
                            try {
                                // 取消超时处理
                                mBinding.root.removeCallbacks(timeoutRunnable)
                                dismissLoadingDialog()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            // 异常情况下作为单线路处理
                            addSub2List(name, url, checked)
                            // 提示用户
                            MD3ToastUtils.showToast("添加订阅成功")
                        }
                    }

                    @Throws(Throwable::class)
                    override fun convertResponse(response: okhttp3.Response): String {
                        return response.body!!.string()
                    }

                    override fun onError(response: Response<String?>) {
                        super.onError(response)
                        try {
                            // 取消超时处理
                            mBinding.root.removeCallbacks(timeoutRunnable)
                            dismissLoadingDialog()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // 网络错误时，尝试作为单线路处理
                        try {
                            addSub2List(name, url, checked)
                            MD3ToastUtils.showToast("网络异常，已作为单线路订阅添加")
                        } catch (e: Exception) {
                            MD3ToastUtils.showToast("订阅失败，请检查地址或网络状态")
                        }
                    }
                })
        } else {
            ToastUtils.showShort("订阅格式不正确")
        }
    }

    /**
     * 仅当选中本地文件和添加的为单线路时,使用此订阅生效。多线路会直接解析全部并添加,多仓会展开并选择,最后也按多线路处理,直接添加
     * @param name
     * @param url
     * @param checkNewest
     */
    private fun addSub2List(name: String, url: String, checkNewest: Boolean) {
        if (checkNewest) { //选中最新的,清除以前的选中订阅
            for (subscription in mSubscriptions) {
                if (subscription.isChecked) {
                    subscription.setChecked(false)
                }
            }
            mSelectedUrl = url
            mSubscriptions.add(Subscription(name, url).setChecked(true))
        } else {
            mSubscriptions.add(Subscription(name, url).setChecked(false))
        }

        // 立即保存订阅数据到Hawk
        saveSubscriptions()

        // 刷新UI显示
        mSubscriptionAdapter.setNewData(mSubscriptions)

        // 确保列表不为空时显示内容
        if (mSubscriptions.isNotEmpty()) {
            showSuccess()
        }
    }

    /**
     * 保存订阅数据到Hawk
     */
    private fun saveSubscriptions() {
        Hawk.put(HawkConfig.API_URL, mSelectedUrl)
        Hawk.put<List<Subscription>?>(HawkConfig.SUBSCRIPTIONS, mSubscriptions)
    }

    override fun onPause() {
        super.onPause()
        // 更新缓存
        saveSubscriptions()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消所有网络请求
        try {
            OkGo.getInstance().cancelTag("get_subscription")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 确保关闭加载对话框
        try {
            dismissLoadingDialog()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun finish() {
        //切换了订阅地址
        if (!TextUtils.isEmpty(mSelectedUrl) && mBeforeUrl != mSelectedUrl) {
            // 使用EventBus发送源变更事件，而不是重启整个应用
            EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_API_URL_CHANGE))

            // 创建返回到首页的Intent
            val intent = Intent(this, MainActivity::class.java)
            // 清除任务栈中MainActivity之上的所有Activity
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            // 添加过渡动画
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        super.finish()
    }
}