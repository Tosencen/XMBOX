package com.github.tvbox.osc.ui.dialog

import android.content.Context
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ScreenUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.DoubanSuggestBean
import com.github.tvbox.osc.databinding.DialogDoubanSuggestBinding
import com.github.tvbox.osc.ui.adapter.DoubanSuggestAdapter
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.core.CenterPopupView
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.Response

class DoubanSuggestDialog(context: Context,var list: List<DoubanSuggestBean>):CenterPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.dialog_douban_suggest
    }

    private lateinit var binding:DialogDoubanSuggestBinding

    override fun onCreate() {
        super.onCreate()
        binding = DialogDoubanSuggestBinding.bind(popupImplView)
        binding.rv.adapter = DoubanSuggestAdapter(list)

        //根据豆瓣id查询分数
        for (bean in list) {
            getDetail(bean)
        }
        getDetail(list[0])
    }
    private fun getDetail(bean: DoubanSuggestBean) {

        OkGo.get<String>("https://api.wmdb.tv/movie/api?id="+bean.id)
            .tag("douban")
            .execute(object : StringCallback(){
                override fun onSuccess(response: Response<String>?) {
                    runCatching {
                        val detail = JsonParser.parseString(response?.body()).asJsonObject
                        LogUtils.json(detail)
                        bean.imdbRating = detail.get("imdbRating").asString
                        bean.doubanRating = detail.get("doubanRating").asString
                        bean.rottenRating = detail.get("rottenRating").asString
                        binding.rv.adapter?.notifyItemChanged(list.indexOf(bean))
                    }
                }
            })
    }

    override fun onDestroy() {
        OkGo.getInstance().cancelTag("douban")
        super.onDestroy()
    }
}