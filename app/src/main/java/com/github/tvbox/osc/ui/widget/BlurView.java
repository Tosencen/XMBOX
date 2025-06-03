package com.github.tvbox.osc.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 真正的磨砂模糊效果View
 * 可以模糊显示底层内容
 */
public class BlurView extends FrameLayout {

    private static final String TAG = "BlurView";
    private static final float DEFAULT_BLUR_RADIUS = 15f;
    private static final float DEFAULT_SCALE_FACTOR = 0.25f;

    private float blurRadius = DEFAULT_BLUR_RADIUS;
    private float scaleFactor = DEFAULT_SCALE_FACTOR;
    private Paint paint;
    private Bitmap blurredBitmap;
    private Canvas blurredCanvas;
    private RenderScript renderScript;
    private ScriptIntrinsicBlur blurScript;
    private Allocation inAllocation;
    private Allocation outAllocation;
    private boolean isBlurEnabled = true;

    public BlurView(Context context) {
        super(context);
        init();
    }

    public BlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setWillNotDraw(false);

        // 初始化RenderScript（用于Android 12以下）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            try {
                renderScript = RenderScript.create(getContext());
                blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            } catch (Exception e) {
                android.util.Log.w(TAG, "RenderScript初始化失败，将使用软件模糊", e);
            }
        }
    }

    /**
     * 设置模糊半径
     * @param radius 模糊半径 (1-25)
     */
    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(1f, Math.min(25f, radius));
        invalidate();
    }

    /**
     * 设置缩放因子（用于性能优化）
     * @param factor 缩放因子 (0.1-1.0)
     */
    public void setScaleFactor(float factor) {
        this.scaleFactor = Math.max(0.1f, Math.min(1.0f, factor));
        invalidate();
    }

    /**
     * 启用或禁用模糊效果
     */
    public void setBlurEnabled(boolean enabled) {
        this.isBlurEnabled = enabled;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isBlurEnabled) {
            super.onDraw(canvas);
            return;
        }

        // 获取背景内容并应用模糊
        Bitmap blurred = getBlurredBackground();
        if (blurred != null) {
            canvas.drawBitmap(blurred, 0, 0, paint);
        }

        super.onDraw(canvas);
    }

    private Bitmap getBlurredBackground() {
        try {
            // 获取父视图
            ViewGroup parent = (ViewGroup) getParent();
            if (parent == null) return null;

            // 计算缩放后的尺寸
            int scaledWidth = Math.max(1, (int) (getWidth() * scaleFactor));
            int scaledHeight = Math.max(1, (int) (getHeight() * scaleFactor));

            // 创建或重用bitmap
            if (blurredBitmap == null ||
                blurredBitmap.getWidth() != scaledWidth ||
                blurredBitmap.getHeight() != scaledHeight) {

                if (blurredBitmap != null && !blurredBitmap.isRecycled()) {
                    blurredBitmap.recycle();
                }

                blurredBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                blurredCanvas = new Canvas(blurredBitmap);
            }

            // 清空画布
            blurredCanvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            // 保存画布状态
            blurredCanvas.save();

            // 缩放和平移画布
            blurredCanvas.scale(scaleFactor, scaleFactor);
            blurredCanvas.translate(-getLeft(), -getTop());

            // 绘制父视图的背景内容（排除当前BlurView）
            drawParentBackground(blurredCanvas, parent);

            // 恢复画布状态
            blurredCanvas.restore();

            // 应用模糊效果
            return applyBlur(blurredBitmap);

        } catch (Exception e) {
            android.util.Log.w(TAG, "模糊处理失败", e);
            return null;
        }
    }

    private void drawParentBackground(Canvas canvas, ViewGroup parent) {
        // 绘制父视图的背景
        if (parent.getBackground() != null) {
            parent.getBackground().draw(canvas);
        }

        // 绘制父视图中在当前BlurView之前的子视图
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == this) {
                break; // 遇到自己就停止
            }

            if (child.getVisibility() == View.VISIBLE) {
                canvas.save();
                canvas.translate(child.getLeft(), child.getTop());
                child.draw(canvas);
                canvas.restore();
            }
        }
    }

    private Bitmap applyBlur(Bitmap original) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 使用RenderEffect
            return applyRenderEffectBlur(original);
        } else {
            // Android 12以下使用RenderScript
            return applyRenderScriptBlur(original);
        }
    }

    private Bitmap applyRenderEffectBlur(Bitmap original) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Android 12+ 的RenderEffect需要通过View来实现，这里直接使用软件模糊
                return applySoftwareBlur(original);
            } catch (Exception e) {
                android.util.Log.w(TAG, "RenderEffect模糊失败，使用软件模糊", e);
                return applySoftwareBlur(original);
            }
        }
        return original;
    }

    private Bitmap applyRenderScriptBlur(Bitmap original) {
        if (renderScript == null || blurScript == null) {
            return applySoftwareBlur(original);
        }

        try {
            // 创建Allocation
            if (inAllocation != null) {
                inAllocation.destroy();
            }
            if (outAllocation != null) {
                outAllocation.destroy();
            }

            inAllocation = Allocation.createFromBitmap(renderScript, original);
            outAllocation = Allocation.createTyped(renderScript, inAllocation.getType());

            // 设置模糊半径
            blurScript.setRadius(blurRadius);
            blurScript.setInput(inAllocation);
            blurScript.forEach(outAllocation);

            // 复制结果
            outAllocation.copyTo(original);

            return original;
        } catch (Exception e) {
            android.util.Log.w(TAG, "RenderScript模糊失败，使用软件模糊", e);
            return applySoftwareBlur(original);
        }
    }

    private Bitmap applySoftwareBlur(Bitmap original) {
        // 高质量软件模糊实现 - 模拟毛玻璃效果
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            int[] pixels = new int[width * height];
            original.getPixels(pixels, 0, width, 0, 0, width, height);

            // 增强的高斯模糊，模拟毛玻璃效果
            int radius = (int) Math.max(2, blurRadius * scaleFactor);

            // 多次应用不同强度的模糊，营造毛玻璃层次感
            for (int i = 0; i < 2; i++) {
                pixels = gaussianBlur(pixels, width, height, radius);
            }

            // 添加轻微的噪点，增强毛玻璃质感
            pixels = addGlassTexture(pixels, width, height);

            Bitmap result = Bitmap.createBitmap(width, height, original.getConfig());
            result.setPixels(pixels, 0, width, 0, 0, width, height);
            return result;
        } catch (Exception e) {
            android.util.Log.w(TAG, "软件模糊失败", e);
            return original;
        }
    }

    private int[] boxBlur(int[] pixels, int width, int height, int radius) {
        int[] result = new int[pixels.length];
        int diameter = radius * 2 + 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, a = 0;
                int count = 0;

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = Math.max(0, Math.min(width - 1, x + dx));
                        int ny = Math.max(0, Math.min(height - 1, y + dy));
                        int pixel = pixels[ny * width + nx];

                        a += (pixel >> 24) & 0xFF;
                        r += (pixel >> 16) & 0xFF;
                        g += (pixel >> 8) & 0xFF;
                        b += pixel & 0xFF;
                        count++;
                    }
                }

                result[y * width + x] = ((a / count) << 24) |
                                       ((r / count) << 16) |
                                       ((g / count) << 8) |
                                       (b / count);
            }
        }

        return result;
    }

    /**
     * 高斯模糊算法 - 更好的毛玻璃效果
     */
    private int[] gaussianBlur(int[] pixels, int width, int height, int radius) {
        int[] result = new int[pixels.length];

        // 水平方向模糊
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, a = 0;
                int count = 0;

                for (int dx = -radius; dx <= radius; dx++) {
                    int nx = Math.max(0, Math.min(width - 1, x + dx));
                    int pixel = pixels[y * width + nx];

                    // 高斯权重（简化版）
                    float weight = 1.0f - Math.abs(dx) / (float)(radius + 1);

                    a += ((pixel >> 24) & 0xFF) * weight;
                    r += ((pixel >> 16) & 0xFF) * weight;
                    g += ((pixel >> 8) & 0xFF) * weight;
                    b += (pixel & 0xFF) * weight;
                    count += weight;
                }

                result[y * width + x] = ((int)(a / count) << 24) |
                                       ((int)(r / count) << 16) |
                                       ((int)(g / count) << 8) |
                                       (int)(b / count);
            }
        }

        // 垂直方向模糊
        int[] finalResult = new int[pixels.length];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int r = 0, g = 0, b = 0, a = 0;
                int count = 0;

                for (int dy = -radius; dy <= radius; dy++) {
                    int ny = Math.max(0, Math.min(height - 1, y + dy));
                    int pixel = result[ny * width + x];

                    // 高斯权重（简化版）
                    float weight = 1.0f - Math.abs(dy) / (float)(radius + 1);

                    a += ((pixel >> 24) & 0xFF) * weight;
                    r += ((pixel >> 16) & 0xFF) * weight;
                    g += ((pixel >> 8) & 0xFF) * weight;
                    b += (pixel & 0xFF) * weight;
                    count += weight;
                }

                finalResult[y * width + x] = ((int)(a / count) << 24) |
                                            ((int)(r / count) << 16) |
                                            ((int)(g / count) << 8) |
                                            (int)(b / count);
            }
        }

        return finalResult;
    }

    /**
     * 添加毛玻璃纹理效果
     */
    private int[] addGlassTexture(int[] pixels, int width, int height) {
        int[] result = new int[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int a = (pixel >> 24) & 0xFF;
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            // 添加轻微的随机噪点，模拟毛玻璃纹理
            int x = i % width;
            int y = i / width;

            // 使用位置生成伪随机噪点
            int noise = ((x * 7 + y * 13) % 32) - 16; // -16到15的噪点

            // 应用轻微的噪点
            r = Math.max(0, Math.min(255, r + noise / 8));
            g = Math.max(0, Math.min(255, g + noise / 8));
            b = Math.max(0, Math.min(255, b + noise / 8));

            result[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        return result;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cleanup();
    }

    private void cleanup() {
        if (blurredBitmap != null && !blurredBitmap.isRecycled()) {
            blurredBitmap.recycle();
            blurredBitmap = null;
        }

        if (inAllocation != null) {
            inAllocation.destroy();
            inAllocation = null;
        }

        if (outAllocation != null) {
            outAllocation.destroy();
            outAllocation = null;
        }

        if (blurScript != null) {
            blurScript.destroy();
            blurScript = null;
        }

        if (renderScript != null) {
            renderScript.destroy();
            renderScript = null;
        }
    }
}
