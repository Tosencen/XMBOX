package com.github.tvbox.osc.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.xmbox.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 垂直方向的TabLayout
 */
public class VerticalTabLayout extends LinearLayout {

    private List<String> tabTitles = new ArrayList<>();
    private List<TextView> tabViews = new ArrayList<>();
    private int selectedPosition = 0;
    private ViewPager2 viewPager;

    public VerticalTabLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public VerticalTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VerticalTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
    }

    public void setupWithViewPager(ViewPager2 viewPager) {
        this.viewPager = viewPager;
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectTab(position);
            }
        });
    }

    public void removeAllTabs() {
        tabTitles.clear();
        tabViews.clear();
        removeAllViews();
    }

    public void addTab(String title) {
        if (title != null) {
            addTabInternal(title);
        }
    }

    private void addTabInternal(String title) {
        tabTitles.add(title);

        TextView textView = new TextView(getContext());
        textView.setText(title);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(20, 30, 20, 30);
        textView.setTextSize(14);
        textView.setTextColor(getResources().getColor(R.color.gray_999));

        final int position = tabTitles.size() - 1;
        textView.setOnClickListener(v -> {
            if (viewPager != null) {
                viewPager.setCurrentItem(position);
            }
            selectTab(position);
        });

        addView(textView);
        tabViews.add(textView);

        if (position == 0) {
            selectTab(0);
        }
    }

    public void selectTab(int position) {
        if (position < 0 || position >= tabViews.size()) {
            return;
        }

        for (int i = 0; i < tabViews.size(); i++) {
            TextView textView = tabViews.get(i);
            if (i == position) {
                textView.setTextColor(getResources().getColor(R.color.colorPrimary));
                textView.setSelected(true);
            } else {
                textView.setTextColor(getResources().getColor(R.color.gray_999));
                textView.setSelected(false);
            }
        }

        selectedPosition = position;
    }

    public int getSelectedTabPosition() {
        return selectedPosition;
    }
}
