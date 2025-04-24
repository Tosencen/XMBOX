package com.github.tvbox.osc.util;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.dialog.ConfirmDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lxj.xpopup.XPopup;

/**
 * Material Design 3 弹窗工具类
 * 提供统一的弹窗创建方法，确保所有弹窗都符合Material Design 3规范
 */
public class MD3DialogUtils {

    /**
     * 创建确认弹窗（使用XPopup）
     *
     * @param context 上下文
     * @param title 标题
     * @param content 内容
     * @param cancelText 取消按钮文本（为空则不显示取消按钮）
     * @param confirmText 确认按钮文本
     * @param listener 按钮点击监听器
     * @return 弹窗实例
     */
    public static ConfirmDialog createConfirmDialog(
            @NonNull Context context,
            String title,
            String content,
            String cancelText,
            String confirmText,
            ConfirmDialog.OnDialogActionListener listener) {

        return new ConfirmDialog(context, title, content, cancelText, confirmText, listener);
    }

    /**
     * 显示确认弹窗（使用XPopup）
     *
     * @param context 上下文
     * @param title 标题
     * @param content 内容
     * @param cancelText 取消按钮文本（为空则不显示取消按钮）
     * @param confirmText 确认按钮文本
     * @param listener 按钮点击监听器
     */
    public static void showConfirmDialog(
            @NonNull Context context,
            String title,
            String content,
            String cancelText,
            String confirmText,
            ConfirmDialog.OnDialogActionListener listener) {

        XPopup.Builder builder = XPopupAnimUtil.createMD3Builder(context);

        builder.asCustom(createConfirmDialog(context, title, content, cancelText, confirmText, listener))
                .show();
    }

    /**
     * 创建警告/确认弹窗（使用MaterialAlertDialogBuilder）
     *
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param positiveText 确认按钮文本
     * @param negativeText 取消按钮文本（为空则不显示取消按钮）
     * @param positiveAction 确认按钮点击事件
     * @param negativeAction 取消按钮点击事件
     * @return 弹窗构建器
     */
    public static MaterialAlertDialogBuilder createAlertDialog(
            @NonNull Context context,
            String title,
            String message,
            String positiveText,
            String negativeText,
            DialogInterface.OnClickListener positiveAction,
            DialogInterface.OnClickListener negativeAction) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3));

        if (positiveText != null) {
            builder.setPositiveButton(positiveText, positiveAction);
        }

        if (negativeText != null) {
            builder.setNegativeButton(negativeText, negativeAction);
        }

        return builder;
    }

    /**
     * 显示警告/确认弹窗（使用MaterialAlertDialogBuilder）
     *
     * @param context 上下文
     * @param title 标题
     * @param message 消息内容
     * @param positiveText 确认按钮文本
     * @param negativeText 取消按钮文本（为空则不显示取消按钮）
     * @param positiveAction 确认按钮点击事件
     * @param negativeAction 取消按钮点击事件
     * @return 弹窗实例
     */
    public static AlertDialog showAlertDialog(
            @NonNull Context context,
            String title,
            String message,
            String positiveText,
            String negativeText,
            DialogInterface.OnClickListener positiveAction,
            DialogInterface.OnClickListener negativeAction) {

        return createAlertDialog(context, title, message, positiveText, negativeText, positiveAction, negativeAction)
                .show();
    }

    /**
     * 创建选择弹窗
     *
     * @param context 上下文
     * @param title 标题
     * @param items 选项数组
     * @param onItemSelected 选项选择事件
     * @return 弹窗构建器
     */
    public static MaterialAlertDialogBuilder createSelectionDialog(
            @NonNull Context context,
            String title,
            String[] items,
            DialogInterface.OnClickListener onItemSelected) {

        return new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3))
                .setItems(items, onItemSelected);
    }

    /**
     * 显示选择弹窗
     *
     * @param context 上下文
     * @param title 标题
     * @param items 选项数组
     * @param onItemSelected 选项选择事件
     * @return 弹窗实例
     */
    public static AlertDialog showSelectionDialog(
            @NonNull Context context,
            String title,
            String[] items,
            DialogInterface.OnClickListener onItemSelected) {

        return createSelectionDialog(context, title, items, onItemSelected)
                .show();
    }

    /**
     * 创建输入弹窗
     *
     * @param context 上下文
     * @param title 标题
     * @param hint 输入框提示文本
     * @param initialValue 初始值
     * @param onConfirm 确认按钮点击事件
     * @return 弹窗构建器
     */
    public static MaterialAlertDialogBuilder createInputDialog(
            @NonNull Context context,
            String title,
            String hint,
            String initialValue,
            OnInputDialogListener onConfirm) {

        // 创建输入框布局
        TextInputLayout inputLayout = new TextInputLayout(context);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setBoxStrokeColor(context.getColor(R.color.md3_primary));

        TextInputEditText editText = new TextInputEditText(context);
        editText.setHint(hint);
        if (initialValue != null) {
            editText.setText(initialValue);
        }

        inputLayout.addView(editText);

        // 设置内边距
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        inputLayout.setPadding(padding, padding, padding, padding);

        return new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3))
                .setView(inputLayout)
                .setPositiveButton("确认", (dialog, which) -> {
                    if (onConfirm != null) {
                        onConfirm.onConfirm(editText.getText().toString());
                    }
                })
                .setNegativeButton("取消", null);
    }

    /**
     * 显示输入弹窗
     *
     * @param context 上下文
     * @param title 标题
     * @param hint 输入框提示文本
     * @param initialValue 初始值
     * @param onConfirm 确认按钮点击事件
     * @return 弹窗实例
     */
    public static AlertDialog showInputDialog(
            @NonNull Context context,
            String title,
            String hint,
            String initialValue,
            OnInputDialogListener onConfirm) {

        return createInputDialog(context, title, hint, initialValue, onConfirm)
                .show();
    }

    /**
     * 输入弹窗监听器
     */
    public interface OnInputDialogListener {
        void onConfirm(String input);
    }

    /**
     * 显示详细信息对话框
     *
     * @param context 上下文
     * @param title 标题
     * @param message 详细信息内容
     * @param positiveButtonText 确定按钮文本
     * @param neutralButtonText 中性按钮文本
     * @param neutralButtonListener 中性按钮点击监听器
     * @return 对话框实例
     */
    public static AlertDialog showDetailDialog(
            @NonNull Context context,
            String title,
            String message,
            String positiveButtonText,
            String neutralButtonText,
            DialogInterface.OnClickListener neutralButtonListener) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog_ErrorDetail)
                .setTitle(title)
                .setMessage(message)
                .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_m3))
                .setPositiveButton(positiveButtonText, null);

        if (neutralButtonText != null) {
            builder.setNeutralButton(neutralButtonText, neutralButtonListener);
        }

        AlertDialog dialog = builder.show();

        // 设置消息文本样式为等宽字体，便于阅读错误信息
        TextView textView = dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            textView.setTypeface(Typeface.MONOSPACE);
        }

        return dialog;
    }
}
