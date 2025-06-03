package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xmbox.app.R;
import com.github.tvbox.osc.ui.adapter.MenuAdapter;
import com.lxj.xpopup.core.AttachPopupView;

import java.util.List;

public class MenuDialog extends AttachPopupView {
    
    private List<MenuAdapter.MenuItem> menuItems;
    private MenuAdapter.OnItemClickListener onItemClickListener;
    
    public MenuDialog(@NonNull Context context, List<MenuAdapter.MenuItem> menuItems) {
        super(context);
        this.menuItems = menuItems;
    }
    
    @Override
    protected int getImplLayoutId() {
        return R.layout.popup_menu_m3;
    }
    
    @Override
    protected void onCreate() {
        super.onCreate();
        
        RecyclerView rvMenu = findViewById(R.id.rv_menu);
        rvMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        
        MenuAdapter adapter = new MenuAdapter(menuItems);
        rvMenu.setAdapter(adapter);
        
        adapter.setOnItemClickListener(position -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
                dismiss();
            }
        });
    }
    
    public void setOnItemClickListener(MenuAdapter.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
