package com.github.tvbox.osc.bean;

import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * 投屏视频信息
 */
public class CastVideo {

    private final String name;
    private final String url;

    public CastVideo(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @NonNull
    public String getId() {
        return UUID.randomUUID().toString();
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    public String getName() {
        return name;
    }

    /**
     * 获取标题
     */
    public String getTitle() {
        return name;
    }
}