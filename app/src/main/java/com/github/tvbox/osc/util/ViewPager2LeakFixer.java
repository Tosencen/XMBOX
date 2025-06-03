package com.github.tvbox.osc.util;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * ViewPager2内存泄漏修复工具
 * 专门用于修复ViewPager2及其内部RecyclerView的内存泄漏问题
 */
public class ViewPager2LeakFixer {
    private static final String TAG = "ViewPager2LeakFixer";

    /**
     * 修复ViewPager2相关的内存泄漏
     */
    public static void fixViewPager2Leak(ViewPager2 viewPager2) {
        if (viewPager2 == null) {
            return;
        }

        try {
            Log.i(TAG, "开始修复ViewPager2内存泄漏");

            // 1. 清理ViewPager2的适配器
            clearViewPager2Adapter(viewPager2);

            // 2. 清理ViewPager2内部的RecyclerView
            clearInternalRecyclerView(viewPager2);

            // 3. 清理ViewPager2的回调
            clearViewPager2Callbacks(viewPager2);

            // 4. 清理ViewPager2的内部引用
            clearViewPager2InternalReferences(viewPager2);

            // 5. 记录修复统计
            MemoryLeakReporter.recordViewPager2Fix();

            Log.i(TAG, "ViewPager2内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复ViewPager2内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 清理ViewPager2的适配器
     */
    private static void clearViewPager2Adapter(ViewPager2 viewPager2) {
        try {
            // 清理适配器
            viewPager2.setAdapter(null);
            Log.d(TAG, "ViewPager2适配器已清理");
        } catch (Exception e) {
            Log.w(TAG, "清理ViewPager2适配器失败: " + e.getMessage());
        }
    }

    /**
     * 清理ViewPager2内部的RecyclerView
     */
    private static void clearInternalRecyclerView(ViewPager2 viewPager2) {
        try {
            // 获取ViewPager2内部的RecyclerView
            RecyclerView recyclerView = null;
            
            // 方法1：通过子视图获取
            if (viewPager2.getChildCount() > 0) {
                View child = viewPager2.getChildAt(0);
                if (child instanceof RecyclerView) {
                    recyclerView = (RecyclerView) child;
                }
            }

            // 方法2：通过反射获取
            if (recyclerView == null) {
                try {
                    Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
                    recyclerViewField.setAccessible(true);
                    recyclerView = (RecyclerView) recyclerViewField.get(viewPager2);
                } catch (Exception e) {
                    Log.w(TAG, "通过反射获取RecyclerView失败: " + e.getMessage());
                }
            }

            if (recyclerView != null) {
                // 停止所有动画
                if (recyclerView.getItemAnimator() != null) {
                    recyclerView.getItemAnimator().endAnimations();
                    recyclerView.setItemAnimator(null);
                }

                // 清理适配器
                recyclerView.setAdapter(null);

                // 清理布局管理器
                recyclerView.setLayoutManager(null);

                // 清理所有监听器
                recyclerView.clearOnScrollListeners();
                recyclerView.clearOnChildAttachStateChangeListeners();

                // 清理RecyclerView的缓存和视图池
                recyclerView.getRecycledViewPool().clear();
                recyclerView.setRecycledViewPool(null);

                // 清理触摸监听器
                recyclerView.setOnTouchListener(null);

                // 移除所有子视图
                recyclerView.removeAllViews();

                Log.d(TAG, "ViewPager2内部RecyclerView已彻底清理");
            }
        } catch (Exception e) {
            Log.w(TAG, "清理ViewPager2内部RecyclerView失败: " + e.getMessage());
        }
    }

    /**
     * 清理ViewPager2的回调
     */
    private static void clearViewPager2Callbacks(ViewPager2 viewPager2) {
        try {
            // 使用反射清理OnPageChangeCallback列表
            Field callbacksField = ViewPager2.class.getDeclaredField("mExternalPageChangeCallbacks");
            callbacksField.setAccessible(true);
            Object callbacks = callbacksField.get(viewPager2);
            
            if (callbacks instanceof List) {
                ((List<?>) callbacks).clear();
                Log.d(TAG, "ViewPager2回调列表已清理");
            }
        } catch (Exception e) {
            Log.w(TAG, "清理ViewPager2回调失败: " + e.getMessage());
        }
    }

    /**
     * 清理ViewPager2的内部引用
     */
    private static void clearViewPager2InternalReferences(ViewPager2 viewPager2) {
        try {
            Class<?> viewPager2Class = viewPager2.getClass();
            Field[] fields = viewPager2Class.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(viewPager2);
                String fieldName = field.getName();

                // 清理Fragment相关引用
                if (value instanceof Fragment) {
                    field.set(viewPager2, null);
                    Log.d(TAG, "清理ViewPager2 Fragment引用: " + fieldName);
                }
                // 清理Adapter相关引用
                else if (fieldName.contains("Adapter") || fieldName.contains("adapter")) {
                    field.set(viewPager2, null);
                    Log.d(TAG, "清理ViewPager2 Adapter引用: " + fieldName);
                }
                // 清理Callback相关引用
                else if (fieldName.contains("Callback") || fieldName.contains("callback")) {
                    field.set(viewPager2, null);
                    Log.d(TAG, "清理ViewPager2 Callback引用: " + fieldName);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理ViewPager2内部引用失败: " + e.getMessage());
        }
    }

    /**
     * 修复TabLayoutMediator相关的内存泄漏
     */
    public static void fixTabLayoutMediatorLeak(TabLayoutMediator mediator) {
        if (mediator == null) {
            return;
        }

        try {
            Log.i(TAG, "开始修复TabLayoutMediator内存泄漏");

            // 分离TabLayoutMediator
            mediator.detach();

            // 清理TabLayoutMediator的内部引用
            clearTabLayoutMediatorReferences(mediator);

            Log.i(TAG, "TabLayoutMediator内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复TabLayoutMediator内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 清理TabLayoutMediator的内部引用
     */
    private static void clearTabLayoutMediatorReferences(TabLayoutMediator mediator) {
        try {
            Class<?> mediatorClass = mediator.getClass();
            Field[] fields = mediatorClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(mediator);
                String fieldName = field.getName();

                // 清理TabLayout引用
                if (value instanceof TabLayout) {
                    field.set(mediator, null);
                    Log.d(TAG, "清理TabLayoutMediator TabLayout引用: " + fieldName);
                }
                // 清理ViewPager2引用
                else if (value instanceof ViewPager2) {
                    field.set(mediator, null);
                    Log.d(TAG, "清理TabLayoutMediator ViewPager2引用: " + fieldName);
                }
                // 清理Callback引用
                else if (fieldName.contains("Callback") || fieldName.contains("callback")) {
                    field.set(mediator, null);
                    Log.d(TAG, "清理TabLayoutMediator Callback引用: " + fieldName);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理TabLayoutMediator内部引用失败: " + e.getMessage());
        }
    }

    /**
     * 修复TabLayout相关的内存泄漏
     */
    public static void fixTabLayoutLeak(TabLayout tabLayout) {
        if (tabLayout == null) {
            return;
        }

        try {
            Log.i(TAG, "开始修复TabLayout内存泄漏");

            // 清理所有Tab
            tabLayout.removeAllTabs();

            // 清理所有监听器
            tabLayout.clearOnTabSelectedListeners();

            // 清理TabLayout的内部引用
            clearTabLayoutReferences(tabLayout);

            Log.i(TAG, "TabLayout内存泄漏修复完成");
        } catch (Exception e) {
            Log.e(TAG, "修复TabLayout内存泄漏失败: " + e.getMessage());
        }
    }

    /**
     * 清理TabLayout的内部引用
     */
    private static void clearTabLayoutReferences(TabLayout tabLayout) {
        try {
            Class<?> tabLayoutClass = tabLayout.getClass();
            Field[] fields = tabLayoutClass.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(tabLayout);
                String fieldName = field.getName();

                // 清理ViewPager引用
                if (fieldName.contains("ViewPager") || fieldName.contains("viewPager")) {
                    field.set(tabLayout, null);
                    Log.d(TAG, "清理TabLayout ViewPager引用: " + fieldName);
                }
                // 清理Listener引用
                else if (fieldName.contains("Listener") || fieldName.contains("listener")) {
                    field.set(tabLayout, null);
                    Log.d(TAG, "清理TabLayout Listener引用: " + fieldName);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "清理TabLayout内部引用失败: " + e.getMessage());
        }
    }

    /**
     * 全面修复ViewPager2相关的所有内存泄漏
     */
    public static void comprehensiveFixViewPager2Leaks(ViewPager2 viewPager2, TabLayout tabLayout, TabLayoutMediator mediator) {
        try {
            Log.i(TAG, "开始全面修复ViewPager2相关内存泄漏");

            // 修复TabLayoutMediator
            if (mediator != null) {
                fixTabLayoutMediatorLeak(mediator);
            }

            // 修复TabLayout
            if (tabLayout != null) {
                fixTabLayoutLeak(tabLayout);
            }

            // 修复ViewPager2
            if (viewPager2 != null) {
                fixViewPager2Leak(viewPager2);
            }

            Log.i(TAG, "全面修复ViewPager2相关内存泄漏完成");
        } catch (Exception e) {
            Log.e(TAG, "全面修复ViewPager2相关内存泄漏失败: " + e.getMessage());
        }
    }
}
