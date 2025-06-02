package com.github.tvbox.osc.ui.model

/**
 * 工具项数据模型
 */
sealed class ToolItem {
    /**
     * 工具分类
     */
    data class Category(
        val title: String
    ) : ToolItem()

    /**
     * 工具项
     */
    data class Tool(
        val id: String,
        val title: String,
        val icon: String,
        val value: String = ""
    ) : ToolItem()
}
