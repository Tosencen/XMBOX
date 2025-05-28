package com.github.tvbox.osc.ui.activity

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xmbox.app.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.ui.adapter.ToolsAdapter
import com.github.tvbox.osc.ui.model.ToolItem
import com.github.tvbox.osc.util.MaterialSymbols
import com.github.tvbox.osc.util.MaterialSymbolsLoader
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar

/**
 * 开发者工具页面
 */
class DevToolsActivity : BaseActivity() {

    private lateinit var titleBar: TitleBar
    private lateinit var rvTools: RecyclerView
    private lateinit var adapter: ToolsAdapter
    private val toolItems = mutableListOf<ToolItem>()

    override fun getLayoutResID(): Int {
        return R.layout.activity_tools_m3
    }

    override fun init() {
        initView()
        initData()
    }

    private fun initView() {
        titleBar = findViewById(R.id.title_bar)
        rvTools = findViewById(R.id.rv_tools)

        // 自定义返回图标，使用Material Symbols字体
        val leftView = titleBar.getLeftView()
        if (leftView is TextView) {
            leftView.text = getString(R.string.ms_arrow_back)
            // 应用Material Symbols字体
            MaterialSymbolsLoader.apply(leftView)
            // 设置正确的大小和样式
            leftView.textSize = 24f  // 24sp是M3规范的标准图标大小
            leftView.setTextColor(getColor(R.color.md3_on_surface))
        }

        titleBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                onBackPressed()
            }

            override fun onTitleClick(titleBar: TitleBar) {
                // 标题点击
            }

            override fun onRightClick(titleBar: TitleBar) {
                // 右侧点击
            }
        })

        // 设置RecyclerView
        rvTools.layoutManager = LinearLayoutManager(this)
        adapter = ToolsAdapter(toolItems, object : ToolsAdapter.OnItemClickListener {
            override fun onItemClick(item: ToolItem.Tool) {
                handleToolItemClick(item)
            }
        })
        rvTools.adapter = adapter
    }

    private fun initData() {
        // 添加工具项
        toolItems.clear()

        // 请求相关
        toolItems.add(ToolItem.Category("请求"))
        toolItems.add(ToolItem.Tool(
            id = "request_log",
            title = "查看最近请求记录",
            icon = MaterialSymbols.HISTORY
        ))

        // 连接相关
        toolItems.add(ToolItem.Category("连接"))
        toolItems.add(ToolItem.Tool(
            id = "connection_data",
            title = "查看当前连接数据",
            icon = MaterialSymbols.WIFI
        ))

        // 资源相关
        toolItems.add(ToolItem.Category("资源"))
        toolItems.add(ToolItem.Tool(
            id = "external_resources",
            title = "外部资源相关信息",
            icon = MaterialSymbols.FOLDER
        ))

        // 设置相关
        toolItems.add(ToolItem.Category("设置"))
        toolItems.add(ToolItem.Tool(
            id = "language",
            title = "语言",
            value = "默认",
            icon = MaterialSymbols.LANGUAGE
        ))

        toolItems.add(ToolItem.Tool(
            id = "theme",
            title = "主题",
            value = "跟随系统",
            icon = MaterialSymbols.SETTINGS_BRIGHTNESS
        ))

        toolItems.add(ToolItem.Tool(
            id = "backup",
            title = "备份与恢复",
            icon = MaterialSymbols.SETTINGS_BACKUP_RESTORE
        ))

        toolItems.add(ToolItem.Tool(
            id = "shortcuts",
            title = "快捷键管理",
            icon = MaterialSymbols.SETTINGS_APPLICATIONS
        ))

        toolItems.add(ToolItem.Tool(
            id = "basic_config",
            title = "基本配置",
            icon = MaterialSymbols.SETTINGS
        ))

        // 通知适配器数据已更新
        adapter.notifyDataSetChanged()
    }

    private fun handleToolItemClick(item: ToolItem.Tool) {
        // 处理工具项点击
        when (item.id) {
            "request_log" -> {
                // 查看请求记录
            }
            "connection_data" -> {
                // 查看连接数据
            }
            "external_resources" -> {
                // 查看外部资源
            }
            "language" -> {
                // 语言设置
            }
            "theme" -> {
                // 主题设置
            }
            "backup" -> {
                // 备份与恢复
            }
            "shortcuts" -> {
                // 快捷键管理
            }
            "basic_config" -> {
                // 基本配置
            }
        }
    }
}
