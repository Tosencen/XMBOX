package com.github.tvbox.osc.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.DnsSafetyTest;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD3ToastUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * DNS测试活动
 * 用于测试DNS安全修复是否有效
 */
public class DnsTestActivity extends AppCompatActivity {
    private static final String TAG = "DnsTestActivity";
    private TextView tvResult;
    private Button btnTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dns_test);

        tvResult = findViewById(R.id.tv_result);
        btnTest = findViewById(R.id.btn_test);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTests();
            }
        });
    }

    private void runTests() {
        tvResult.setText("正在运行DNS安全测试...\n");

        // 测试1: 基本DNS安全测试
        boolean basicTestResult = DnsSafetyTest.testDnsSafety();
        appendResult("基本DNS安全测试: " + (basicTestResult ? "通过" : "失败"));

        // 测试2: 实际网络请求测试
        testNetworkRequest();
    }

    private void testNetworkRequest() {
        appendResult("开始网络请求测试...");

        // 创建一个使用null DNS的OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        try {
            // 这里故意使用null DNS，如果我们的修复有效，应该会使用系统DNS而不是崩溃
            builder.dns(null);
            OkHttpClient client = builder.build();

            // 尝试发起网络请求
            Request request = new Request.Builder()
                    .url("https://www.baidu.com")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        appendResult("网络请求失败: " + e.getMessage());
                        if (e.getMessage() != null && e.getMessage().contains("dns == null")) {
                            appendResult("检测到DNS空指针异常，修复无效!");
                            MD3ToastUtils.showToast("DNS修复测试失败!");
                        } else {
                            appendResult("网络请求失败，但不是由于DNS空指针异常");
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        appendResult("网络请求成功，状态码: " + response.code());
                        appendResult("修复有效，即使使用null DNS也能正常工作!");
                        MD3ToastUtils.showToast("DNS修复测试通过!");
                    });
                    response.close();
                }
            });
        } catch (Exception e) {
            appendResult("创建OkHttpClient时异常: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("dns == null")) {
                appendResult("检测到DNS空指针异常，修复无效!");
                MD3ToastUtils.showToast("DNS修复测试失败!");
            }
        }
    }

    private void appendResult(String text) {
        runOnUiThread(() -> {
            tvResult.append(text + "\n");
            LOG.i(TAG, text);
        });
    }
}
