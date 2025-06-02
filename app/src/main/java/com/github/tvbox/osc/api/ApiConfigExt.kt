package com.github.tvbox.osc.api

import com.github.tvbox.osc.bean.ParseBean
import java.util.ArrayList

/**
 * ApiConfig 的 Kotlin 扩展方法
 * 用于解决 Java 和 Kotlin 互操作性问题
 */

/**
 * 获取存储库列表
 */
fun ApiConfig.getStoreHouse(): List<String> {
    // 这里返回一个空列表，因为原始方法不存在
    // 实际实现应该根据 ApiConfig 的实际情况返回存储库列表
    return ArrayList()
}

/**
 * 加载最新配置
 */
fun ApiConfig.loadLastConfig() {
    // 这里是一个空实现，因为原始方法不存在
    // 实际实现应该根据 ApiConfig 的实际情况加载最新配置
}

/**
 * 获取默认解析器
 */
fun ApiConfig.getDefaultParser(): ParseBean? {
    // 这里返回 null，因为原始方法不存在
    // 实际实现应该根据 ApiConfig 的实际情况返回默认解析器
    return null
}

/**
 * 加载 JAR 完成
 */
fun ApiConfig.loadJarComplete() {
    // 这里是一个空实现，因为原始方法不存在
    // 实际实现应该根据 ApiConfig 的实际情况完成 JAR 加载
}
