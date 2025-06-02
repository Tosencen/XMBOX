package com.github.tvbox.osc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xmbox.app.R
import com.github.tvbox.osc.ui.model.ToolItem
import com.github.tvbox.osc.util.MaterialSymbolsLoader

/**
 * 工具页面适配器
 */
class ToolsAdapter(
    private val items: List<ToolItem>,
    private val onItemClickListener: OnItemClickListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_TOOL = 1
    }

    interface OnItemClickListener {
        fun onItemClick(item: ToolItem.Tool)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tool_category_m3, parent, false)
                CategoryViewHolder(view)
            }
            VIEW_TYPE_TOOL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tool_m3, parent, false)
                ToolViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ToolItem.Category -> (holder as CategoryViewHolder).bind(item)
            is ToolItem.Tool -> (holder as ToolViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ToolItem.Category -> VIEW_TYPE_CATEGORY
            is ToolItem.Tool -> VIEW_TYPE_TOOL
        }
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)

        fun bind(item: ToolItem.Category) {
            tvCategory.text = item.title
        }
    }

    inner class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tv_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvValue: TextView = itemView.findViewById(R.id.tv_value)

        init {
            // 应用Material Symbols字体
            MaterialSymbolsLoader.apply(tvIcon)
            
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.onItemClick(items[position] as ToolItem.Tool)
                }
            }
        }

        fun bind(item: ToolItem.Tool) {
            tvIcon.text = item.icon
            tvTitle.text = item.title
            
            if (item.value.isNotEmpty()) {
                tvValue.text = item.value
                tvValue.visibility = View.VISIBLE
            } else {
                tvValue.visibility = View.GONE
            }
        }
    }
}
