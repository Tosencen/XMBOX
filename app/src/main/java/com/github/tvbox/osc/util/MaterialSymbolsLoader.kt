package com.github.tvbox.osc.util

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.github.tvbox.osc.R

/**
 * Material Symbols字体加载器
 *
 * 用于加载Material Symbols字体并应用到TextView上
 */
object MaterialSymbolsLoader {

    // 字体缓存
    private var materialSymbolsTypeface: Typeface? = null

    /**
     * 初始化Material Symbols字体
     *
     * @param context 上下文
     */
    @JvmStatic
    fun init(context: Context) {
        try {
            // 尝试从资源加载字体文件
            materialSymbolsTypeface = ResourcesCompat.getFont(context, R.font.material_symbols_rounded_font)

            // 如果从资源加载失败，尝试使用系统内置的Material Symbols字体
            if (materialSymbolsTypeface == null) {
                materialSymbolsTypeface = Typeface.create("Material Symbols Rounded", Typeface.NORMAL)
            }

            // 如果仍然加载失败，使用默认字体
            if (materialSymbolsTypeface == null || materialSymbolsTypeface.toString() == "null") {
                materialSymbolsTypeface = Typeface.DEFAULT
            }

            // 打印日志，查看字体加载情况
            android.util.Log.d("MaterialSymbolsLoader", "Font loaded: $materialSymbolsTypeface")
        } catch (e: Exception) {
            // 如果发生异常，使用默认字体
            materialSymbolsTypeface = Typeface.DEFAULT
            android.util.Log.e("MaterialSymbolsLoader", "Error loading font", e)
        }
    }

    /**
     * 应用Material Symbols字体到TextView
     *
     * @param textView 要应用字体的TextView
     */
    @JvmStatic
    fun apply(textView: TextView) {
        if (materialSymbolsTypeface != null) {
            textView.typeface = materialSymbolsTypeface
            android.util.Log.d("MaterialSymbolsLoader", "Applied typeface: $materialSymbolsTypeface to ${textView.id}")
        } else {
            android.util.Log.e("MaterialSymbolsLoader", "Failed to apply typeface: materialSymbolsTypeface is null")
        }
    }

    /**
     * 设置图标到TextView
     *
     * @param textView 要设置图标的TextView
     * @param icon 图标代码，如MaterialSymbols.HOME
     */
    @JvmStatic
    fun setIcon(textView: TextView, icon: String) {
        // 先设置文本，再设置字体，避免某些设备上的问题
        textView.text = icon
        if (materialSymbolsTypeface != null) {
            textView.typeface = materialSymbolsTypeface
        }

        // 打印日志，查看图标设置情况
        android.util.Log.d("MaterialSymbolsLoader", "Icon set: $icon with typeface: $materialSymbolsTypeface")
    }
}
