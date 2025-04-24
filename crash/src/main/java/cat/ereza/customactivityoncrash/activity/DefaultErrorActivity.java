/*
 * Copyright 2014-2017 Eduard Ereza Martínez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cat.ereza.customactivityoncrash.activity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.R;
import cat.ereza.customactivityoncrash.config.CaocConfig;

public final class DefaultErrorActivity extends AppCompatActivity {

    @SuppressLint("PrivateResource")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set a modern theme
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar);

        setContentView(R.layout.customactivityoncrash_default_error_activity_m3);

        //Close/restart button logic:
        //If a class if set, use restart.
        //Else, use close and just finish the app.
        //It is recommended that you follow this logic if implementing a custom error activity.
        Button restartButton = findViewById(R.id.customactivityoncrash_error_activity_restart_button);

        final CaocConfig config = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        if (config == null) {
            //This should never happen - Just finish the activity to avoid a recursive crash.
            finish();
            return;
        }

        if (config.isShowRestartButton() && config.getRestartActivityClass() != null) {
            restartButton.setText(R.string.customactivityoncrash_error_activity_restart_app);
            restartButton.setOnClickListener(v -> CustomActivityOnCrash.restartApplication(DefaultErrorActivity.this, config));
        } else {
            restartButton.setOnClickListener(v -> CustomActivityOnCrash.closeApplication(DefaultErrorActivity.this, config));
        }

        Button moreInfoButton = findViewById(R.id.customactivityoncrash_error_activity_more_info_button);

        if (config.isShowErrorDetails()) {
            moreInfoButton.setOnClickListener(v -> {
                // 使用Material Design 3风格的对话框
                try {
                    // 尝试使用应用中的MD3对话框工具类
                    Class<?> md3DialogUtilsClass = Class.forName("com.github.tvbox.osc.util.MD3DialogUtils");
                    java.lang.reflect.Method showDetailDialogMethod = md3DialogUtilsClass.getMethod("showDetailDialog",
                            Context.class, String.class, String.class, String.class, String.class, DialogInterface.OnClickListener.class);

                    showDetailDialogMethod.invoke(null,
                            DefaultErrorActivity.this,
                            getString(R.string.customactivityoncrash_error_activity_error_details_title),
                            CustomActivityOnCrash.getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent()),
                            getString(R.string.customactivityoncrash_error_activity_error_details_close),
                            getString(R.string.customactivityoncrash_error_activity_error_details_copy),
                            (DialogInterface.OnClickListener) (dialog1, which) -> copyErrorToClipboard());
                } catch (Exception e) {
                    // 如果无法使用应用的MD3对话框工具类，则使用自定义的Material Design 3风格对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(DefaultErrorActivity.this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                            .setTitle(R.string.customactivityoncrash_error_activity_error_details_title)
                            .setMessage(CustomActivityOnCrash.getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent()))
                            .setPositiveButton(R.string.customactivityoncrash_error_activity_error_details_close, null)
                            .setNeutralButton(R.string.customactivityoncrash_error_activity_error_details_copy,
                                    (dialog1, which) -> copyErrorToClipboard());

                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_error_dialog_m3);
                    dialog.show();

                    // 设置按钮文字颜色
                    Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);

                    if (positiveButton != null) {
                        positiveButton.setTextColor(Color.parseColor("#232626"));
                        positiveButton.setTextSize(16);
                    }

                    if (neutralButton != null) {
                        neutralButton.setTextColor(Color.parseColor("#64D8D8"));
                        neutralButton.setTextSize(16);
                    }

                    TextView textView = dialog.findViewById(android.R.id.message);
                    if (textView != null) {
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.customactivityoncrash_error_activity_error_details_text_size));
                        textView.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
                    }
                }
            });
        } else {
            moreInfoButton.setVisibility(View.GONE);
        }

        Integer defaultErrorActivityDrawableId = config.getErrorDrawable();
        ImageView errorImageView = findViewById(R.id.customactivityoncrash_error_activity_image);

        if (defaultErrorActivityDrawableId != null) {
            errorImageView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), defaultErrorActivityDrawableId, getTheme()));
        }
    }

    private void copyErrorToClipboard() {
        String errorInformation = CustomActivityOnCrash.getAllErrorDetailsFromIntent(DefaultErrorActivity.this, getIntent());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        //Are there any devices without clipboard...?
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(getString(R.string.customactivityoncrash_error_activity_error_details_clipboard_label), errorInformation);
            clipboard.setPrimaryClip(clip);
            // 使用MD3ToastUtils显示Toast
            try {
                Class<?> md3ToastUtilsClass = Class.forName("com.github.tvbox.osc.util.MD3ToastUtils");
                java.lang.reflect.Method showToastMethod = md3ToastUtilsClass.getMethod("showToast", String.class);
                showToastMethod.invoke(null, getString(R.string.customactivityoncrash_error_activity_error_details_copied));
            } catch (Exception e) {
                // 如果无法使用MD3ToastUtils，则使用默认Toast
                Toast.makeText(DefaultErrorActivity.this, R.string.customactivityoncrash_error_activity_error_details_copied, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
