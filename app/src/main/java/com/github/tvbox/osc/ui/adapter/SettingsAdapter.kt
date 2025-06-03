package com.github.tvbox.osc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xmbox.app.R
import com.github.tvbox.osc.ui.model.SettingItem
import com.google.android.material.button.MaterialButton

/**
 * 设置页面适配器
 */
class SettingsAdapter(
    private val items: List<SettingItem>,
    private val onItemClickListener: OnItemClickListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_REGULAR = 1
        private const val VIEW_TYPE_SWITCH = 2
        private const val VIEW_TYPE_BUTTON = 3
    }

    interface OnItemClickListener {
        fun onItemClick(item: SettingItem)
        fun onSwitchChanged(item: SettingItem.Switch, isChecked: Boolean)
        fun onButtonClick(item: SettingItem.Button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_category_m3, parent, false)
                CategoryViewHolder(view)
            }
            VIEW_TYPE_REGULAR -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_m3, parent, false)
                RegularViewHolder(view)
            }
            VIEW_TYPE_SWITCH -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_m3, parent, false)
                SwitchViewHolder(view)
            }
            VIEW_TYPE_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_setting_button_m3, parent, false)
                ButtonViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SettingItem.Category -> (holder as CategoryViewHolder).bind(item)
            is SettingItem.Regular -> (holder as RegularViewHolder).bind(item)
            is SettingItem.Switch -> (holder as SwitchViewHolder).bind(item)
            is SettingItem.Button -> (holder as ButtonViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SettingItem.Category -> VIEW_TYPE_CATEGORY
            is SettingItem.Regular -> VIEW_TYPE_REGULAR
            is SettingItem.Switch -> VIEW_TYPE_SWITCH
            is SettingItem.Button -> VIEW_TYPE_BUTTON
        }
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)

        fun bind(item: SettingItem.Category) {
            tvCategory.text = item.title
        }
    }

    inner class RegularViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvValue: TextView = itemView.findViewById(R.id.tv_value)
        private val ivArrow: ImageView = itemView.findViewById(R.id.iv_arrow)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.onItemClick(items[position])
                }
            }
        }

        fun bind(item: SettingItem.Regular) {
            tvTitle.text = item.title
            tvValue.text = item.value
            
            if (item.iconResId != 0) {
                ivIcon.setImageResource(item.iconResId)
                ivIcon.visibility = View.VISIBLE
            } else {
                ivIcon.visibility = View.GONE
            }
        }
    }

    inner class SwitchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvValue: TextView = itemView.findViewById(R.id.tv_value)
        private val ivArrow: ImageView = itemView.findViewById(R.id.iv_arrow)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position] as SettingItem.Switch
                    val newState = !item.isChecked
                    onItemClickListener?.onSwitchChanged(item, newState)
                }
            }
        }

        fun bind(item: SettingItem.Switch) {
            tvTitle.text = item.title
            
            // 设置开关状态文本
            tvValue.text = if (item.isChecked) "开" else "关"
            tvValue.setTextColor(
                itemView.context.getColor(
                    if (item.isChecked) R.color.md3_primary else R.color.md3_on_surface_variant
                )
            )
            
            // 隐藏箭头
            ivArrow.visibility = View.GONE
            
            if (item.iconResId != 0) {
                ivIcon.setImageResource(item.iconResId)
                ivIcon.visibility = View.VISIBLE
            } else {
                ivIcon.visibility = View.GONE
            }
        }
    }

    inner class ButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvValue: TextView = itemView.findViewById(R.id.tv_value)
        private val btnAction: MaterialButton = itemView.findViewById(R.id.btn_action)

        init {
            btnAction.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.onButtonClick(items[position] as SettingItem.Button)
                }
            }
            
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.onItemClick(items[position])
                }
            }
        }

        fun bind(item: SettingItem.Button) {
            tvTitle.text = item.title
            tvValue.text = item.value
            btnAction.text = item.buttonText
            
            if (item.iconResId != 0) {
                ivIcon.setImageResource(item.iconResId)
                ivIcon.visibility = View.VISIBLE
            } else {
                ivIcon.visibility = View.GONE
            }
        }
    }
}
