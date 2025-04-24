package com.github.tvbox.osc.ui.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.util.SearchHelper;
import com.google.android.material.radiobutton.MaterialRadioButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchSourceAdapter extends ListAdapter<SourceBean, SearchSourceAdapter.ViewHolder> {

    public SearchSourceAdapter(DiffUtil.ItemCallback<SourceBean> diffCallback) {
        super(diffCallback);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_search_source_md3_no_bg, parent, false));
    }

    private void setCheckedSource(HashMap<String, String> checkedSources) {
        mCheckedSources = checkedSources;
    }

    private ArrayList<SourceBean> data = new ArrayList<>();
    public HashMap<String, String> mCheckedSources = new HashMap<>();

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<SourceBean> newData, HashMap<String, String> checkedSources) {
        data.clear();
        data.addAll(newData);
        setCheckedSource(checkedSources);
        notifyDataSetChanged();
    }

    public void setMCheckedSources() {
        SearchHelper.putCheckedSources(mCheckedSources, data.size() == mCheckedSources.size());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int pos = holder.getAdapterPosition();
        SourceBean sourceBean = data.get(pos);
        holder.tvSearchSourceText.setText(sourceBean.getName());
        holder.oneSearchSource.setChecked(mCheckedSources != null && mCheckedSources.containsKey(sourceBean.getKey()));
        holder.itemView.setOnClickListener(v -> {
            if (mCheckedSources.containsKey(sourceBean.getKey())) {
                mCheckedSources.remove(sourceBean.getKey());
                holder.oneSearchSource.setChecked(false);
            } else {
                mCheckedSources.put(sourceBean.getKey(), "1");
                holder.oneSearchSource.setChecked(true);
            }
            // 自动保存选择结果
            setMCheckedSources();
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public MaterialRadioButton oneSearchSource;
        public TextView tvSearchSourceText;

        public ViewHolder(View view) {
            super(view);
            oneSearchSource = view.findViewById(R.id.oneSearchSource);
            tvSearchSourceText = view.findViewById(R.id.tvSearchSourceText);
        }
    }
}
