package com.github.tvbox.osc.util

import java.util.*

/**
 * 温暖话语工具类
 * 根据当前时间返回不同的问候语
 */
object WarmGreetingUtil {

    /**
     * 获取当前时间对应的温暖话语
     */
    fun getCurrentGreeting(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 6..11 -> getMorningGreeting()
            in 12..13 -> getNoonGreeting()
            in 14..17 -> getAfternoonGreeting()
            in 18..21 -> getEveningGreeting()
            else -> getNightGreeting()
        }
    }

    /**
     * 早晨问候语 (6:00-11:59)
     */
    private fun getMorningGreeting(): String {
        val greetings = arrayOf(
            "早上好！新的一天，新的开始 ☀️",
            "美好的一天从现在开始",
            "愿你今天心情如阳光般灿烂",
            "早安！今天也要元气满满哦",
            "晨光正好，开启美好的一天",
            "早起的鸟儿有虫吃，早起的你有好剧看"
        )
        return greetings.random()
    }

    /**
     * 中午问候语 (12:00-13:59)
     */
    private fun getNoonGreeting(): String {
        val greetings = arrayOf(
            "中午好！记得按时吃饭 🍽️",
            "午休时光，放松一下吧",
            "阳光正好，心情也要美美的",
            "午餐时间，来看个剧配饭吧",
            "正午时分，享受悠闲时光"
        )
        return greetings.random()
    }

    /**
     * 下午问候语 (14:00-17:59)
     */
    private fun getAfternoonGreeting(): String {
        val greetings = arrayOf(
            "下午好！工作辛苦了 ☕",
            "下午茶时间，来看个剧放松一下",
            "午后时光，最适合追剧了",
            "下午的阳光很温暖，心情也要暖暖的",
            "忙碌的下午，给自己一点娱乐时间",
            "下午时光，让好剧陪伴你"
        )
        return greetings.random()
    }

    /**
     * 晚上问候语 (18:00-21:59)
     */
    private fun getEveningGreeting(): String {
        val greetings = arrayOf(
            "晚上好！忙碌了一天，该休息了 🌙",
            "夜幕降临，享受悠闲时光",
            "晚餐后的娱乐时间开始啦",
            "夕阳西下，最美的时光开始了",
            "晚风习习，来看个好剧吧",
            "夜色温柔，愿你心情也温柔"
        )
        return greetings.random()
    }

    /**
     * 深夜问候语 (22:00-5:59)
     */
    private fun getNightGreeting(): String {
        val greetings = arrayOf(
            "夜深了，注意休息哦 😴",
            "熬夜追剧虽爽，但要注意身体",
            "晚安！愿你有个好梦",
            "深夜时光，记得早点休息",
            "夜猫子，看完这集就睡觉吧",
            "星空璀璨，愿你的梦也璀璨"
        )
        return greetings.random()
    }

    /**
     * 获取特殊节日问候语（可扩展）
     */
    fun getSpecialGreeting(): String? {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return when {
            month == 1 && day == 1 -> "新年快乐！愿新的一年里好剧相伴 🎉"
            month == 2 && day == 14 -> "情人节快乐！和心爱的人一起看剧吧 💕"
            month == 5 && day == 1 -> "劳动节快乐！放假的日子最适合追剧了 🎊"
            month == 10 && day == 1 -> "国庆节快乐！假期愉快，好剧不断 🇨🇳"
            month == 12 && day == 25 -> "圣诞快乐！温馨的节日，温馨的剧集 🎄"
            else -> null
        }
    }
}
