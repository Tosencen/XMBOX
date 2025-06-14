package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseVbActivity
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.cache.VodCollect
import com.github.tvbox.osc.callback.EmptyCollectCallback
import com.xmbox.app.databinding.ActivityCollectBinding
import com.github.tvbox.osc.ui.adapter.CollectAdapter
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.MD3DialogUtils
import com.github.tvbox.osc.util.Utils
import com.github.tvbox.osc.ui.dialog.ConfirmDialog
import com.owen.tvrecyclerview.widget.V7GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectActivity : BaseVbActivity<ActivityCollectBinding>() {

    private var collectAdapter  = CollectAdapter()
    override fun init() {
        initView()
        initData()
    }

    private fun initView() {
        setLoadSir(mBinding.mGridView, EmptyCollectCallback::class.java)

        mBinding.mGridView.setHasFixedSize(true)
        mBinding.mGridView.setLayoutManager(GridLayoutManager(this, 3))
        mBinding.mGridView.setAdapter(collectAdapter)

        // 初始化时隐藏删除按钮，等待数据加载后再决定是否显示
        mBinding.titleBar.rightView.visibility = View.GONE

        mBinding.titleBar.rightView.setOnClickListener {
            MD3DialogUtils.showConfirmDialog(
                this,
                "提示",
                "确定清空?",
                "取消",
                "确定",
                object : ConfirmDialog.OnDialogActionListener {
                    override fun onConfirm() {
                        showLoadingDialog()
                        lifecycleScope.launch(Dispatchers.IO){
                            RoomDataManger.deleteVodCollectAll()
                            withContext(Dispatchers.Main){
                                dismissLoadingDialog()
                                collectAdapter.setNewData(ArrayList())
                                mBinding.topTip.visibility = View.GONE
                                showEmpty(EmptyCollectCallback::class.java)
                            }
                        }
                    }
                }
            )
        }
        collectAdapter.onItemLongClickListener =
            BaseQuickAdapter.OnItemLongClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
                val vodInfo = collectAdapter.data[position]
                if (vodInfo != null) {
                    collectAdapter.remove(position)
                    RoomDataManger.deleteVodCollect(vodInfo.id)
                }
                if (collectAdapter.data.isEmpty()) {
                    mBinding.topTip.visibility = View.GONE
                    // 当删除最后一项时隐藏删除按钮
                    mBinding.titleBar.rightView.visibility = View.GONE
                    showEmpty(EmptyCollectCallback::class.java)
                }
                true
            }
        collectAdapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
                FastClickCheckUtil.check(view)
                val vodInfo = collectAdapter.data[position]
                if (vodInfo != null) {
                    if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                        val bundle = Bundle()
                        bundle.putString("id", vodInfo.vodId)
                        bundle.putString("sourceKey", vodInfo.sourceKey)
                        jumpActivity(DetailActivity::class.java, bundle)
                    } else {
//                            Intent newIntent = new Intent(mContext, SearchActivity.class);
                        val newIntent = Intent(mContext, FastSearchActivity::class.java)
                        newIntent.putExtra("title", vodInfo.name)
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(newIntent)
                    }
                }
            }
    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allVodRecord = RoomDataManger.getAllVodCollect()
            val vodInfoList: MutableList<VodCollect> = ArrayList()
            for (vodInfo in allVodRecord) {
                vodInfoList.add(vodInfo)
            }
            withContext(Dispatchers.Main) {
                collectAdapter.setNewData(vodInfoList)
                if (vodInfoList.isNotEmpty()) {
                    showSuccess()
                    mBinding.topTip.visibility = View.VISIBLE
                    // 有内容时显示删除按钮
                    mBinding.titleBar.rightView.visibility = View.VISIBLE
                }else{
                    showEmpty(EmptyCollectCallback::class.java)
                    // 空状态时隐藏删除按钮
                    mBinding.titleBar.rightView.visibility = View.GONE
                }
            }
        }
    }
}