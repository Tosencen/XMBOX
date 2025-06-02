package com.github.tvbox.osc.ui.model

/**
 * 设置项数据模型
 */
sealed class SettingItem {
    /**
     * 设置分类
     */
    data class Category(
        val title: String
    ) : SettingItem()

    /**
     * 普通设置项
     */
    data class Regular(
        val id: String,
        val title: String,
        val value: String = "",
        val iconResId: Int = 0,
        val description: String = ""
    ) : SettingItem()

    /**
     * 开关设置项
     */
    data class Switch(
        val id: String,
        val title: String,
        val isChecked: Boolean = false,
        val iconResId: Int = 0,
        val description: String = ""
    ) : SettingItem()

    /**
     * 按钮设置项
     */
    data class Button(
        val id: String,
        val title: String,
        val buttonText: String,
        val iconResId: Int = 0,
        val value: String = "",
        val description: String = ""
    ) : SettingItem()
}
