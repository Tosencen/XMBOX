package com.github.tvbox.osc.ui.activity

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.blankj.utilcode.util.ToastUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.util.MD3ToastUtils
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.IJKCode
import com.github.tvbox.osc.constant.IntentKey
import com.github.tvbox.osc.databinding.ActivitySettingBinding
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectDialogInterface
import com.github.tvbox.osc.ui.dialog.BackupDialog

import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.HistoryHelper
import com.github.tvbox.osc.util.MaterialSymbols
import com.github.tvbox.osc.util.MaterialSymbolsLoader
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.PlayerHelper
import com.github.tvbox.osc.util.Utils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.lxj.xpopup.XPopup
import com.orhanobut.hawk.Hawk
import okhttp3.HttpUrl
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class SettingActivity : BaseVbActivity<ActivitySettingBinding>() {

    private var homeRec = Hawk.get(HawkConfig.HOME_REC, 0)
    private var dnsOpt = Hawk.get(HawkConfig.DOH_URL, 0)

    override fun init() {
        // 设置返回图标
        val backIcon = android.widget.TextView(this)
        backIcon.text = MaterialSymbols.ARROW_BACK
        backIcon.setTextAppearance(R.style.Widget_App_MaterialSymbols_Setting_Back)
        mBinding.titleBar.leftView = backIcon
        mBinding.titleBar.leftView.setOnClickListener { onBackPressed() }
        mBinding.tvMediaCodec.text = Hawk.get(HawkConfig.IJK_CODEC, "")

        // 确保使用有效的索引访问 dnsHttpsList
        val dnsIndex = Hawk.get(HawkConfig.DOH_URL, 0)
        val safeDnsIndex = if (dnsIndex >= 0 && dnsIndex < OkGoHelper.dnsHttpsList.size) {
            dnsIndex
        } else {
            // 如果索引无效，重置为0（关闭）
            Hawk.put(HawkConfig.DOH_URL, 0)
            0
        }
        mBinding.tvDns.text = OkGoHelper.dnsHttpsList[safeDnsIndex]
        mBinding.tvHomeRec.text = getHomeRecName(Hawk.get(HawkConfig.HOME_REC, 0))
        mBinding.tvHistoryNum.text =
            HistoryHelper.getHistoryNumName(Hawk.get(HawkConfig.HISTORY_NUM, 0))
        mBinding.tvScaleType.text = PlayerHelper.getScaleName(Hawk.get(HawkConfig.PLAY_SCALE, 0))
        mBinding.tvPlay.text = PlayerHelper.getPlayerName(Hawk.get(HawkConfig.PLAY_TYPE, 0))
        mBinding.tvRenderType.text =
            PlayerHelper.getRenderName(Hawk.get(HawkConfig.PLAY_RENDER, 0))

        mBinding.switchPrivateBrowsing.setChecked(Hawk.get(HawkConfig.PRIVATE_BROWSING, false))
        mBinding.llPrivateBrowsing.setOnClickListener { view: View? ->
            val newConfig = !Hawk.get(HawkConfig.PRIVATE_BROWSING, false)
            mBinding.switchPrivateBrowsing.setChecked(newConfig)
            Hawk.put(HawkConfig.PRIVATE_BROWSING, newConfig)
        }



        val defaultBgPlayTypePos = Hawk.get(HawkConfig.BACKGROUND_PLAY_TYPE, 0)
        val bgPlayTypes = ArrayList<String>()
        bgPlayTypes.add("关闭")
        bgPlayTypes.add("开启")
        bgPlayTypes.add("画中画")
        mBinding.tvBackgroundPlayType.text = bgPlayTypes[defaultBgPlayTypePos]
        mBinding.llBackgroundPlay.setOnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            val dialog = SelectDialog<String>(this@SettingActivity)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogInterface<String?> {
                override fun click(value: String?, pos: Int) {
                    mBinding.tvBackgroundPlayType.text = value
                    Hawk.put(HawkConfig.BACKGROUND_PLAY_TYPE, pos)
                }

                override fun getDisplay(name: String?): String {
                    return name?:""
                }
            },SelectDialogAdapter.stringDiff, bgPlayTypes, defaultBgPlayTypePos)
            dialog.show()
        }

        mBinding.tvSpeed.text = Hawk.get(HawkConfig.VIDEO_SPEED, 2.0f).toString()
        mBinding.llPressSpeed.setOnClickListener {
            val types = ArrayList<String>()
            types.add("2.0")
            types.add("3.0")
            types.add("4.0")
            types.add("5.0")
            types.add("6.0")
            types.add("8.0")
            types.add("10.0")
            val defaultPos = types.indexOf(Hawk.get(HawkConfig.VIDEO_SPEED, 2.0f).toString())
            val dialog = SelectDialog<String>(this@SettingActivity)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogInterface<String?> {
                override fun click(value: String?, pos: Int) {
                    Hawk.put(HawkConfig.VIDEO_SPEED, value?.toFloat())
                    mBinding.tvSpeed.text = value
                }

                override fun getDisplay(name: String?): String {
                    return name ?: ""
                }
            }, SelectDialogAdapter.stringDiff, types, defaultPos)
            dialog.show()
        }

        mBinding.llBackup.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            if (XXPermissions.isGranted(this@SettingActivity, Permission.MANAGE_EXTERNAL_STORAGE)) {
                val dialog = BackupDialog(this@SettingActivity)
                dialog.show()
            } else {
                XXPermissions.with(this@SettingActivity)
                    .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                val dialog = BackupDialog(this@SettingActivity)
                                dialog.show()
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            if (never) {
                                ToastUtils.showLong("获取存储权限失败,请在系统设置中开启")
                                XXPermissions.startPermissionActivity(
                                    this@SettingActivity,
                                    permissions
                                )
                            } else {
                                ToastUtils.showShort("获取存储权限失败")
                            }
                        }
                    })
            }
        }

        // DNS设置功能
        mBinding.llDns.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val currentValue = Hawk.get(HawkConfig.DOH_URL, 0)
            // 切换DNS状态：如果当前是关闭状态，则开启；如果当前是开启状态，则关闭
            val newValue = if (currentValue == 0) 1 else 0
            // 确保新值在有效范围内
            if (newValue >= 0 && newValue < OkGoHelper.dnsHttpsList.size) {
                Hawk.put(HawkConfig.DOH_URL, newValue)
                mBinding.tvDns.text = OkGoHelper.dnsHttpsList[newValue]
                dnsOpt = newValue
                OkGoHelper.dnsOverHttps = null
                OkGoHelper.init()
                // 显示提示信息
                if (newValue == 1) {
                    MD3ToastUtils.showToast("已开启阿里DNS，网络访问更安全")
                } else {
                    MD3ToastUtils.showToast("已关闭安全DNS")
                }
            } else {
                // 如果新值无效，重置为0（关闭）
                Hawk.put(HawkConfig.DOH_URL, 0)
                mBinding.tvDns.text = OkGoHelper.dnsHttpsList[0]
                dnsOpt = 0
                OkGoHelper.dnsOverHttps = null
                OkGoHelper.init()
                MD3ToastUtils.showToast("已关闭安全DNS")
            }
        }

        mBinding.llMediaCodec.setOnClickListener { v: View? ->
            val ijkCodes = ApiConfig.get().ijkCodes
            if (ijkCodes == null || ijkCodes.size == 0) return@setOnClickListener
            FastClickCheckUtil.check(v)
            var defaultPos = 0
            val ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "")
            for (j in ijkCodes.indices) {
                if (ijkSel == ijkCodes[j].name) {
                    defaultPos = j
                    break
                }
            }
            val dialog = SelectDialog<IJKCode>(this@SettingActivity)
            dialog.setTip("请选择IJK解码")
            dialog.setAdapter(object : SelectDialogInterface<IJKCode?> {
                override fun click(value: IJKCode?, pos: Int) {
                    value?.selected(true)
                    mBinding.tvMediaCodec.text = value?.name
                }

                override fun getDisplay(code: IJKCode?): String {
                    return code?.name ?: ""
                }
            }, object : DiffUtil.ItemCallback<IJKCode>() {
                override fun areItemsTheSame(oldItem: IJKCode, newItem: IJKCode): Boolean {
                    return oldItem === newItem
                }

                override fun areContentsTheSame(oldItem: IJKCode, newItem: IJKCode): Boolean {
                    return oldItem.name.contentEquals(newItem.name)
                }
            }, ijkCodes, defaultPos)
            dialog.show()
        }

        mBinding.llScale.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0)
            val players = ArrayList<Int>()
            players.add(0)
            players.add(1)
            players.add(2)
            players.add(3)
            players.add(4)
            players.add(5)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择画面缩放")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_SCALE, value)
                    mBinding.tvScaleType.text = value?.let { PlayerHelper.getScaleName(it) }
                }

                override fun getDisplay(value: Int?): String {
                    return PlayerHelper.getScaleName(value ?: 0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, players, defaultPos)
            dialog.show()
        }

        mBinding.llPlay.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
            var defaultPos = 0
            val players = PlayerHelper.getExistPlayerTypes()
            val renders = ArrayList<Int>()
            for (p in players.indices) {
                renders.add(p)
                if (players[p] == playerType) {
                    defaultPos = p
                }
            }
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择默认播放器")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    val thisPlayerType = players[pos]
                    Hawk.put(HawkConfig.PLAY_TYPE, thisPlayerType)
                    mBinding.tvPlay.text = PlayerHelper.getPlayerName(thisPlayerType)
                    PlayerHelper.init()
                }

                override fun getDisplay(value: Int?): String {
                    return PlayerHelper.getPlayerName(players[value?:0])
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, renders, defaultPos)
            dialog.show()
        }

        mBinding.llRender.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0)
            val renders = ArrayList<Int>()
            renders.add(0)
            renders.add(1)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择默认渲染方式")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.PLAY_RENDER, value)
                    mBinding.tvRenderType.text = PlayerHelper.getRenderName(value?:0)
                    PlayerHelper.init()
                }

                override fun getDisplay(value: Int?): String {
                    return PlayerHelper.getRenderName(value?:0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, renders, defaultPos)
            dialog.show()
        }
        mBinding.llHomeRec.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.HOME_REC, 0)
            val types = ArrayList<Int>()
            types.add(0)
            types.add(1)
            types.add(2)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("主页内容显示")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.HOME_REC, value)
                    mBinding.tvHomeRec.text = getHomeRecName(value?:0)
                }

                override fun getDisplay(value: Int?): String {
                    return getHomeRecName(value?:0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, defaultPos)
            dialog.show()
        }

        mBinding.llHistoryNum.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val defaultPos = Hawk.get(HawkConfig.HISTORY_NUM, 0)
            val types = ArrayList<Int>()
            types.add(0)
            types.add(1)
            types.add(2)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("保留历史记录数量")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    Hawk.put(HawkConfig.HISTORY_NUM, value)
                    mBinding.tvHistoryNum.text = HistoryHelper.getHistoryNumName(value?:0)
                }

                override fun getDisplay(value: Int?): String {
                    return HistoryHelper.getHistoryNumName(value?:0)
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, defaultPos)
            dialog.show()
        }
        // 计算缓存大小
        calculateCacheSize()

        // 清空缓存按钮点击事件
        mBinding.btnClearCache.setOnClickListener {
            FastClickCheckUtil.check(it)
            clearCache()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mBinding.llTheme.visibility = View.GONE
        }
        val oldTheme = Hawk.get(HawkConfig.THEME_TAG, 0)
        val themes = arrayOf("跟随系统", "浅色", "深色")
        mBinding.tvTheme.text = themes[oldTheme]
        mBinding.llTheme.setOnClickListener(View.OnClickListener { view: View? ->
            FastClickCheckUtil.check(view)
            val types = ArrayList<Int>()
            types.add(0)
            types.add(1)
            types.add(2)
            val dialog = SelectDialog<Int>(this@SettingActivity)
            dialog.setTip("请选择")
            dialog.setAdapter(object : SelectDialogInterface<Int?> {
                override fun click(value: Int?, pos: Int) {
                    mBinding.tvTheme.text = themes[value?:0]
                    Hawk.put(HawkConfig.THEME_TAG, value)
                }

                override fun getDisplay(value: Int?): String {
                    return themes[value?:0]
                }
            }, object : DiffUtil.ItemCallback<Int>() {
                override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }

                override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
                    return oldItem == newItem
                }
            }, types, oldTheme)
            dialog.setOnDismissListener { dialog1: DialogInterface? ->
                if (oldTheme != Hawk.get(HawkConfig.THEME_TAG, 0)) {
                    Utils.initTheme()
                    val bundle = Bundle()
                    bundle.putBoolean(IntentKey.CACHE_CONFIG_CHANGED, true)
                    jumpActivity(MainActivity::class.java, bundle)
                }
            }
            dialog.show()
        })

        mBinding.switchVideoPurify.setChecked(Hawk.get(HawkConfig.VIDEO_PURIFY, true))
        // toggle purify video -------------------------------------
        mBinding.llVideoPurify.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val newConfig = !Hawk.get(HawkConfig.VIDEO_PURIFY, true)
            mBinding.switchVideoPurify.setChecked(newConfig)
            Hawk.put(HawkConfig.VIDEO_PURIFY, newConfig)
        }
        mBinding.switchIjkCachePlay.setChecked(Hawk.get(HawkConfig.IJK_CACHE_PLAY, false))
        mBinding.llIjkCachePlay.setOnClickListener { v: View? ->
            FastClickCheckUtil.check(v)
            val newConfig = !Hawk.get(HawkConfig.IJK_CACHE_PLAY, false)
            mBinding.switchIjkCachePlay.setChecked(newConfig)
            Hawk.put(HawkConfig.IJK_CACHE_PLAY, newConfig)
        }
    }

    override fun onBackPressed() {
        if (homeRec != Hawk.get(HawkConfig.HOME_REC, 0) || dnsOpt != Hawk.get(
                HawkConfig.DOH_URL,
                0
            )
        ) { // 首页类型/dns/doh有更改,需重载页面
            val bundle = Bundle()
            bundle.putBoolean(IntentKey.CACHE_CONFIG_CHANGED, true)
            jumpActivity(MainActivity::class.java, bundle)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        } else {
            super.onBackPressed()
        }
    }

    private fun calculateCacheSize() {
        Thread {
            try {
                val cachePath = FileUtils.getCachePath()
                val cacheDir = File(cachePath)
                if (!cacheDir.exists()) {
                    runOnUiThread {
                        mBinding.tvCacheSizeLabel.text = "缓存大小（0 MB）"
                    }
                    return@Thread
                }
                val size = FileUtils.getFileSize(cacheDir)
                val readableSize = FileUtils.formatFileSize(size)
                runOnUiThread {
                    mBinding.tvCacheSizeLabel.text = "缓存大小（${readableSize}）"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    mBinding.tvCacheSizeLabel.text = "缓存大小（计算失败）"
                }
            }
        }.start()
    }

    private fun clearCache() {
        val cachePath = FileUtils.getCachePath()
        val cacheDir = File(cachePath)
        if (!cacheDir.exists()) return

        // 显示清理中的提示
        mBinding.tvCacheSizeLabel.text = "缓存大小（清理中...）"
        mBinding.btnClearCache.isEnabled = false

        Thread {
            try {
                FileUtils.cleanDirectory(cacheDir)
                runOnUiThread {
                    MD3ToastUtils.showToast("缓存已清空")
                    mBinding.tvCacheSizeLabel.text = "缓存大小（0 MB）"
                    mBinding.btnClearCache.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    MD3ToastUtils.showToast("清空缓存失败")
                    calculateCacheSize() // 重新计算缓存大小
                    mBinding.btnClearCache.isEnabled = true
                }
            }
        }.start()
    }

    private fun getHomeRecName(type: Int): String {
        return when (type) {
            0 -> "豆瓣热播"
            1 -> "站点推荐"
            else -> "关闭"
        }
    }
}