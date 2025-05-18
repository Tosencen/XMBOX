package com.github.tvbox.osc.network.repository

import android.text.TextUtils
import android.util.Log
import com.github.tvbox.osc.bean.Subtitle
import com.github.tvbox.osc.network.RetrofitClient
import com.github.tvbox.osc.network.api.SubtitleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * 字幕仓库
 * 负责处理字幕相关的网络请求和数据处理
 */
class SubtitleRepository {
    private val TAG = "SubtitleRepository"
    private val service = RetrofitClient.createService(SubtitleService::class.java)
    
    // 字幕API基础URL
    private val ASSRT_BASE_URL = "https://secure.assrt.net/"
    
    // 正则表达式
    private val regexShooterFileOnclick = Pattern.compile("onthefly\\(\"(\\d+)\",\"(\\d+)\",\"([\\s\\S]*)\"\\)")
    
    // 总页数
    private var pagesTotal = -1
    
    /**
     * 搜索字幕
     * 
     * @param title 搜索关键词
     * @param page 页码
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun searchSubtitles(title: String, page: Int): Result<Pair<List<Subtitle>, Boolean>> = withContext(Dispatchers.IO) {
        try {
            // 检查是否超过总页数
            if (pagesTotal > 0 && page > pagesTotal) {
                return@withContext Result.success(Pair(emptyList(), true))
            }
            
            // 第一页时重置总页数
            if (page == 1) pagesTotal = -1
            
            val response = service.searchSubtitles(title, page = page)
            
            if (response.isSuccessful) {
                val content = response.body()
                if (content != null) {
                    val doc = Jsoup.parse(content)
                    val items = doc.select(".resultcard .sublist_box_title a.introtitle")
                    val data = ArrayList<Subtitle>()
                    
                    for (item in items) {
                        val itemTitle = item.attr("title")
                        val href = item.attr("href")
                        if (TextUtils.isEmpty(href)) continue
                        
                        val subtitle = Subtitle()
                        subtitle.name = itemTitle
                        subtitle.url = "https://assrt.net$href"
                        subtitle.isZip = true
                        data.add(subtitle)
                    }
                    
                    // 获取总页数
                    val pages = doc.select(".pagelinkcard a")
                    if (pages.isNotEmpty()) {
                        val ps = pages.last().text().split("/", limit = 2)
                        if (ps.size == 2 && !TextUtils.isEmpty(ps[1])) {
                            pagesTotal = ps[1].trim().toInt()
                        }
                    }
                    
                    return@withContext Result.success(Pair(data, true))
                } else {
                    return@withContext Result.failure(Exception("响应内容为空"))
                }
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "搜索字幕失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * 获取字幕详情
     * 
     * @param subtitle 字幕对象
     * @return 结果包装类，包含成功或失败信息
     */
    suspend fun getSubtitleDetail(subtitle: Subtitle): Result<List<Subtitle>> = withContext(Dispatchers.IO) {
        try {
            val response = service.getSubtitleDetail(subtitle.url)
            
            if (response.isSuccessful) {
                val content = response.body()
                if (content != null) {
                    val data = ArrayList<Subtitle>()
                    val doc = Jsoup.parse(content)
                    val items = doc.select("#detail-filelist .waves-effect")
                    
                    if (items.isNotEmpty()) {
                        // 压缩包里面的字幕
                        for (item in items) {
                            val onclick = item.attr("onclick")
                            if (TextUtils.isEmpty(onclick)) continue
                            
                            val matcher = regexShooterFileOnclick.matcher(onclick)
                            if (matcher.find()) {
                                val url = "https://secure.assrt.net/download/${matcher.group(1)}/-/${matcher.group(2)}/${matcher.group(3)}"
                                val one = Subtitle()
                                val name = item.selectFirst("#filelist-name")
                                one.name = name?.text() ?: matcher.group(3)
                                one.url = url
                                one.isZip = false
                                data.add(one)
                            }
                        }
                        return@withContext Result.success(data)
                    } else {
                        // 有的字幕不一定是压缩包
                        val item = doc.selectFirst(".download a#btn_download")
                        val href = item?.attr("href") ?: ""
                        if (TextUtils.isEmpty(href)) {
                            return@withContext Result.failure(Exception("未找到下载链接"))
                        }
                        
                        val h2 = href.toLowerCase()
                        if (h2.endsWith("srt") || h2.endsWith("ass") || h2.endsWith("scc") || h2.endsWith("ttml")) {
                            val url = "https://assrt.net$href"
                            val one = Subtitle()
                            val title = href.substring(href.lastIndexOf("/") + 1)
                            one.name = URLDecoder.decode(title)
                            one.url = url
                            one.isZip = false
                            data.add(one)
                            return@withContext Result.success(data)
                        } else {
                            return@withContext Result.failure(Exception("不支持的字幕格式"))
                        }
                    }
                } else {
                    return@withContext Result.failure(Exception("响应内容为空"))
                }
            } else {
                return@withContext Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取字幕详情失败: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
}
