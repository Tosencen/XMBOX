package com.fongmi.android.tv;

import android.content.Context;

import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 1;
    }

    public static int getColumn(Context context) {
        // 平板版本：根据设置调整列数
        // size 越大，列数越少，图片越大
        // size=0(特大): 7/6列, size=1(大): 6/5列, size=2(中): 5/4列, size=3(小): 4/3列, size=4(特小): 3/2列
        int count = ResUtil.isLand(context) ? 7 : 6;
        return Math.abs(Setting.getSize() - count);
    }

    public static int getColumn(Context context, Style style) {
        return style.isLand() ? getColumn(context) - 1 : getColumn(context);
    }

    public static int[] getSpec(Context context) {
        return getSpec(context, Style.rect());
    }

    public static int[] getSpec(Context context, Style style) {
        int column = getColumn(context, style);
        // 简化计算：只考虑 item 的 margin
        // 每个 item 有 20dp margin，左右各 20dp
        // 总间距 = column * 40dp (每个item左右margin)
        int space = ResUtil.dp2px(column * 40);
        if (style.isOval()) space += ResUtil.dp2px(column * 40);
        
        android.util.Log.d("Product", "=== getSpec Debug ===");
        android.util.Log.d("Product", "isLand: " + ResUtil.isLand(context));
        android.util.Log.d("Product", "column: " + column);
        android.util.Log.d("Product", "item margin per item: 40dp");
        android.util.Log.d("Product", "total space (dp): " + (column * 40));
        android.util.Log.d("Product", "total space (px): " + space);
        android.util.Log.d("Product", "screenWidth (px): " + ResUtil.getScreenWidth(context));
        
        return getSpec(context, space, column, style);
    }

    public static int[] getSpec(Context context, int space, int column) {
        return getSpec(context, space, column, Style.rect());
    }

    private static int[] getSpec(Context context, int space, int column, Style style) {
        int base = ResUtil.getScreenWidth(context) - space;
        int width = base / column;
        int height = (int) (width / style.getRatio());
        
        android.util.Log.d("Product", "base (screenWidth - space): " + base);
        android.util.Log.d("Product", "item width (px): " + width);
        android.util.Log.d("Product", "item height (px): " + height);
        android.util.Log.d("Product", "ratio: " + style.getRatio());
        android.util.Log.d("Product", "==================");
        
        return new int[]{width, height};
    }

    public static int getEms() {
        return Math.min(ResUtil.getScreenWidth() / ResUtil.sp2px(20), 25);
    }
}
