package com.github.tvbox.osc.ui.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.DoubanSuggestBean
import com.github.tvbox.osc.databinding.DialogDoubanSuggestBinding
import com.github.tvbox.osc.network.repository.DoubanRepository
import com.github.tvbox.osc.ui.adapter.DoubanSuggestAdapter
import com.lxj.xpopup.core.CenterPopupView
import kotlinx.coroutines.launch

class DoubanSuggestDialog(context: Context, var list: List<DoubanSuggestBean>): CenterPopupView(context) {

    override fun getImplLayoutId(): Int {
        return R.layout.dialog_douban_suggest
    }

    private lateinit var binding: DialogDoubanSuggestBinding
    private val doubanRepository = DoubanRepository()

    override fun onCreate() {
        super.onCreate()
        binding = DialogDoubanSuggestBinding.bind(popupImplView)
        binding.rv.adapter = DoubanSuggestAdapter(list)

        // 确保context是LifecycleOwner
        if (context is LifecycleOwner) {
            // 根据豆瓣id查询分数
            for (bean in list) {
                getDetail(bean)
            }
        } else {
            LogUtils.e("DoubanSuggestDialog", "Context不是LifecycleOwner，无法启动协程")
        }
    }

    private fun getDetail(bean: DoubanSuggestBean) {
        // 使用协程和Retrofit处理网络请求
        (context as LifecycleOwner).lifecycleScope.launch {
            try {
                // 调用仓库方法获取电影详情
                val result = doubanRepository.getMovieDetail(bean.id)

                result.fold(
                    onSuccess = { movieDetail ->
                        bean.imdbRating = movieDetail.imdbRating
                        bean.doubanRating = movieDetail.doubanRating
                        bean.rottenRating = movieDetail.rottenRating
                        binding.rv.adapter?.notifyItemChanged(list.indexOf(bean))
                    },
                    onFailure = { error ->
                        LogUtils.e("DoubanSuggestDialog", "获取电影详情失败: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                LogUtils.e("DoubanSuggestDialog", "获取电影详情过程中发生错误: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        // 已迁移到Retrofit，不再需要取消OkGo请求
        super.onDestroy()
    }
}